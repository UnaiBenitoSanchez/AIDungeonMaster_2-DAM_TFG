package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InventoryViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _character = MutableStateFlow<Character?>(null)
    val character = _character.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    /**
     * Emite el nuevo nivel cuando el personaje sube.
     * GamePlayScreen lo escucha para mostrar el diálogo de subida de nivel.
     * replay=0 → cada evento se consume una sola vez.
     */
    private val _levelUpEvent = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    val levelUpEvent = _levelUpEvent.asSharedFlow()

    // ── CARGA INVENTARIO DESDE FIREBASE ──────────────────────────────────────
    fun loadInventory(gameId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _character.value = null
            try {
                val snapshot = db.collection("partidas").document(gameId).get().await()
                if (snapshot.exists()) {
                    val hpMax = snapshot.getLong("hpMax")?.toInt() ?: 20
                    val hpCurrent = snapshot.getLong("hpCurrent")?.toInt() ?: 20
                    val rawName = snapshot.getString("characterName") ?: ""
                    val rawClass = snapshot.getString("characterClass") ?: ""
                    val xp = snapshot.getLong("xp")?.toInt() ?: 0
                    val level = snapshot.getLong("level")?.toInt() ?: 1
                    val coins = snapshot.getLong("coins")?.toInt() ?: 0

                    val rawInv = snapshot.get("inventory") as? List<Map<String, Any>> ?: emptyList()
                    val inventory = rawInv.map { m ->
                        Item(
                            id = m["id"] as? String ?: System.currentTimeMillis().toString(),
                            name = m["name"] as? String ?: "Objeto sin nombre",
                            description = m["description"] as? String ?: "",
                            type = m["type"] as? String ?: "consumible",
                            effect = m["effect"] as? String ?: ""
                        )
                    }

                    _character.value = Character(
                        id = gameId,
                        name = rawName,
                        characterClass = rawClass,
                        inventory = inventory,
                        hpMax = hpMax,
                        hpCurrent = hpCurrent,
                        xp = xp,
                        level = level,
                        coins = coins
                    )
                } else {
                    _character.value = Character(
                        id = gameId,
                        hpMax = 20,
                        hpCurrent = 20,
                        inventory = emptyList(),
                        xp = 0,
                        level = 1
                    )
                    Log.d(
                        "INVENTORY_DEBUG",
                        "loadInventory: doc $gameId no existe, inicializando con defaults"
                    )
                }
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "loadInventory: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── ACTUALIZA HP EN FIREBASE Y EN EL STATE ────────────────────────────────
    fun updateHp(gameId: String, newHp: Int) {
        viewModelScope.launch {
            try {
                db.collection("partidas").document(gameId)
                    .update("hpCurrent", newHp).await()
                _character.value = _character.value?.copy(hpCurrent = newHp)

                db.collection("ranking").document(gameId)
                    .update("hpCurrent", newHp.toLong())
                    .await()

                Log.d("INVENTORY_DEBUG", "HP actualizado: $newHp")
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "updateHp: ${e.message}")
            }
        }
    }

    // ── AÑADE XP Y GESTIONA SUBIDA DE NIVEL ──────────────────────────────────
    fun addXp(gameId: String, amount: Int) {
        viewModelScope.launch {
            try {
                val snap = db.collection("partidas").document(gameId).get().await()
                if (!snap.exists()) {
                    Log.w("INVENTORY_ERROR", "addXp: documento $gameId no existe")
                    return@launch
                }

                var currentXp = (snap.getLong("xp")?.toInt() ?: 0) + amount
                var currentLevel = snap.getLong("level")?.toInt() ?: 1
                var currentHpMax = snap.getLong("hpMax")?.toInt() ?: 20

                val char = _character.value
                val conMod = ((char?.stats?.get("Constitución") ?: 10) - 10) / 2

                while (currentXp >= currentLevel * 100) {
                    currentXp -= currentLevel * 100
                    currentLevel += 1
                    currentHpMax += (5 + conMod).coerceAtLeast(1)
                    Log.d("LEVEL_UP", "¡Nivel $currentLevel! HP máx: $currentHpMax")
                    _levelUpEvent.emit(currentLevel)
                }

                val didLevelUp = currentLevel > (snap.getLong("level")?.toInt() ?: 1)
                val newHpCurrent = if (didLevelUp) currentHpMax
                else snap.getLong("hpCurrent")?.toInt() ?: currentHpMax

                db.collection("partidas").document(gameId).update(
                    mapOf(
                        "xp" to currentXp,
                        "level" to currentLevel,
                        "hpMax" to currentHpMax,
                        "hpCurrent" to newHpCurrent
                    )
                ).await()

                _character.value = _character.value?.copy(
                    xp = currentXp,
                    level = currentLevel,
                    hpMax = currentHpMax,
                    hpCurrent = newHpCurrent
                ) ?: Character(
                    id = gameId,
                    xp = currentXp,
                    level = currentLevel,
                    hpMax = currentHpMax,
                    hpCurrent = newHpCurrent
                )

                Log.d(
                    "INVENTORY_DEBUG",
                    "XP guardado: +$amount → total ${currentXp}xp, nivel $currentLevel"
                )

            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "addXp: ${e.message}")
            }

            try {
                val currentLevel = _character.value?.level ?: 1
                val currentHpMax = _character.value?.hpMax ?: 20
                db.collection("ranking").document(gameId)
                    .set(
                        mapOf(
                            "level" to currentLevel.toLong(),
                            "hpMax" to currentHpMax.toLong()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
            } catch (e: Exception) {
                Log.w("INVENTORY_ERROR", "addXp ranking: ${e.message}")
            }
        }
    }

    // ── AÑADE MONEDAS Y LAS PERSISTE EN FIRESTORE ────────────────────────────
    fun addCoins(gameId: String, amount: Int) {
        if (amount <= 0) return
        viewModelScope.launch {
            try {
                val snap = db.collection("partidas").document(gameId).get().await()
                val current = snap.getLong("coins")?.toInt() ?: 0
                val newTotal = current + amount
                db.collection("partidas").document(gameId)
                    .update("coins", newTotal).await()
                _character.value = _character.value?.copy(coins = newTotal)
                Log.d("INVENTORY_DEBUG", "Monedas +$amount → total $newTotal")
            } catch (e: Exception) {
                try {
                    val current = _character.value?.coins ?: 0
                    val newTotal = current + amount
                    db.collection("partidas").document(gameId)
                        .set(
                            mapOf("coins" to newTotal),
                            com.google.firebase.firestore.SetOptions.merge()
                        ).await()
                    _character.value = _character.value?.copy(coins = newTotal)
                } catch (e2: Exception) {
                    Log.e("INVENTORY_ERROR", "addCoins: ${e2.message}")
                }
            }
        }
    }

    // ── REPUTACIÓN EN TIENDAS ─────────────────────────────────────────────────
    suspend fun loadShopReputation(gameId: String): Map<String, Int> {
        return try {
            val snap = db.collection("partidas").document(gameId).get().await()
            @Suppress("UNCHECKED_CAST")
            val raw = snap.get("shopReputation") as? Map<String, Long> ?: emptyMap()
            raw.mapValues { it.value.toInt() }
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "loadShopReputation: ${e.message}")
            emptyMap()
        }
    }

    suspend fun addShopReputation(gameId: String, shopKey: String, points: Int = 10): Int {
        return try {
            val snap = db.collection("partidas").document(gameId).get().await()
            @Suppress("UNCHECKED_CAST")
            val current = (snap.get("shopReputation") as? Map<String, Long>)
                ?.get(shopKey)?.toInt() ?: 0
            val newTotal = current + points
            db.collection("partidas").document(gameId)
                .update("shopReputation.$shopKey", newTotal).await()
            Log.d("INVENTORY_DEBUG", "Reputación $shopKey: +$points → $newTotal")
            newTotal
        } catch (e: Exception) {
            try {
                db.collection("partidas").document(gameId).set(
                    mapOf("shopReputation" to mapOf(shopKey to points)),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
                points
            } catch (e2: Exception) {
                Log.e("INVENTORY_ERROR", "addShopReputation: ${e2.message}")
                0
            }
        }
    }

    // ── BANCO DEL PERSONAJE ───────────────────────────────────────────────────
    suspend fun getBankAccount(gameId: String): Map<String, Any>? {
        return try {
            val snap = db.collection("bank_accounts").document(gameId).get().await()
            if (snap.exists()) snap.data else null
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "getBankAccount: ${e.message}")
            null
        }
    }

    suspend fun registerBankPin(gameId: String, pin: String): Boolean {
        if (!pin.matches(Regex("""\d{4}"""))) return false
        return try {
            val docRef = db.collection("bank_accounts").document(gameId)
            val existing = docRef.get().await()
            if (existing.exists()) return false

            docRef.set(
                mapOf(
                    "gameId" to gameId,
                    "pin" to pin,
                    "balance" to 0,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "registerBankPin: ${e.message}")
            false
        }
    }

    suspend fun verifyBankPin(gameId: String, pin: String): Boolean {
        return try {
            val snap = db.collection("bank_accounts").document(gameId).get().await()
            snap.exists() && snap.getString("pin") == pin
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "verifyBankPin: ${e.message}")
            false
        }
    }

    suspend fun depositToBank(gameId: String, amount: Int): Boolean {
        if (amount <= 0) return false
        return try {
            val gameRef = db.collection("partidas").document(gameId)
            val bankRef = db.collection("bank_accounts").document(gameId)

            val gameSnap = gameRef.get().await()
            val bankSnap = bankRef.get().await()

            if (!bankSnap.exists()) return false

            val currentCoins = gameSnap.getLong("coins")?.toInt() ?: 0
            if (currentCoins < amount) return false

            val currentBankBalance = bankSnap.getLong("balance")?.toInt() ?: 0
            val newCoins = currentCoins - amount
            val newBankBalance = currentBankBalance + amount

            gameRef.set(
                mapOf("coins" to newCoins),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            bankRef.update(
                mapOf(
                    "balance" to newBankBalance,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            _character.value = _character.value?.copy(coins = newCoins)
            true
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "depositToBank: ${e.message}")
            false
        }
    }

    suspend fun withdrawFromBank(gameId: String, amount: Int): Boolean {
        if (amount <= 0) return false
        return try {
            val gameRef = db.collection("partidas").document(gameId)
            val bankRef = db.collection("bank_accounts").document(gameId)

            val gameSnap = gameRef.get().await()
            val bankSnap = bankRef.get().await()

            if (!bankSnap.exists()) return false

            val currentCoins = gameSnap.getLong("coins")?.toInt() ?: 0
            val currentBankBalance = bankSnap.getLong("balance")?.toInt() ?: 0
            if (currentBankBalance < amount) return false

            val newCoins = currentCoins + amount
            val newBankBalance = currentBankBalance - amount

            gameRef.set(
                mapOf("coins" to newCoins),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            bankRef.update(
                mapOf(
                    "balance" to newBankBalance,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            _character.value = _character.value?.copy(coins = newCoins)
            true
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "withdrawFromBank: ${e.message}")
            false
        }
    }

    // ── GASTA MONEDAS (TIENDA) ────────────────────────────────────────────────
    suspend fun spendCoins(gameId: String, amount: Int): Boolean {
        return try {
            val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("partidas").document(gameId).get()
                .await()
            val current = snap.getLong("coins")?.toInt() ?: 0
            if (current < amount) return false
            val newTotal = current - amount
            db.collection("partidas").document(gameId)
                .update("coins", newTotal).await()
            _character.value = _character.value?.copy(coins = newTotal)
            Log.d("INVENTORY_DEBUG", "Monedas gastadas: -$amount → total $newTotal")
            true
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "spendCoins: ${e.message}")
            false
        }
    }

    // ── USA UN OBJETO DEL INVENTARIO ─────────────────────────────────────────
    fun useItem(gameId: String, item: Item, hpCurrent: Int, hpMax: Int): String {
        val effect = item.effect.lowercase().trim()
        return when {
            effect.startsWith("cura:") -> {
                val expr = effect.removePrefix("cura:").trim()
                val healed = rollDiceExpression(expr)
                val newHp = (hpCurrent + healed).coerceAtMost(hpMax)
                updateHp(gameId, newHp)
                removeItemFromInventory(gameId, item)
                "💚 ${item.name}: recuperas $healed HP ($newHp/$hpMax)"
            }

            effect.startsWith("daño:") || effect.startsWith("dano:") -> {
                val expr = effect.substringAfter(":").trim()
                val damage = rollDiceExpression(expr)
                removeItemFromInventory(gameId, item)
                "💥 ${item.name}: causa $damage de daño al usarlo"
            }

            effect.startsWith("veneno:") -> {
                val expr = effect.removePrefix("veneno:").trim()
                val dmg = rollDiceExpression(expr)
                removeItemFromInventory(gameId, item)
                "☠️ ${item.name}: veneno activo — $dmg de daño por turno"
            }

            effect.startsWith("explosivo:") -> {
                val expr = effect.removePrefix("explosivo:").trim()
                val dmg = rollDiceExpression(expr)
                removeItemFromInventory(gameId, item)
                "💣 ${item.name} explota: $dmg de daño en área"
            }

            item.type == "armadura" -> "🛡️ ${item.name} ya está equipado"
            item.type == "arma" -> "⚔️ ${item.name} ya está en tu mano"
            else -> {
                removeItemFromInventory(gameId, item)
                "✨ Usaste ${item.name}"
            }
        }
    }

    // ── REINICIA PERSONAJE AL MORIR ───────────────────────────────────────────
    fun resetCharacter(charId: String) {
        viewModelScope.launch {
            try {
                val snap = db.collection("partidas").document(charId).get().await()
                val originalHpMax = snap.getLong("hpMax")?.toInt() ?: 20

                db.collection("partidas").document(charId)
                    .update(
                        mapOf(
                            "hpCurrent" to originalHpMax,
                            "hpMax" to originalHpMax,
                            "inventory" to emptyList<Any>(),
                            "coins" to 0
                        )
                    ).await()

                _character.value = _character.value?.copy(
                    hpCurrent = originalHpMax,
                    hpMax = originalHpMax,
                    inventory = emptyList(),
                    coins = 0
                )
                Log.d(
                    "INVENTORY_DEBUG",
                    "Personaje reiniciado: HP=$originalHpMax, inventario vacío"
                )
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "resetCharacter: ${e.message}")
            }
        }
    }

    // ── GUARDA ITEM DEL QR EN FIREBASE ───────────────────────────────────────
    fun addItemToInventory(gameId: String, newItem: Item) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas").document(gameId)
                val snapshot = docRef.get().await()

                val rawInv = snapshot.get("inventory") as? List<Map<String, Any>> ?: emptyList()
                val newEntry = mapOf(
                    "id" to (newItem.id.ifBlank { System.currentTimeMillis().toString() }),
                    "name" to newItem.name,
                    "description" to newItem.description,
                    "type" to newItem.type,
                    "effect" to newItem.effect
                )
                val updatedInv = rawInv + newEntry

                docRef.update(
                    "inventory", updatedInv,
                    "hpMax", snapshot.getLong("hpMax") ?: 20,
                    "hpCurrent", snapshot.getLong("hpCurrent") ?: 20
                ).await()

                loadInventory(gameId)
            } catch (e: Exception) {
                try {
                    db.collection("partidas").document(gameId).set(
                        mapOf(
                            "inventory" to listOf(
                                mapOf(
                                    "id" to newItem.id,
                                    "name" to newItem.name,
                                    "description" to newItem.description,
                                    "type" to newItem.type,
                                    "effect" to newItem.effect
                                )
                            ),
                            "hpMax" to 20,
                            "hpCurrent" to 20
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                    loadInventory(gameId)
                } catch (e2: Exception) {
                    Log.e("INVENTORY_ERROR", "addItem: ${e2.message}")
                }
            }
        }
    }

    // ── ELIMINA UN ITEM DEL INVENTARIO ───────────────────────────────────────
    private fun removeItemFromInventory(gameId: String, item: Item) {
        viewModelScope.launch {
            try {
                val char = _character.value ?: return@launch
                val updated = char.inventory.filter {
                    if (item.id.isNotBlank()) it.id != item.id
                    else it.name != item.name || it.type != item.type
                }
                val updatedMaps = updated.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "description" to it.description,
                        "type" to it.type,
                        "effect" to it.effect
                    )
                }
                db.collection("partidas").document(gameId)
                    .update("inventory", updatedMaps).await()
                _character.value = char.copy(inventory = updated)
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "removeItem: ${e.message}")
            }
        }
    }

    // ── MOTOR DE DADOS ────────────────────────────────────────────────────────
    private fun rollDiceExpression(expr: String): Int {
        return try {
            val clean = expr.uppercase().trim()
            val rx = Regex("""(\d*)D(\d+)(?:\+(\d+))?""")
            val m = rx.find(clean)
            if (m != null) {
                val cnt = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                val sides = m.groupValues[2].toIntOrNull() ?: 6
                val bonus = m.groupValues[3].toIntOrNull() ?: 0
                List(cnt) { (1..sides).random() }.sum() + bonus
            } else {
                clean.toIntOrNull() ?: 0
            }
        } catch (e: Exception) {
            0
        }
    }
}