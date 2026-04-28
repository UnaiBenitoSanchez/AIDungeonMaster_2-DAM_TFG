package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.repository.CharacterDeletionRepository
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val socialRepository = SocialRepository()

    private val deletionRepository = CharacterDeletionRepository()

    private var charactersListener: ListenerRegistration? = null
    private var charactersListenerUserId: String? = null

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters = _characters.asStateFlow()

    init { fetchCharacters() }

    fun saveCharacter(
        name: String,
        race: String,
        clazz: String,
        stats: Map<String, Int>,
        physicalTraits: String,
        portraitUrl: String = ""
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                Log.d("APP_FIRESTORE", "Guardando personaje...")

                val charData = Character(
                    name = name,
                    race = race,
                    characterClass = clazz,
                    stats = stats,
                    physicalTraits = physicalTraits,
                    portraitUrl = portraitUrl
                )

                // 1. Guardar en users/{uid}/characters
                db.collection("users")
                    .document(userId)
                    .collection("characters")
                    .add(charData)
                    .await()

                // 2. Crear documento base en partidas/{userId}_{name}
                val charId = "${userId}_${name}"
                db.collection("partidas")
                    .document(charId)
                    .set(
                        mapOf(
                            "characterName" to name,
                            "characterClass" to clazz,
                            "userId" to userId,
                            "hpMax" to 20,
                            "hpCurrent" to 20,
                            "inventory" to emptyList<Any>(),
                            "lastPlayed" to 0L,
                            "xp" to 0,
                            "level" to 1
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()

                // 3. Escribir en ranking global
                db.collection("ranking")
                    .document(charId)
                    .set(
                        RankingViewModel.buildRankingData(name, race, clazz, stats, hpMax = 20),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()

                // 4. Actualizar contador público de personajes
                db.collection("users")
                    .document(userId)
                    .update(
                        mapOf(
                            "characterCount" to FieldValue.increment(1),
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()

                Log.d("APP_SUCCESS", "Personaje, partida, ranking y contador guardados correctamente")

            } catch (e: Exception) {
                Log.e("APP_ERROR", "Error al guardar: ${e.message}", e)
            }
        }
    }

    fun fetchCharacters(forceRefresh: Boolean = false) {
        val userId = auth.currentUser?.uid

        if (userId.isNullOrBlank()) {
            charactersListener?.remove()
            charactersListener = null
            charactersListenerUserId = null
            _characters.value = emptyList()
            return
        }

        if (!forceRefresh && charactersListenerUserId == userId && charactersListener != null) {
            return
        }

        charactersListener?.remove()
        charactersListenerUserId = userId

        charactersListener = db.collection("users")
            .document(userId)
            .collection("characters")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("APP_ERROR", "Error escuchando cambios: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val baseList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Character::class.java)?.copy(id = doc.id)
                    }

                    viewModelScope.launch {
                        syncCharacterCount(userId, baseList.size)

                        val enriched = baseList.map { char ->
                            try {
                                val charId = "${userId}_${char.name}"
                                val partidaSnap = db.collection("partidas")
                                    .document(charId)
                                    .get()
                                    .await()

                                if (partidaSnap.exists()) {
                                    char.copy(
                                        hpMax = partidaSnap.getLong("hpMax")?.toInt() ?: char.hpMax,
                                        hpCurrent = partidaSnap.getLong("hpCurrent")?.toInt() ?: char.hpCurrent,
                                        lastPlayed = partidaSnap.getLong("lastPlayed") ?: 0L,
                                        xp = partidaSnap.getLong("xp")?.toInt() ?: char.xp,
                                        level = partidaSnap.getLong("level")?.toInt() ?: char.level
                                    )
                                } else {
                                    char
                                }
                            } catch (e: Exception) {
                                char
                            }
                        }

                        _characters.value = enriched.sortedByDescending { it.lastPlayed }
                        Log.d("APP_FIRESTORE", "Lista actualizada: ${enriched.size} personajes")
                    }
                }
            }
    }

    /**
     * Refresca solo el HP y lastPlayed desde partidas sin relanzar el snapshot listener.
     * Se llama cada vez que HomeScreen vuelve a ser visible.
     */
    fun refreshHp() {
        val userId = auth.currentUser?.uid ?: return
        val current = _characters.value
        if (current.isEmpty()) return

        viewModelScope.launch {
            val enriched = current.map { char ->
                try {
                    val charId = "${userId}_${char.name}"
                    val snap = db.collection("partidas").document(charId).get().await()
                    if (snap.exists()) {
                        char.copy(
                            hpMax = snap.getLong("hpMax")?.toInt() ?: char.hpMax,
                            hpCurrent = snap.getLong("hpCurrent")?.toInt() ?: char.hpCurrent,
                            lastPlayed = snap.getLong("lastPlayed") ?: char.lastPlayed,
                            xp = snap.getLong("xp")?.toInt() ?: char.xp,
                            level = snap.getLong("level")?.toInt() ?: char.level
                        )
                    } else {
                        char
                    }
                } catch (e: Exception) {
                    char
                }
            }
            _characters.value = enriched.sortedByDescending { it.lastPlayed }
        }
    }

    fun deleteCharacter(characterId: String, characterName: String) {
        val userId = auth.currentUser?.uid ?: return

        // Quitar la tarjeta al instante para que no parezca que no funciona.
        _characters.value = _characters.value.filterNot {
            it.id == characterId || it.name == characterName
        }

        viewModelScope.launch {
            try {
                deletionRepository.deleteEverywhere(
                    userId = userId,
                    characterName = characterName,
                    userCharacterDocId = characterId
                )

                syncCharacterCount(userId, _characters.value.size)

                Log.d("APP_SUCCESS", "Personaje eliminado completamente: ${userId}_${characterName}")
            } catch (e: Exception) {
                Log.e("APP_ERROR", "Error eliminando personaje completo: ${e.message}", e)

                // Si algo falla, recargamos para no dejar la UI en estado raro.
                fetchCharacters(forceRefresh = true)
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            runCatching { socialRepository.updatePresence(false) }
            FirebaseAuth.getInstance().signOut()
            onLogout()
        }
    }

    fun updateCharacterTheme(characterId: String, theme: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("characters")
                    .document(characterId)
                    .update("gameTheme", theme)
                    .await()
            } catch (e: Exception) {
                Log.e("APP_ERROR", "Error actualizando tema: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        charactersListener?.remove()
        charactersListener = null
        super.onCleared()
    }

    private suspend fun syncCharacterCount(userId: String, count: Int) {
        runCatching {
            db.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "characterCount" to count,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
        }.onFailure {
            Log.w("APP_WARN", "No se pudo sincronizar characterCount: ${it.message}")
        }
    }
}