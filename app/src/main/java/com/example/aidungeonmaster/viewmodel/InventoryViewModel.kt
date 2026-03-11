package com.example.aidungeonmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InventoryViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _character = MutableStateFlow<Character?>(null)
    val character = _character.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()


    // CARGA Y CREA DATOS EN FIREBASE
    fun loadInventory(gameId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docRef = db.collection("partidas").document(gameId)
                val snapshot = docRef.get().await()

                if (snapshot.exists()) {
                    val charData = snapshot.toObject(Character::class.java)

                    // Si el documento existe pero no tiene vida/inventario (porque viene del GameViewModel viejo)
                    if (charData?.hpMax == 0 || charData == null) {
                        val initializedChar = Character(
                            id = gameId,
                            name = gameId.split("_").getOrNull(1) ?: "Héroe",
                            hpMax = 20,
                            hpCurrent = 20,
                            inventory = emptyList()
                        )
                        // Fusionamos los datos nuevos con lo que ya hubiera (como el chat)
                        docRef.set(initializedChar, com.google.firebase.firestore.SetOptions.merge()).await()
                        _character.value = initializedChar
                    } else {
                        _character.value = charData
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // GUARDA EL OBJETO DEL QR EN FIREBASE
    fun addItemToInventory(gameId: String, newItem: Item) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas").document(gameId)
                val snapshot = docRef.get().await()

                val currentChar = snapshot.toObject(Character::class.java) ?: Character()
                val updatedInventory = currentChar.inventory + newItem

                // Usamos update para no machacar el chatHistory
                docRef.update(
                    "inventory", updatedInventory,
                    "hpMax", 20,      // Aprovechamos para inicializar la vida si no existe
                    "hpCurrent", 20
                ).await()

                _character.value = currentChar.copy(inventory = updatedInventory)
            } catch (e: Exception) {
                // Si el documento es muy viejo y no tiene los campos, update fallará.
                // En ese caso usamos set con merge
                val docRef = db.collection("partidas").document(gameId)
                docRef.set(mapOf(
                    "inventory" to listOf(newItem),
                    "hpMax" to 20,
                    "hpCurrent" to 20
                ), com.google.firebase.firestore.SetOptions.merge()).await()
            }
        }
    }
}