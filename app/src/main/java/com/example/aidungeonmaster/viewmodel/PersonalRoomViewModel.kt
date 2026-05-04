package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.PersonalRoomPlacedDecoration
import com.example.aidungeonmaster.data.model.PersonalRoomState
import com.example.aidungeonmaster.data.model.personalRoomDecorationById
import com.example.aidungeonmaster.data.model.personalRoomSlotById
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel que coordina el estado y la lógica de personal room.
class PersonalRoomViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _roomState = MutableStateFlow(PersonalRoomState())
    val roomState = _roomState.asStateFlow()

    private var currentCharId: String = ""

    // Carga room.
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

    // Ejecuta la lógica de buy decoration.
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

    // Ejecuta la lógica de place decoration.
    fun placeDecoration(
        decorationId: String,
        slotId: String,
        onResult: (String) -> Unit = {}
    ) {
        val state = _roomState.value
        val decoration = personalRoomDecorationById(decorationId)
        val slot = personalRoomSlotById(slotId)

        if (decoration == null) {
            onResult("❌ Decoración no encontrada.")
            return
        }

        if (slot == null) {
            onResult("❌ Baldosa no válida.")
            return
        }

        if (!state.ownedDecorationIds.contains(decorationId)) {
            onResult("❌ Primero debes comprar ${decoration.name}.")
            return
        }

        if (slotId !in decoration.allowedSlots) {
            onResult("❌ ${decoration.name} no se puede colocar en ${slot.label}.")
            return
        }

        val occupied = state.placedDecorations.firstOrNull { it.slotId == slotId }
        if (occupied != null && occupied.decorationId != decorationId) {
            val occupiedName = personalRoomDecorationById(occupied.decorationId)?.name ?: "otra decoración"
            onResult("❌ ${slot.label} ya está ocupada por $occupiedName.")
            return
        }

        val currentPlacement = state.placedDecorations.firstOrNull { it.decorationId == decorationId }
        if (currentPlacement?.slotId == slotId) {
            onResult("✅ ${decoration.name} ya está en ${slot.label}.")
            return
        }

        val updatedPlaced = state.placedDecorations
            .filterNot { it.decorationId == decorationId }
            .filterNot { it.slotId == slotId }
            .plus(
                PersonalRoomPlacedDecoration(
                    decorationId = decorationId,
                    slotId = slotId
                )
            )

        _roomState.value = state.copy(
            placedDecorations = updatedPlaced,
            updatedAt = System.currentTimeMillis()
        )

        persistCurrentRoom()

        onResult(
            if (currentPlacement == null) {
                "🏠 ${decoration.name} colocada en ${slot.label}."
            } else {
                "🔁 ${decoration.name} movida a ${slot.label}."
            }
        )
    }

    // Elimina decoration.
    fun removeDecoration(decorationId: String, onResult: (String) -> Unit = {}) {
        val state = _roomState.value
        val decoration = personalRoomDecorationById(decorationId)
        val placement = state.placedDecorations.firstOrNull { it.decorationId == decorationId }

        if (placement == null) {
            onResult("❌ Esa decoración no está puesta.")
            return
        }

        _roomState.value = state.copy(
            placedDecorations = state.placedDecorations.filterNot { it.decorationId == decorationId },
            updatedAt = System.currentTimeMillis()
        )

        persistCurrentRoom()

        val slotName = personalRoomSlotById(placement.slotId)?.label ?: "esa baldosa"
        onResult("↩️ ${decoration?.name ?: "Decoración"} retirada de $slotName.")
    }

    // Elimina decoration from slot.
    fun removeDecorationFromSlot(slotId: String, onResult: (String) -> Unit = {}) {
        val placed = _roomState.value.placedDecorations.firstOrNull { it.slotId == slotId }
        if (placed == null) {
            onResult("❌ No hay decoración en esa baldosa.")
            return
        }
        removeDecoration(placed.decorationId, onResult)
    }

    // Ejecuta la lógica de persist current room.
    private fun persistCurrentRoom() {
        if (currentCharId.isBlank()) return
        viewModelScope.launch {
            saveRoom(currentCharId)
        }
    }

    private suspend fun saveRoom(charId: String) {
        try {
            val state = _roomState.value
            val data = mapOf(
                "ownedDecorationIds" to state.ownedDecorationIds,
                "placedDecorations" to state.placedDecorations.map {
                    mapOf(
                        "decorationId" to it.decorationId,
                        "slotId" to it.slotId
                    )
                },
                "roomTheme" to state.roomTheme,
                "updatedAt" to state.updatedAt
            )

            db.collection("partidas")
                .document(charId)
                .collection("personalRoom")
                .document("state")
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("ROOM_VM", "saveRoom: ${e.message}", e)
        }
    }
}
