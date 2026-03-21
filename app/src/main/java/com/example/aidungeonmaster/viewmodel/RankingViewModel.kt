package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RankingEntry(
    val charId: String = "",
    val characterName: String = "",
    val race: String = "",
    val characterClass: String = "",
    val stats: Map<String, Int> = emptyMap(),
    val hpMax: Int = 0,
    val totalStats: Int = 0,
    // Stats aplanados para poder ordenar en Firestore
    val fuerza: Int = 0,
    val destreza: Int = 0,
    val inteligencia: Int = 0,
    val sabiduria: Int = 0,
    val constitucion: Int = 0,
    val carisma: Int = 0
)

enum class RankingCategory(val label: String, val icon: String, val field: String, val displayName: String) {
    TOTAL_STATS("Stats Totales",  "🏆", "totalStats",   "pts totales"),
    HP_MAX     ("HP Máximo",      "❤️", "hpMax",        "HP"),
    STRENGTH   ("Fuerza",         "⚔️", "fuerza",       "FUE"),
    DEXTERITY  ("Destreza",       "🏹", "destreza",     "DES"),
    INTELLIGENCE("Inteligencia",  "🔮", "inteligencia", "INT"),
    WISDOM     ("Sabiduría",      "🦉", "sabiduria",    "SAB"),
    CONSTITUTION("Constitución",  "🛡️", "constitucion", "CON"),
    CHARISMA   ("Carisma",        "✨", "carisma",      "CAR")
}

class RankingViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _rankings = MutableStateFlow<Map<RankingCategory, List<RankingEntry>>>(emptyMap())
    val rankings = _rankings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init { loadAllRankings() }

    fun loadAllRankings() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = mutableMapOf<RankingCategory, List<RankingEntry>>()
            for (category in RankingCategory.entries) {
                result[category] = fetchTop(category)
            }
            _rankings.value = result
            _isLoading.value = false
        }
    }

    private suspend fun fetchTop(category: RankingCategory): List<RankingEntry> {
        return try {
            val snap = db.collection("ranking")
                .orderBy(category.field, Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val stats = (doc.get("stats") as? Map<String, Long>)
                        ?.mapValues { it.value.toInt() } ?: emptyMap()
                    RankingEntry(
                        charId         = doc.id,
                        characterName  = doc.getString("characterName") ?: "???",
                        race           = doc.getString("race")           ?: "",
                        characterClass = doc.getString("characterClass") ?: "",
                        stats          = stats,
                        hpMax          = doc.getLong("hpMax")?.toInt()        ?: 0,
                        totalStats     = doc.getLong("totalStats")?.toInt()   ?: 0,
                        fuerza         = doc.getLong("fuerza")?.toInt()       ?: stats["Fuerza"]       ?: 0,
                        destreza       = doc.getLong("destreza")?.toInt()     ?: stats["Destreza"]     ?: 0,
                        inteligencia   = doc.getLong("inteligencia")?.toInt() ?: stats["Inteligencia"] ?: 0,
                        sabiduria      = doc.getLong("sabiduria")?.toInt()    ?: stats["Sabiduría"]    ?: 0,
                        constitucion   = doc.getLong("constitucion")?.toInt() ?: stats["Constitución"] ?: 0,
                        carisma        = doc.getLong("carisma")?.toInt()      ?: stats["Carisma"]      ?: 0
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            Log.e("RankingVM", "Error cargando ${category.name}: ${e.message}")
            emptyList()
        }
    }

    companion object {
        /**
         * Construye el mapa de campos aplanados a partir de un mapa de stats para
         * poder escribirlos directamente en la colección ranking.
         * Llamar desde HomeViewModel al crear/actualizar personaje.
         */
        fun buildRankingData(
            name: String,
            race: String,
            clazz: String,
            stats: Map<String, Int>,
            hpMax: Int
        ): Map<String, Any> {
            val totalStats = stats.values.sum()

            // Mapeo de nombre de stat (en español) → campo en Firestore
            fun stat(key: String) = stats[key]?.toLong() ?: 0L

            return mapOf(
                "characterName"  to name,
                "race"           to race,
                "characterClass" to clazz,
                "stats"          to stats,
                "hpMax"          to hpMax.toLong(),
                "totalStats"     to totalStats.toLong(),
                // Campos aplanados para poder hacer orderBy en Firestore sin índice compuesto
                "fuerza"         to stat("Fuerza"),
                "destreza"       to stat("Destreza"),
                "inteligencia"   to stat("Inteligencia"),
                "sabiduria"      to (stat("Sabiduría").takeIf { it > 0L } ?: stat("Sabiduria")),
                "constitucion"   to (stat("Constitución").takeIf { it > 0L } ?: stat("Constitucion")),
                "carisma"        to stat("Carisma"),
                "lastUpdated"    to System.currentTimeMillis()
            )
        }
    }
}