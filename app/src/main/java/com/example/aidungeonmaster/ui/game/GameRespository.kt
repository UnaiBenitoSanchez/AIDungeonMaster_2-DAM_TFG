package com.example.aidungeonmaster.ui.game

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Repositorio que centraliza el acceso a datos de game.
class GameRepository {
    private val db = FirebaseFirestore.getInstance()
    private val gamesCollection = db.collection("partidas")

    // Guardar o actualizar partida
    suspend fun saveGame(gameId: String, gameData: Map<String, Any>) {
        try {
            gamesCollection.document(gameId).set(gameData).await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Cargar una partida específica
    suspend fun loadGame(gameId: String): Map<String, Any>? {
        return try {
            val snapshot = gamesCollection.document(gameId).get().await()
            snapshot.data
        } catch (e: Exception) {
            null
        }
    }
}
