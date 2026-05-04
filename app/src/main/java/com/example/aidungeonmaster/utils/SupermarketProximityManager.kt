package com.example.aidungeonmaster.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.aidungeonmaster.workers.SupermarketProximityWorker
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Detecta supermercados en tiempo real mientras la app está en primer plano.
 *
 * Complementa al [SupermarketProximityWorker] (que actúa cuando la app está cerrada):
 *  - Solicita actualizaciones de ubicación cada [LOCATION_INTERVAL_MS] ms.
 *  - Al recibir una posición, consulta Overpass API si han pasado al menos
 *    [QUERY_COOLDOWN_MS] desde la última consulta (para no saturar la API).
 *  - Si detecta un supermercado nuevo (diferente al último avisado) o el mismo
 *    pasado [SAME_SHOP_COOLDOWN_MS], lanza la notificación.
 *
 * Uso desde un Composable:
 * ```
 * DisposableEffect(Unit) {
 *     val manager = SupermarketProximityManager(context)
 *     manager.start()
 *     onDispose { manager.stop() }
 * }
 * ```
 */
class SupermarketProximityManager(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val scope        = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var lastQueryTime  = 0L
    private var lastShopName   = ""

    private val locationCallback = object : LocationCallback() {
        // Gestiona el evento de location result.
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            Log.d(TAG, "Ubicación recibida: ${location.latitude}, ${location.longitude}")
            scope.launch { checkProximity(location) }
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    // Ejecuta la lógica de start.
    fun start() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            LOCATION_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_MS)
            .setWaitForAccurateLocation(false)
            .build()

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        Log.d(TAG, "SupermarketProximityManager iniciado.")
    }

    // Ejecuta la lógica de stop.
    fun stop() {
        fusedClient.removeLocationUpdates(locationCallback)
        scope.cancel()
        Log.d(TAG, "SupermarketProximityManager detenido.")
    }

    // ── Lógica principal ──────────────────────────────────────────────────────

    private suspend fun checkProximity(location: Location) {
        val now = System.currentTimeMillis()

        // Limitar consultas a Overpass para no abusar de la API gratuita
        if (now - lastQueryTime < QUERY_COOLDOWN_MS) return
        lastQueryTime = now

        val shop = findNearestSupermarket(location) ?: return

        // Anti-spam: no repetir la misma tienda hasta que pase SAME_SHOP_COOLDOWN_MS
        val lastNotified = prefs.getLong(KEY_LAST_NOTIFIED, 0L)
        val sameShop     = shop.name == lastShopName
        if (sameShop && now - lastNotified < SAME_SHOP_COOLDOWN_MS) {
            Log.d(TAG, "Cooldown activo para ${shop.name}, omitiendo.")
            return
        }

        Log.i(TAG, "¡Supermercado detectado en tiempo real! ${shop.name} a ${shop.distanceMeters}m")

        val specialty = supermarketSpecialty(shop.name)
        NotificationHelper.showProximityNotification(
            context         = context,
            supermarketName = shop.name,
            distanceMeters  = shop.distanceMeters,
            specialty       = specialty,
            notificationId  = NOTIF_ID
        )

        lastShopName = shop.name
        prefs.edit().putLong(KEY_LAST_NOTIFIED, now).apply()
    }

    // ── Overpass API ──────────────────────────────────────────────────────────

    private suspend fun findNearestSupermarket(location: Location): NearbyShop? {
        val lat = location.latitude
        val lon = location.longitude

        val query = """
            [out:json][timeout:10];
            (
              node["shop"="supermarket"](around:$RADIUS_METERS,$lat,$lon);
              node["shop"="convenience"](around:$RADIUS_METERS,$lat,$lon);
            );
            out 10;
        """.trimIndent()

        val url = "https://overpass-api.de/api/interpreter?data=${
            URLEncoder.encode(query, "UTF-8")
        }"

        return try {
            val request  = Request.Builder().url(url)
                .header("User-Agent", "AIDungeonMasterApp/1.0")
                .build()
            val response = http.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Overpass HTTP ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            parseOverpassResponse(body, location)
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando Overpass: ${e.message}")
            null
        }
    }

    // Analiza overpass response.
    private fun parseOverpassResponse(json: String, userLocation: Location): NearbyShop? {
        return try {
            val elements = JSONObject(json).getJSONArray("elements")
            if (elements.length() == 0) return null

            var nearest: NearbyShop? = null
            for (i in 0 until elements.length()) {
                val el   = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags") ?: continue

                val rawName = tags.optString("name")
                    .ifBlank { tags.optString("brand") }
                    .ifBlank { tags.optString("operator") }
                if (rawName.isBlank()) continue

                val elLat = el.optDouble("lat", Double.NaN)
                val elLon = el.optDouble("lon", Double.NaN)
                if (elLat.isNaN() || elLon.isNaN()) continue

                val dist        = haversineMeters(userLocation.latitude, userLocation.longitude, elLat, elLon)
                val displayName = mapToKnownChain(rawName) ?: rawName

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

    // ── Helpers (igual que en SupermarketProximityWorker) ─────────────────────

    private fun mapToKnownChain(rawName: String): String? {
        val lower = rawName.lowercase()
        return when {
            "mercadona"    in lower                                           -> "MERCADONA"
            "carrefour"    in lower                                           -> "CARREFOUR"
            "lidl"         in lower                                           -> "LIDL"
            "aldi"         in lower                                           -> "ALDI"
            " dia"         in lower || lower == "dia" || lower.startsWith("dia ") -> "DIA"
            "lupa"         in lower                                           -> "LUPA"
            "eroski"       in lower                                           -> "EROSKI"
            "consum"       in lower                                           -> "CONSUM"
            "alcampo"      in lower                                           -> "ALCAMPO"
            "hipercor"     in lower                                           -> "HIPERCOR"
            "supercor"     in lower                                           -> "SUPERCOR"
            "corte ingles" in lower || "corte inglés" in lower               -> "EL CORTE INGLÉS"
            "spar"         in lower                                           -> "SPAR"
            "simply"       in lower                                           -> "SIMPLY"
            "auchan"       in lower                                           -> "AUCHAN"
            "froiz"        in lower                                           -> "FROIZ"
            "gadis"        in lower                                           -> "GADIS"
            "bon preu"     in lower                                           -> "BON PREU"
            else                                                              -> null
        }
    }

    // Ejecuta la lógica de supermarket specialty.
    private fun supermarketSpecialty(name: String): String = when {
        "mercadona" in name.lowercase() -> "🧪 Pociones y suministros curativos"
        "lidl"      in name.lowercase() -> "🎲 Artículos misteriosos con descuento"
        "carrefour" in name.lowercase() -> "⚔️ Armas y armaduras de campaña"
        "aldi"      in name.lowercase() -> "🪙 Suministros básicos a bajo coste"
        "dia"       in name.lowercase() -> "💰 Descuentos especiales en todo"
        "eroski"    in name.lowercase() -> "📜 Pergaminos mágicos y hechizos"
        "lupa"      in name.lowercase() -> "🌿 Pociones regionales únicas"
        "consum"    in name.lowercase() -> "🍞 Raciones de campaña y consumibles"
        "alcampo"   in name.lowercase() -> "⚗️ Gran selección de elixires"
        else                            -> "🛒 Suministros generales de aventura"
    }

    // Ejecuta la lógica de haversine meters.
    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r    = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    // Clase que encapsula la lógica de nearby shop.
    private data class NearbyShop(val name: String, val distanceMeters: Int)

    companion object {
        private const val TAG                  = "SupermarketRealtime"
        private const val PREFS_NAME           = "supermarket_proximity_prefs"  // mismo que el Worker
        private const val KEY_LAST_NOTIFIED    = "last_proximity_notified"      // mismo que el Worker
        private const val RADIUS_METERS        = 500
        private const val NOTIF_ID             = 3001                           // mismo que el Worker
        /** Frecuencia de actualización de GPS mientras la app está abierta */
        private const val LOCATION_INTERVAL_MS = 30_000L   // cada 30 segundos
        private const val LOCATION_FASTEST_MS  = 15_000L   // mínimo 15 s entre updates
        /** Cooldown entre consultas a Overpass (respetar la API pública) */
        private const val QUERY_COOLDOWN_MS    = 60_000L   // 1 consulta por minuto máximo
        /** Cooldown para no repetir notificación del mismo super */
        private const val SAME_SHOP_COOLDOWN_MS = 60L * 60L * 1_000L  // 1 hora
    }
}
