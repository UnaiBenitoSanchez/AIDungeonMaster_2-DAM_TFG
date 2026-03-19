package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.Character
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
                    name           = name,
                    race           = race,
                    characterClass = clazz,
                    stats          = stats,
                    physicalTraits = physicalTraits,
                    portraitUrl    = portraitUrl
                )

                // Guardamos directamente en la colección del usuario
                db.collection("users")
                    .document(userId)
                    .collection("characters")
                    .add(charData)
                    .await()

                Log.d("APP_SUCCESS", "Personaje guardado correctamente en Firebase")

            } catch (e: Exception) {
                Log.e("APP_ERROR", "Error al guardar en base de datos: ${e.message}", e)
            }
        }
    }

    fun fetchCharacters() {
        val userId = auth.currentUser?.uid ?: return

        // Listener en tiempo real: Si añades uno, la lista se actualiza sola
        db.collection("users")
            .document(userId)
            .collection("characters")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("APP_ERROR", "Error escuchando cambios: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Character::class.java)?.copy(id = doc.id)
                    }
                    _characters.value = list
                    Log.d("APP_FIRESTORE", "Lista actualizada: ${list.size} personajes")
                }
            }
    }

    fun deleteCharacter(characterId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("characters")
                    .document(characterId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("APP_ERROR", "Error eliminando: ${e.message}")
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        onLogout()
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

}