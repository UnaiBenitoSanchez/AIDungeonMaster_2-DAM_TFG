package com.example.aidungeonmaster.workers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aidungeonmaster.utils.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.*

/**
 * Worker periódico que obtiene la ubicación actual del dispositivo y consulta
 * la API Overpass (OpenStreetMap, gratuita) para detectar supermercados en un
 * radio de [RADIUS_METERS] metros.
 *
 * Si encuentra alguno y han pasado al menos [COOLDOWN_MS] desde el último aviso,
 * muestra una notificación invitando al jugador a abrir el escáner de la app.
 *
 * Requiere:
 *   - Permiso ACCESS_FINE_LOCATION (o ACCESS_COARSE_LOCATION)
 *   - Dependencia: com.google.android.gms:play-services-location
 *
 * Frecuencia sugerida: cada 30 minutos (configurable en AIDungeonMasterApp).
 */
class SupermarketProximityWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        // ── Anti-spam: no notificar si avisamos hace menos de COOLDOWN_MS ────
        val lastNotified = prefs.getLong(KEY_LAST_NOTIFIED, 0L)
        if (System.currentTimeMillis() - lastNotified < COOLDOWN_MS) {
            Log.d(TAG, "Cooldown activo, omitiendo comprobación.")
            return Result.success()
        }

        return try {
            val location = getLastLocation() ?: run {
                Log.d(TAG, "No se pudo obtener la ubicación.")
                return Result.success()
            }

            Log.d(TAG, "Ubicación: ${location.latitude}, ${location.longitude}")

            val nearest = findNearestSupermarket(location)
            if (nearest == null) {
                Log.d(TAG, "Ningún supermercado en $RADIUS_METERS m.")
                return Result.success()
            }

            Log.i(TAG, "Super encontrado: ${nearest.name} a ${nearest.distanceMeters} m")

            val specialty = supermarketSpecialty(nearest.name)

            NotificationHelper.showProximityNotification(
                context         = applicationContext,
                supermarketName = nearest.name,
                distanceMeters  = nearest.distanceMeters,
                specialty       = specialty,
                notificationId  = NOTIF_ID
            )

            prefs.edit().putLong(KEY_LAST_NOTIFIED, System.currentTimeMillis()).apply()
            Result.success()

        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso de localización: ${e.message}")
            Result.success()   // No reintentar; el permiso no cambia solo
        } catch (e: Exception) {
            Log.e(TAG, "Error en SupermarketProximityWorker: ${e.message}")
            Result.retry()
        }
    }

    // ── Obtener ubicación ─────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Location? {
        val client = LocationServices.getFusedLocationProviderClient(applicationContext)
        return try {
            // Intentamos primero la última ubicación conocida (rápida, sin consumo de batería)
            val last = client.lastLocation.await()
            if (last != null && System.currentTimeMillis() - last.time < MAX_LOCATION_AGE_MS) {
                return last
            }
            // Si es demasiado antigua o nula, pedimos una nueva lectura puntual
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "getLastLocation error: ${e.message}")
            null
        }
    }

    // ── Consulta Overpass API ─────────────────────────────────────────────────

    private suspend fun findNearestSupermarket(location: Location): NearbyShop? {
        val lat = location.latitude
        val lon = location.longitude

        // Consulta QL: nodos y ways con shop=supermarket o shop=convenience en el radio
        val query = """
            [out:json][timeout:15];
            (
              node["shop"="supermarket"](around:$RADIUS_METERS,$lat,$lon);
              node["shop"="convenience"](around:$RADIUS_METERS,$lat,$lon);
            );
            out 10;
        """.trimIndent()

        val url = "https://overpass-api.de/api/interpreter?data=${
            java.net.URLEncoder.encode(query, "UTF-8")
        }"

        val request = Request.Builder().url(url)
            .header("User-Agent", "AIDungeonMasterApp/1.0")
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val response = http.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Overpass error: ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            parseOverpassResponse(body, location)
        }
    }

    private fun parseOverpassResponse(json: String, userLocation: Location): NearbyShop? {
        return try {
            val root     = JSONObject(json)
            val elements = root.getJSONArray("elements")
            if (elements.length() == 0) return null

            var nearest: NearbyShop? = null

            for (i in 0 until elements.length()) {
                val el   = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags") ?: continue

                val name = tags.optString("name")
                    .ifBlank { tags.optString("brand") }
                    .ifBlank { tags.optString("operator") }

                if (name.isBlank()) continue

                val elLat = el.optDouble("lat", Double.NaN)
                val elLon = el.optDouble("lon", Double.NaN)
                if (elLat.isNaN() || elLon.isNaN()) continue

                val dist = haversineMeters(userLocation.latitude, userLocation.longitude, elLat, elLon)

                // Mapear nombre OSM a nombre reconocido (o usar el nombre tal cual)
                val displayName = mapToKnownChain(name) ?: name

                if (nearest == null || dist < nearest.distanceMeters) {
                    nearest = NearbyShop(displayName, dist.toInt())
                }
            }

            nearest
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando Overpass: ${e.message}")
            null
        }
    }

    // ── Mapear nombre OSM a cadena conocida ───────────────────────────────────

    private fun mapToKnownChain(rawName: String): String? {
        val lower = rawName.lowercase()
        return when {
            "mercadona"      in lower -> "MERCADONA"
            "carrefour"      in lower -> "CARREFOUR"
            "lidl"           in lower -> "LIDL"
            "aldi"           in lower -> "ALDI"
            " dia"           in lower || lower == "dia" || lower.startsWith("dia ") -> "DIA"
            "lupa"           in lower -> "LUPA"
            "eroski"         in lower -> "EROSKI"
            "consum"         in lower -> "CONSUM"
            "alcampo"        in lower -> "ALCAMPO"
            "hipercor"       in lower -> "HIPERCOR"
            "supercor"       in lower -> "SUPERCOR"
            "corte ingles"   in lower || "corte inglés" in lower -> "EL CORTE INGLÉS"
            "spar"           in lower -> "SPAR"
            "simply"         in lower -> "SIMPLY"
            "auchan"         in lower -> "AUCHAN"
            "froiz"          in lower -> "FROIZ"
            "gadis"          in lower -> "GADIS"
            "bon preu"       in lower -> "BON PREU"
            else                      -> null
        }
    }

    // ── Especialidad de cada cadena ───────────────────────────────────────────

    private fun supermarketSpecialty(name: String): String = when {
        "mercadona"  in name.lowercase() -> "🧪 Pociones y suministros curativos"
        "lidl"       in name.lowercase() -> "🎲 Artículos misteriosos con descuento"
        "carrefour"  in name.lowercase() -> "⚔️ Armas y armaduras de campaña"
        "aldi"       in name.lowercase() -> "🪙 Suministros básicos a bajo coste"
        "dia"        in name.lowercase() -> "💰 Descuentos especiales en todo"
        "eroski"     in name.lowercase() -> "📜 Pergaminos mágicos y hechizos"
        "lupa"       in name.lowercase() -> "🌿 Pociones regionales únicas"
        "consum"     in name.lowercase() -> "🍞 Raciones de campaña y consumibles"
        "alcampo"    in name.lowercase() -> "⚗️ Gran selección de elixires"
        else                             -> "🛒 Suminitros generales de aventura"
    }

    // ── Haversine ─────────────────────────────────────────────────────────────

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r    = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    // ── Datos ─────────────────────────────────────────────────────────────────

    private data class NearbyShop(val name: String, val distanceMeters: Int)

    companion object {
        private const val TAG                = "SupermarketProxWorker"
        private const val PREFS_NAME         = "supermarket_proximity_prefs"
        private const val KEY_LAST_NOTIFIED  = "last_proximity_notified"
        private const val RADIUS_METERS      = 500
        private const val NOTIF_ID           = 3001
        /** Mínimo tiempo entre notificaciones de proximidad: 1 hora */
        private const val COOLDOWN_MS        = 60L * 60L * 1_000L
        /** Máxima antigüedad de la última ubicación para considerarla válida: 5 min */
        private const val MAX_LOCATION_AGE_MS = 5L * 60L * 1_000L
    }
}
