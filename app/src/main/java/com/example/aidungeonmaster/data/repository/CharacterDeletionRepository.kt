package com.example.aidungeonmaster.data.repository

import android.util.Log
import com.google.firebase.firestore.CollectionReference
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
        if (userId.isBlank()) return
        if (characterName.isBlank()) return

        val charId = "${userId}_${characterName}"

        Log.d("CHAR_DELETE", "Borrando personaje completo: $charId")

        // 1. Borrar documento del personaje en users/{uid}/characters
        deleteCharacterDocument(
            userId = userId,
            characterName = characterName,
            userCharacterDocId = userCharacterDocId
        )

        // 2. Borrar partidas relacionadas
        deletePartidas(
            userId = userId,
            characterName = characterName
        )

        // 3. Borrar documentos extra. No deben romper el borrado si fallan.
        runCatching {
            db.collection("ranking")
                .document(charId)
                .delete()
                .await()
        }

        runCatching {
            db.collection("bank_accounts")
                .document(charId)
                .delete()
                .await()
        }

        Log.d("CHAR_DELETE", "Borrado terminado: $charId")
    }

    private suspend fun deleteCharacterDocument(
        userId: String,
        characterName: String,
        userCharacterDocId: String?
    ) {
        val charactersRef = db.collection("users")
            .document(userId)
            .collection("characters")

        if (!userCharacterDocId.isNullOrBlank()) {
            val docRef = charactersRef.document(userCharacterDocId)

            // Las subcollecciones se borran por separado: si fallan por permisos
            // (la regla catch-all las bloquea) no deben impedir borrar el documento
            // principal del personaje.
            runCatching { deleteCollection(docRef.collection("inventory")) }
            runCatching { deleteCollection(docRef.collection("stats")) }
            runCatching { deleteCollection(docRef.collection("logs")) }
            runCatching { docRef.delete().await() }
        }

        // Extra de seguridad por si el ID no coincide o el personaje fue creado con add()
        val byName = charactersRef
            .whereEqualTo("name", characterName)
            .get()
            .await()

        byName.documents.forEach { doc ->
            // Igual que arriba: subcollecciones aisladas del borrado principal
            runCatching { deleteCollection(doc.reference.collection("inventory")) }
            runCatching { deleteCollection(doc.reference.collection("stats")) }
            runCatching { deleteCollection(doc.reference.collection("logs")) }
            runCatching { doc.reference.delete().await() }
        }
    }

    private suspend fun deletePartidas(
        userId: String,
        characterName: String
    ) {
        val baseCharId = "${userId}_${characterName}"
        val prefix = "${baseCharId}_"

        // Documento base: partidas/{uid}_{personaje}
        deletePartidaDocument(baseCharId)

        // Documentos que tengan campos userId + characterName
        runCatching {
            val byFields = db.collection("partidas")
                .whereEqualTo("userId", userId)
                .whereEqualTo("characterName", characterName)
                .get()
                .await()

            byFields.documents.forEach { doc ->
                deletePartidaDocument(doc.id)
            }
        }

        // Fallback por ID prefijo: busca SOLO las partidas del usuario (whereEqualTo userId)
        // y filtra client-side las que coincidan con el personaje.
        // NOTA: requiere que la regla de Firebase sea  allow read: if resource.data.userId == request.auth.uid
        // para que Firestore valide esta query de colección correctamente.
        runCatching {
            val myPartidas = db.collection("partidas")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            myPartidas.documents
                .filter { doc ->
                    doc.id == baseCharId || doc.id.startsWith(prefix)
                }
                .forEach { doc ->
                    deletePartidaDocument(doc.id)
                }
        }
    }

    private suspend fun deletePartidaDocument(partidaId: String) {
        if (partidaId.isBlank()) return

        val partidaRef = db.collection("partidas").document(partidaId)

        val subcollections = listOf(
            "bestiary",
            "journal",
            "worldMap",
            "stats",
            "personalRoom",
            "inventory",
            "messages",
            "chat",
            "events",
            "logs",
            "quests",
            "achievements"
        )

        subcollections.forEach { sub ->
            runCatching {
                deleteCollection(partidaRef.collection(sub))
            }
        }

        runCatching {
            partidaRef.delete().await()
        }
    }

    private suspend fun deleteCollection(collectionRef: CollectionReference) {
        while (true) {
            val snap = collectionRef
                .limit(100)
                .get()
                .await()

            if (snap.isEmpty) break

            val batch = db.batch()

            snap.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
        }
    }
}