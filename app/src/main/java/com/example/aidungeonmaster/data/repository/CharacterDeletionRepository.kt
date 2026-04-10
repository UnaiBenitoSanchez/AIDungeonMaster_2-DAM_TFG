package com.example.aidungeonmaster.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CharacterDeletionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun deleteEverywhere(
        userId: String,
        characterName: String,
        userCharacterDocId: String? = null
    ) {
        val charId = buildCharacterId(userId, characterName)

        try {
            // 1) Borrar el personaje de users/{uid}/characters
            if (!userCharacterDocId.isNullOrBlank()) {
                db.collection("users")
                    .document(userId)
                    .collection("characters")
                    .document(userCharacterDocId)
                    .delete()
                    .await()
            } else {
                val chars = db.collection("users")
                    .document(userId)
                    .collection("characters")
                    .whereEqualTo("name", characterName)
                    .get()
                    .await()

                for (doc in chars.documents) {
                    doc.reference.delete().await()
                }
            }

            // 2) Borrar ranking global
            db.collection("ranking")
                .document(charId)
                .delete()
                .await()

            // 3) Borrar cuenta bancaria
            db.collection("bank_accounts")
                .document(charId)
                .delete()
                .await()

            // 4) Borrar TODAS las partidas relacionadas con el personaje
            //    Esto incluye:
            //    - partidas/{uid}_{characterName}
            //    - partidas/{uid}_{characterName}_{theme}
            //    - cualquier otra variante cuyo docId empiece por ese prefijo
            val prefix = "${userId}_${characterName}"
            val partidasSnapshot = db.collection("partidas")
                .get()
                .await()

            val matchingDocs = partidasSnapshot.documents.filter { doc ->
                doc.id == prefix || doc.id.startsWith("${prefix}_")
            }

            for (doc in matchingDocs) {
                deleteGameDocumentCompletely(doc.id)
            }

            Log.d("CHAR_DELETE", "Borrado completo realizado para $charId")
        } catch (e: Exception) {
            Log.e("CHAR_DELETE", "Error borrando personaje completamente: ${e.message}", e)
            throw e
        }
    }

    private suspend fun deleteGameDocumentCompletely(gameDocId: String) {
        val gameRef = db.collection("partidas").document(gameDocId)

        deleteSubcollection(gameRef.collection("bestiary"))
        deleteSubcollection(gameRef.collection("journal"))
        deleteSubcollection(gameRef.collection("worldMap"))
        deleteSubcollection(gameRef.collection("stats"))

        gameRef.delete().await()
    }

    private suspend fun deleteSubcollection(
        collection: com.google.firebase.firestore.CollectionReference
    ) {
        val snapshot = collection.get().await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }

    private fun buildCharacterId(userId: String, characterName: String): String {
        return "${userId}_${characterName}"
    }
}