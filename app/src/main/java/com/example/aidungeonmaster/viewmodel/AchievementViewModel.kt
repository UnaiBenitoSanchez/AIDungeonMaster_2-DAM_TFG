package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.Achievement
import com.example.aidungeonmaster.data.model.AchievementCatalog
import com.example.aidungeonmaster.data.model.Quest
import com.example.aidungeonmaster.data.model.QuestCatalog
import com.example.aidungeonmaster.data.model.QuestObjectiveType
import com.example.aidungeonmaster.data.model.QuestStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel central para logros y misiones.
 *
 * Estructura en Firestore:
 *   users/{uid}/achievements/{achievementId}  → {isUnlocked, unlockedAt}
 *   users/{uid}/quests/{questId}              → {status, objectives:[{currentValue}], acceptedAt, completedAt}
 *   partidas/{charId}/stats                   → {combatWins, locationsDiscovered, itemsFound, messagesSent}
 */
class AchievementViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Estado expuesto ───────────────────────────────────────────────────────

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements = _achievements.asStateFlow()

    private val _quests = MutableStateFlow<List<Quest>>(emptyList())
    val quests = _quests.asStateFlow()

    /** Emite el logro recién desbloqueado para mostrar un Toast/Dialog */
    private val _newAchievement = MutableSharedFlow<Achievement>(replay = 0, extraBufferCapacity = 1)
    val newAchievement = _newAchievement.asSharedFlow()

    /** Emite la misión recién completada */
    private val _completedQuest = MutableSharedFlow<Quest>(replay = 0, extraBufferCapacity = 1)
    val completedQuest = _completedQuest.asSharedFlow()

    // XP pendiente de conceder por logros/misiones (el GameViewModel lo consume)
    private val _pendingAchievementXp = MutableStateFlow(0)
    val pendingAchievementXp = _pendingAchievementXp.asStateFlow()

    fun consumeAchievementXp() { _pendingAchievementXp.value = 0 }

    // ── Carga inicial ─────────────────────────────────────────────────────────

    fun loadForCharacter(charId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            loadAchievements(uid)
            loadQuests(uid, charId)
        }
    }

    private suspend fun loadAchievements(uid: String) {
        try {
            val snap = db.collection("users").document(uid)
                .collection("achievements").get().await()
            val unlockedIds = snap.documents.mapNotNull { doc ->
                if (doc.getBoolean("isUnlocked") == true) doc.id else null
            }.toSet()

            _achievements.value = AchievementCatalog.all.map { achievement ->
                if (achievement.id in unlockedIds) {
                    val doc = snap.documents.firstOrNull { it.id == achievement.id }
                    achievement.copy(
                        isUnlocked  = true,
                        unlockedAt  = doc?.getLong("unlockedAt") ?: 0L
                    )
                } else achievement
            }
        } catch (e: Exception) {
            Log.e("AchievVM", "loadAchievements: ${e.message}")
        }
    }

    private suspend fun loadQuests(uid: String, charId: String) {
        try {
            val snap = db.collection("users").document(uid)
                .collection("quests").get().await()

            val questMap = snap.documents.associate { doc ->
                val status = when (doc.getString("status")) {
                    "IN_PROGRESS" -> QuestStatus.IN_PROGRESS
                    "COMPLETED"   -> QuestStatus.COMPLETED
                    "FAILED"      -> QuestStatus.FAILED
                    else          -> QuestStatus.AVAILABLE
                }
                @Suppress("UNCHECKED_CAST")
                val objectiveValues = (doc.get("objectives") as? List<Map<String, Any>>)
                    ?.map { it["currentValue"] as? Long ?: 0L } ?: emptyList()

                doc.id to Pair(status, objectiveValues)
            }

            _quests.value = QuestCatalog.all.map { quest ->
                val saved = questMap[quest.id]
                if (saved != null) {
                    val updatedObjectives = quest.objectives.mapIndexed { i, obj ->
                        val savedVal = saved.second.getOrNull(i)?.toInt() ?: obj.currentValue
                        obj.copy(currentValue = savedVal)
                    }
                    quest.copy(status = saved.first, objectives = updatedObjectives)
                } else {
                    // Auto-aceptar la primera misión
                    if (quest.id == "intro_quest") {
                        val accepted = quest.copy(
                            status     = QuestStatus.IN_PROGRESS,
                            acceptedAt = System.currentTimeMillis()
                        )
                        saveQuestToFirestore(uid, accepted)
                        accepted
                    } else quest
                }
            }
        } catch (e: Exception) {
            Log.e("AchievVM", "loadQuests: ${e.message}")
        }
    }

    // ── Eventos del juego que disparan comprobaciones ─────────────────────────

    /** Llamar cuando el jugador gana un combate */
    fun onCombatWon(charId: String) {
        updateQuestProgress(charId, QuestObjectiveType.WINS, 1)
        checkAchievement("first_blood")
        viewModelScope.launch {
            val wins = getCombatWins(charId)
            if (wins >= 10) checkAchievement("ten_victories")
        }
    }

    /** Llamar cuando el jugador consigue un golpe crítico */
    fun onCriticalHit() = checkAchievement("critical_strike")

    /** Llamar cuando el jugador sobrevive con 1 HP al final de un combate */
    fun onSurvivedLowHp() = checkAchievement("survive_low_hp")

    /** Llamar cuando se descubre una nueva ubicación en el mapa */
    fun onLocationDiscovered(charId: String, locationCount: Int) {
        updateQuestProgress(charId, QuestObjectiveType.LOCATIONS, 1)
        if (locationCount >= 1)  checkAchievement("first_location")
        if (locationCount >= 5)  checkAchievement("five_locations")
        if (locationCount >= 10) checkAchievement("ten_locations")
    }

    /** Llamar cuando se encuentra un objeto */
    fun onItemFound(charId: String, inventorySize: Int) {
        updateQuestProgress(charId, QuestObjectiveType.ITEMS, 1)
        if (inventorySize >= 1) checkAchievement("first_item")
        if (inventorySize >= 5) checkAchievement("five_items")
    }

    /** Llamar cuando el jugador envía un mensaje/acción al DM */
    fun onMessageSent(charId: String) =
        updateQuestProgress(charId, QuestObjectiveType.MESSAGES, 1)

    /** Llamar cuando el personaje sube de nivel */
    fun onLevelUp(charId: String, newLevel: Int) {
        checkAchievement("first_levelup")
        if (newLevel >= 5)  checkAchievement("level_5")
        if (newLevel >= 10) checkAchievement("level_10")
        updateQuestProgress(charId, QuestObjectiveType.LEVEL_REACH, 0, absolute = newLevel)
    }

    /** Llamar cuando el jugador escanea un QR */
    fun onQrScanned() = checkAchievement("qr_scan")

    /**
     * Carga logros y misiones sin necesitar un charId (útil desde pantallas
     * fuera de la partida, como el Salón de la Fama accedido desde el Home).
     */
    fun load() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            loadAchievements(uid)
            loadQuests(uid, "")
        }
    }

    // ── Lógica interna ────────────────────────────────────────────────────────

    private fun checkAchievement(achievementId: String) {
        val uid = auth.currentUser?.uid ?: return
        val current = _achievements.value.firstOrNull { it.id == achievementId } ?: return
        if (current.isUnlocked) return

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                db.collection("users").document(uid)
                    .collection("achievements").document(achievementId)
                    .set(mapOf("isUnlocked" to true, "unlockedAt" to now), SetOptions.merge())
                    .await()

                val unlocked = current.copy(isUnlocked = true, unlockedAt = now)
                _achievements.value = _achievements.value.map {
                    if (it.id == achievementId) unlocked else it
                }
                _pendingAchievementXp.value += unlocked.xpReward
                _newAchievement.emit(unlocked)
                Log.d("AchievVM", "Logro desbloqueado: ${unlocked.title}")
            } catch (e: Exception) {
                Log.e("AchievVM", "checkAchievement: ${e.message}")
            }
        }
    }

    /**
     * @param absolute si > 0, establece el valor directamente (útil para niveles)
     */
    private fun updateQuestProgress(
        charId: String,
        objectiveType: QuestObjectiveType,
        increment: Int,
        absolute: Int = 0
    ) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val updatedQuests = _quests.value.map { quest ->
                if (quest.status != QuestStatus.IN_PROGRESS) return@map quest

                val updatedObjectives = quest.objectives.map { obj ->
                    if (obj.type != objectiveType) return@map obj
                    val newVal = if (absolute > 0) maxOf(obj.currentValue, absolute)
                    else (obj.currentValue + increment)
                    obj.copy(currentValue = newVal.coerceAtMost(obj.targetValue))
                }

                val wasCompleted = quest.isCompleted
                val newQuest = quest.copy(objectives = updatedObjectives)

                if (!wasCompleted && newQuest.isCompleted) {
                    val completed = newQuest.copy(
                        status      = QuestStatus.COMPLETED,
                        completedAt = System.currentTimeMillis()
                    )
                    saveQuestToFirestore(uid, completed)
                    _pendingAchievementXp.value += completed.xpReward
                    _completedQuest.emit(completed)
                    // Desbloquear siguiente misión automáticamente
                    unlockNextQuest(uid, completed.id)
                    return@map completed
                } else {
                    saveQuestToFirestore(uid, newQuest)
                    return@map newQuest
                }
            }
            _quests.value = updatedQuests
        }
    }

    /** Acepta la siguiente misión disponible en orden del catálogo */
    private suspend fun unlockNextQuest(uid: String, completedQuestId: String) {
        val completedIndex = QuestCatalog.all.indexOfFirst { it.id == completedQuestId }
        val nextQuest = QuestCatalog.all.getOrNull(completedIndex + 1) ?: return
        val alreadyStarted = _quests.value.any {
            it.id == nextQuest.id && it.status != QuestStatus.AVAILABLE
        }
        if (alreadyStarted) return
        val accepted = nextQuest.copy(
            status     = QuestStatus.IN_PROGRESS,
            acceptedAt = System.currentTimeMillis()
        )
        saveQuestToFirestore(uid, accepted)
        _quests.value = _quests.value.map { if (it.id == nextQuest.id) accepted else it }
    }

    private suspend fun saveQuestToFirestore(uid: String, quest: Quest) {
        try {
            val objectivesData = quest.objectives.map {
                mapOf("currentValue" to it.currentValue, "targetValue" to it.targetValue)
            }
            db.collection("users").document(uid)
                .collection("quests").document(quest.id)
                .set(
                    mapOf(
                        "status"      to quest.status.name,
                        "objectives"  to objectivesData,
                        "acceptedAt"  to quest.acceptedAt,
                        "completedAt" to quest.completedAt
                    ),
                    SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            Log.e("AchievVM", "saveQuestToFirestore: ${e.message}")
        }
    }

    private suspend fun getCombatWins(charId: String): Int {
        return try {
            val doc = db.collection("partidas").document(charId)
                .collection("stats").document("combat").get().await()
            doc.getLong("wins")?.toInt() ?: 0
        } catch (e: Exception) { 0 }
    }

    // ── Aceptar misión manualmente ────────────────────────────────────────────

    fun acceptQuest(questId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val quest = _quests.value.firstOrNull { it.id == questId } ?: return@launch
            if (quest.status != QuestStatus.AVAILABLE) return@launch
            val accepted = quest.copy(
                status     = QuestStatus.IN_PROGRESS,
                acceptedAt = System.currentTimeMillis()
            )
            saveQuestToFirestore(uid, accepted)
            _quests.value = _quests.value.map { if (it.id == questId) accepted else it }
        }
    }

    // ── Estadísticas resumidas ────────────────────────────────────────────────

    val unlockedCount: Int get() = _achievements.value.count { it.isUnlocked }
    val totalCount: Int    get() = _achievements.value.size
    val completedQuestCount: Int get() = _quests.value.count { it.status == QuestStatus.COMPLETED }
}