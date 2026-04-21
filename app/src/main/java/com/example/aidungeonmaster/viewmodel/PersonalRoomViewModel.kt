package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.PERSONAL_ROOM_CATALOG
import com.example.aidungeonmaster.data.model.PersonalRoomPlacedDecoration
import com.example.aidungeonmaster.data.model.PersonalRoomState
import com.example.aidungeonmaster.data.model.personalRoomDecorationById
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PersonalRoomViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _roomState = MutableStateFlow(PersonalRoomState())
    val roomState = _roomState.asStateFlow()

    private var currentCharId: String = ""

    fun loadRoom(charId: String) {
        currentCharId = charId
        viewModelScope.launch {
            try {
                val doc = db.collection("partidas")
                    .document(charId)
                    .collection("personalRoom")
                    .document("state")
                    .get()
                    .await()

                if (!doc.exists()) {
                    _roomState.value = PersonalRoomState()
                    return@launch
                }

                val owned = (doc.get("ownedDecorationIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.distinct()
                    ?: emptyList()

                val placed = (doc.get("placedDecorations") as? List<*>)
                    ?.mapNotNull { raw ->
                        val map = raw as? Map<*, *> ?: return@mapNotNull null
                        val decorationId = map["decorationId"] as? String ?: return@mapNotNull null
                        val slotId = map["slotId"] as? String ?: return@mapNotNull null
                        PersonalRoomPlacedDecoration(
                            decorationId = decorationId,
                            slotId = slotId
                        )
                    }
                    ?: emptyList()

                _roomState.value = PersonalRoomState(
                    ownedDecorationIds = owned,
                    placedDecorations = placed,
                    roomTheme = doc.getString("roomTheme") ?: "fortaleza",
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e("ROOM_VM", "loadRoom: ${e.message}", e)
            }
        }
    }

    fun buyDecoration(
        charId: String,
        decorationId: String,
        spendCoins: suspend (Int) -> Boolean,
        onResult: (String) -> Unit = {}
    ) {
        val decoration = personalRoomDecorationById(decorationId)
        if (decoration == null) {
            onResult("❌ Decoración no encontrada.")
            return
        }

        if (_roomState.value.ownedDecorationIds.contains(decorationId)) {
            onResult("✅ Ya habías comprado ${decoration.name}.")
            return
        }

        viewModelScope.launch {
            try {
                val paid = spendCoins(decoration.price)
                if (!paid) {
                    onResult("❌ No tienes suficientes monedas para ${decoration.name}.")
                    return@launch
                }

                val newOwned = (_roomState.value.ownedDecorationIds + decorationId).distinct()
                _roomState.value = _roomState.value.copy(
                    ownedDecorationIds = newOwned,
                    updatedAt = System.currentTimeMillis()
                )
                saveRoom(charId)
                onResult("✅ Has comprado ${decoration.emoji} ${decoration.name}.")
            } catch (e: Exception) {
                Log.e("ROOM_VM", "buyDecoration: ${e.message}", e)
                onResult("❌ No se pudo comprar ${decoration.name}.")
            }
        }
    }

    fun placeDecoration(decorationId: String, onResult: (String) -> Unit = {}) {
        val state = _roomState.value
        val decoration = personalRoomDecorationById(decorationId)
        if (decoration == null) {
            onResult("❌ Decoración no encontrada.")
            return
        }

        if (!state.ownedDecorationIds.contains(decorationId)) {
            onResult("❌ Primero debes comprar ${decoration.name}.")
            return
        }

        if (state.placedDecorations.any { it.decorationId == decorationId }) {
            onResult("✅ ${decoration.name} ya está colocada.")
            return
        }

        val occupiedSlots = state.placedDecorations.map { it.slotId }.toSet()
        val freeSlot = decoration.allowedSlots.firstOrNull { it !in occupiedSlots }

        if (freeSlot == null) {
            onResult("❌ No hay huecos libres para ${decoration.name}.")
            return
        }

        _roomState.value = state.copy(
            placedDecorations = state.placedDecorations + PersonalRoomPlacedDecoration(
                decorationId = decorationId,
                slotId = freeSlot
            ),
            updatedAt = System.currentTimeMillis()
        )

        persistCurrentRoom()
        onResult("🏠 ${decoration.name} colocada.")
    }

    fun removeDecoration(decorationId: String, onResult: (String) -> Unit = {}) {
        val state = _roomState.value
        val decoration = personalRoomDecorationById(decorationId)

        if (state.placedDecorations.none { it.decorationId == decorationId }) {
            onResult("❌ Esa decoración no está puesta.")
            return
        }

        _roomState.value = state.copy(
            placedDecorations = state.placedDecorations.filterNot { it.decorationId == decorationId },
            updatedAt = System.currentTimeMillis()
        )

        persistCurrentRoom()
        onResult("📦 ${decoration?.name ?: "Decoración"} guardada.")
    }

    private fun persistCurrentRoom() {
        if (currentCharId.isBlank()) return
        viewModelScope.launch {
            saveRoom(currentCharId)
        }
    }

    private suspend fun saveRoom(charId: String) {
        val state = _roomState.value
        val safeOwned = state.ownedDecorationIds.filter { id ->
            PERSONAL_ROOM_CATALOG.any { it.id == id }
        }
        val safePlaced = state.placedDecorations.filter { placed ->
            PERSONAL_ROOM_CATALOG.any { it.id == placed.decorationId }
        }

        val data = mapOf(
            "ownedDecorationIds" to safeOwned,
            "placedDecorations" to safePlaced.map {
                mapOf(
                    "decorationId" to it.decorationId,
                    "slotId" to it.slotId
                )
            },
            "roomTheme" to state.roomTheme,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("partidas")
            .document(charId)
            .collection("personalRoom")
            .document("state")
            .set(data, SetOptions.merge())
            .await()
    }
}
