package com.example.aidungeonmaster.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.aidungeonmaster.data.model.Game
import com.google.firebase.firestore.FirebaseFirestore

// Repositorio que centraliza el acceso a datos de game.
class GameRepository {

    private val db = FirebaseFirestore.getInstance()

    @RequiresApi(Build.VERSION_CODES.O)
    // Obtiene games by user.
    fun getGamesByUser(
        userId: String,
        onResult: (List<Game>) -> Unit
    ) {
        db.collection("games")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val games = result.documents.mapNotNull {
                    it.toObject(Game::class.java)?.copy(id = it.id)
                }
                onResult(games)
            }
    }

    // Crea game.
    fun createGame(game: Game) {
        db.collection("games").add(game)
    }
}
