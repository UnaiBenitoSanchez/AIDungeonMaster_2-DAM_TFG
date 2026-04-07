package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.EquippedItems
import com.example.aidungeonmaster.data.model.Item
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    private val _levelUpEvent = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    val levelUpEvent = _levelUpEvent.asSharedFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // CARGA INVENTARIO + EQUIPO + STATS
    // ─────────────────────────────────────────────────────────────────────────
    fun loadInventory(gameId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _character.value = null

            try {
                val snapshot = db.collection("partidas").document(gameId).get().await()

                if (!snapshot.exists()) {
                    _character.value = Character(
                        id = gameId,
                        hpMax = 20,
                        hpCurrent = 20,
                        inventory = emptyList(),
                        equipment = EquippedItems(),
                        xp = 0,
                        level = 1,
                        coins = 0
                    )
                    Log.d("INVENTORY_DEBUG", "loadInventory: documento $gameId no existe")
                    return@launch
                }

                val hpMax = snapshot.getLong("hpMax")?.toInt() ?: 20
                val hpCurrent = snapshot.getLong("hpCurrent")?.toInt() ?: 20
                val rawName = snapshot.getString("characterName")
                    ?: snapshot.getString("name")
                    ?: ""
                val rawClass = snapshot.getString("characterClass") ?: ""
                val xp = snapshot.getLong("xp")?.toInt() ?: 0
                val level = snapshot.getLong("level")?.toInt() ?: 1
                val coins = snapshot.getLong("coins")?.toInt() ?: 0
                val lastPlayed = snapshot.getLong("lastPlayed") ?: 0L

                val inventory = parseInventory(snapshot.get("inventory"))
                val equipment = parseEquippedItems(snapshot.get("equipment"))
                val stats = loadStatsForCharacter(gameId, snapshot)

                Log.d("INVENTORY_DEBUG", "loadInventory: ${inventory.size} items cargados")
                Log.d("INVENTORY_DEBUG", "loadInventory: equipment=$equipment")

                _character.value = Character(
                    id = gameId,
                    name = rawName,
                    characterClass = rawClass,
                    stats = stats,
                    inventory = inventory,
                    equipment = equipment,
                    hpMax = hpMax,
                    hpCurrent = hpCurrent,
                    xp = xp,
                    level = level,
                    coins = coins,
                    lastPlayed = lastPlayed
                )
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "loadInventory: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EQUIPAR / DESEQUIPAR
    // ─────────────────────────────────────────────────────────────────────────
    fun equipItem(gameId: String, item: Item, onResult: (String) -> Unit = {}) {
        if (!item.isEquippable) {
            onResult("❌ ${item.name} no se puede equipar")
            return
        }

        viewModelScope.launch {
            try {
                val char = _character.value ?: run {
                    onResult("❌ No hay personaje cargado")
                    return@launch
                }

                val slot = item.resolvedEquipSlot
                if (slot.isBlank()) {
                    onResult("❌ ${item.name} no tiene slot válido")
                    return@launch
                }

                var updatedInventory = char.inventory.toMutableList()
                var updatedEquipment = char.equipment

                updatedInventory = removeMatchingItem(updatedInventory, item)

                if (slot == "main_hand" && item.handedness.equals("two_hand", ignoreCase = true)) {
                    updatedEquipment.offHand?.let { updatedInventory.add(it) }
                    updatedEquipment = updatedEquipment.withItem("off_hand", null)
                }

                if (slot == "off_hand") {
                    val mainHand = updatedEquipment.mainHand
                    if (mainHand?.handedness.equals("two_hand", ignoreCase = true)) {
                        if (mainHand != null) updatedInventory.add(mainHand)
                        updatedEquipment = updatedEquipment.withItem("main_hand", null)
                    }
                }

                updatedEquipment.itemInSlot(slot)?.let { previous ->
                    updatedInventory.add(previous)
                }

                updatedEquipment = updatedEquipment.withItem(slot, item)

                persistInventoryAndEquipment(gameId, updatedInventory, updatedEquipment)

                _character.value = char.copy(
                    inventory = updatedInventory,
                    equipment = updatedEquipment
                )

                onResult(
                    when (slot) {
                        "main_hand" -> "⚔️ ${item.name} equipada en mano principal"
                        "off_hand" -> "🛡️ ${item.name} equipada en mano secundaria"
                        else -> "✅ ${item.name} equipada en ${friendlySlotName(slot)}"
                    }
                )
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "equipItem: ${e.message}", e)
                onResult("❌ Error al equipar ${item.name}")
            }
        }
    }

    fun unequipItem(gameId: String, slot: String, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val char = _character.value ?: run {
                    onResult("❌ No hay personaje cargado")
                    return@launch
                }

                val equipped = char.equipment.itemInSlot(slot)
                if (equipped == null) {
                    onResult("❌ No hay objeto equipado en ${friendlySlotName(slot)}")
                    return@launch
                }

                val updatedInventory = char.inventory + equipped
                val updatedEquipment = char.equipment.withItem(slot, null)

                persistInventoryAndEquipment(gameId, updatedInventory, updatedEquipment)

                _character.value = char.copy(
                    inventory = updatedInventory,
                    equipment = updatedEquipment
                )

                onResult("📦 ${equipped.name} volvió al inventario")
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "unequipItem: ${e.message}", e)
                onResult("❌ Error al desequipar")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HP
    // ─────────────────────────────────────────────────────────────────────────
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
                Log.e("INVENTORY_ERROR", "updateHp: ${e.message}", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XP / LEVEL UP
    // ─────────────────────────────────────────────────────────────────────────
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
                Log.e("INVENTORY_ERROR", "addXp: ${e.message}", e)
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
                        SetOptions.merge()
                    ).await()
            } catch (e: Exception) {
                Log.w("INVENTORY_ERROR", "addXp ranking: ${e.message}", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEDAS
    // ─────────────────────────────────────────────────────────────────────────
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
                        .set(mapOf("coins" to newTotal), SetOptions.merge()).await()
                    _character.value = _character.value?.copy(coins = newTotal)
                } catch (e2: Exception) {
                    Log.e("INVENTORY_ERROR", "addCoins: ${e2.message}", e2)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPUTACIÓN EN TIENDAS
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun loadShopReputation(gameId: String): Map<String, Int> {
        return try {
            val snap = db.collection("partidas").document(gameId).get().await()
            @Suppress("UNCHECKED_CAST")
            val raw = snap.get("shopReputation") as? Map<String, Long> ?: emptyMap()
            raw.mapValues { it.value.toInt() }
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "loadShopReputation: ${e.message}", e)
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
                    SetOptions.merge()
                ).await()
                points
            } catch (e2: Exception) {
                Log.e("INVENTORY_ERROR", "addShopReputation: ${e2.message}", e2)
                0
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BANCO
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getBankAccount(gameId: String): Map<String, Any>? {
        return try {
            val snap = db.collection("bank_accounts").document(gameId).get().await()
            if (snap.exists()) snap.data else null
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "getBankAccount: ${e.message}", e)
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
            Log.e("INVENTORY_ERROR", "registerBankPin: ${e.message}", e)
            false
        }
    }

    suspend fun verifyBankPin(gameId: String, pin: String): Boolean {
        return try {
            val snap = db.collection("bank_accounts").document(gameId).get().await()
            snap.exists() && snap.getString("pin") == pin
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "verifyBankPin: ${e.message}", e)
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

            gameRef.set(mapOf("coins" to newCoins), SetOptions.merge()).await()
            bankRef.update(
                mapOf(
                    "balance" to newBankBalance,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            _character.value = _character.value?.copy(coins = newCoins)
            true
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "depositToBank: ${e.message}", e)
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

            gameRef.set(mapOf("coins" to newCoins), SetOptions.merge()).await()
            bankRef.update(
                mapOf(
                    "balance" to newBankBalance,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            _character.value = _character.value?.copy(coins = newCoins)
            true
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "withdrawFromBank: ${e.message}", e)
            false
        }
    }

    suspend fun spendCoins(gameId: String, amount: Int): Boolean {
        return try {
            val snap = FirebaseFirestore.getInstance()
                .collection("partidas").document(gameId).get().await()

            val current = snap.getLong("coins")?.toInt() ?: 0
            if (current < amount) return false

            val newTotal = current - amount
            db.collection("partidas").document(gameId)
                .update("coins", newTotal).await()

            _character.value = _character.value?.copy(coins = newTotal)
            Log.d("INVENTORY_DEBUG", "Monedas gastadas: -$amount → total $newTotal")
            true
        } catch (e: Exception) {
            Log.e("INVENTORY_ERROR", "spendCoins: ${e.message}", e)
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USAR OBJETO
    // ─────────────────────────────────────────────────────────────────────────
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

            item.type.equals("armadura", ignoreCase = true) ->
                "🛡️ ${item.name} es equipable, no consumible"

            item.type.equals("arma", ignoreCase = true) ->
                "⚔️ ${item.name} es equipable, no consumible"

            else -> {
                removeItemFromInventory(gameId, item)
                "✨ Usaste ${item.name}"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESET PERSONAJE
    // ─────────────────────────────────────────────────────────────────────────
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
                            "equipment" to emptyMap<String, Any>(),
                            "coins" to 0
                        )
                    ).await()

                _character.value = _character.value?.copy(
                    hpCurrent = originalHpMax,
                    hpMax = originalHpMax,
                    inventory = emptyList(),
                    equipment = EquippedItems(),
                    coins = 0
                )

                Log.d(
                    "INVENTORY_DEBUG",
                    "Personaje reiniciado: HP=$originalHpMax, inventario y equipo vacíos"
                )
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "resetCharacter: ${e.message}", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AÑADIR ITEM
    // ─────────────────────────────────────────────────────────────────────────
    fun addItemToInventory(gameId: String, newItem: Item) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas").document(gameId)
                val snapshot = docRef.get().await()

                val currentInventory = parseInventory(snapshot.get("inventory"))
                val updatedInventory = currentInventory + newItem

                docRef.set(
                    mapOf(
                        "inventory" to updatedInventory.map(::itemToMap),
                        "hpMax" to (snapshot.getLong("hpMax") ?: 20),
                        "hpCurrent" to (snapshot.getLong("hpCurrent") ?: 20)
                    ),
                    SetOptions.merge()
                ).await()

                _character.value = _character.value?.copy(inventory = updatedInventory)
                    ?: _character.value

            } catch (e: Exception) {
                try {
                    db.collection("partidas").document(gameId).set(
                        mapOf(
                            "inventory" to listOf(itemToMap(newItem)),
                            "hpMax" to 20,
                            "hpCurrent" to 20
                        ),
                        SetOptions.merge()
                    ).await()
                    loadInventory(gameId)
                } catch (e2: Exception) {
                    Log.e("INVENTORY_ERROR", "addItem: ${e2.message}", e2)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ELIMINAR ITEM DEL INVENTARIO
    // ─────────────────────────────────────────────────────────────────────────
    private fun removeItemFromInventory(gameId: String, item: Item) {
        viewModelScope.launch {
            try {
                val char = _character.value ?: return@launch
                val updated = removeMatchingItem(char.inventory.toMutableList(), item)

                db.collection("partidas").document(gameId)
                    .update("inventory", updated.map(::itemToMap)).await()

                _character.value = char.copy(inventory = updated)
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "removeItem: ${e.message}", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SERIALIZACIÓN / PARSEO
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseInventory(raw: Any?): List<Item> {
        val rawList = raw as? List<*> ?: return emptyList()
        return rawList.mapNotNull { entry ->
            (entry as? Map<*, *>)?.let { parseItemMap(it) }
        }
    }

    private fun parseEquippedItems(raw: Any?): EquippedItems {
        val map = raw as? Map<*, *> ?: return EquippedItems()

        fun parseSlot(slot: String): Item? {
            val rawItem = map[slot] as? Map<*, *> ?: return null
            return parseItemMap(rawItem)
        }

        return EquippedItems(
            head = parseSlot("head"),
            chest = parseSlot("chest"),
            legs = parseSlot("legs"),
            feet = parseSlot("feet"),
            hands = parseSlot("hands"),
            mainHand = parseSlot("main_hand"),
            offHand = parseSlot("off_hand"),
            ring = parseSlot("ring"),
            amulet = parseSlot("amulet")
        )
    }

    private fun parseItemMap(m: Map<*, *>): Item {
        val statBonusesRaw = m["statBonuses"] as? Map<*, *>
        val statBonuses = statBonusesRaw
            ?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val value = (v as? Number)?.toInt() ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            ?: emptyMap()

        return Item(
            id = m["id"] as? String ?: System.currentTimeMillis().toString(),
            name = m["name"] as? String ?: "Objeto sin nombre",
            description = m["description"] as? String ?: "",
            type = m["type"] as? String ?: "consumible",
            effect = m["effect"] as? String ?: "",
            equipSlot = m["equipSlot"] as? String ?: "",
            weaponDamage = m["weaponDamage"] as? String ?: "",
            armorBase = (m["armorBase"] as? Number)?.toInt(),
            armorBonus = (m["armorBonus"] as? Number)?.toInt() ?: 0,
            maxDexBonus = (m["maxDexBonus"] as? Number)?.toInt(),
            handedness = m["handedness"] as? String ?: "one_hand",
            statBonuses = statBonuses
        )
    }

    private fun itemToMap(item: Item): Map<String, Any> {
        val out = mutableMapOf<String, Any>(
            "id" to item.id.ifBlank { System.currentTimeMillis().toString() },
            "name" to item.name,
            "description" to item.description,
            "type" to item.type,
            "effect" to item.effect
        )

        if (item.equipSlot.isNotBlank()) out["equipSlot"] = item.equipSlot
        if (item.weaponDamage.isNotBlank()) out["weaponDamage"] = item.weaponDamage
        item.armorBase?.let { out["armorBase"] = it }
        if (item.armorBonus != 0) out["armorBonus"] = item.armorBonus
        item.maxDexBonus?.let { out["maxDexBonus"] = it }
        if (!item.handedness.equals("one_hand", ignoreCase = true)) {
            out["handedness"] = item.handedness
        }
        if (item.statBonuses.isNotEmpty()) out["statBonuses"] = item.statBonuses

        return out
    }

    private fun equippedItemsToMap(equipment: EquippedItems): Map<String, Any> {
        val out = mutableMapOf<String, Any>()

        equipment.head?.let { out["head"] = itemToMap(it) }
        equipment.chest?.let { out["chest"] = itemToMap(it) }
        equipment.legs?.let { out["legs"] = itemToMap(it) }
        equipment.feet?.let { out["feet"] = itemToMap(it) }
        equipment.hands?.let { out["hands"] = itemToMap(it) }
        equipment.mainHand?.let { out["main_hand"] = itemToMap(it) }
        equipment.offHand?.let { out["off_hand"] = itemToMap(it) }
        equipment.ring?.let { out["ring"] = itemToMap(it) }
        equipment.amulet?.let { out["amulet"] = itemToMap(it) }

        return out
    }

    private suspend fun persistInventoryAndEquipment(
        gameId: String,
        inventory: List<Item>,
        equipment: EquippedItems
    ) {
        val current = _character.value
        val projected = current?.copy(
            inventory = inventory,
            equipment = equipment
        ) ?: Character(
            id = gameId,
            inventory = inventory,
            equipment = equipment
        )

        db.collection("partidas").document(gameId).set(
            mapOf(
                "inventory" to inventory.map(::itemToMap),
                "equipment" to equippedItemsToMap(equipment),
                "armorClass" to projected.armorClass
            ),
            SetOptions.merge()
        ).await()
    }

    private fun removeMatchingItem(
        items: MutableList<Item>,
        target: Item
    ): MutableList<Item> {
        val index = items.indexOfFirst {
            if (target.id.isNotBlank()) it.id == target.id
            else it.name == target.name && it.type == target.type
        }
        if (index >= 0) items.removeAt(index)
        return items
    }

    private suspend fun loadStatsForCharacter(
        gameId: String,
        snapshot: DocumentSnapshot
    ): Map<String, Int> {
        val fromGame = mapFromAny(snapshot.get("stats"))
        if (fromGame.isNotEmpty()) return normalizeStats(fromGame)

        return try {
            val rankingSnap = db.collection("ranking").document(gameId).get().await()

            val fromRankingMap = mapFromAny(rankingSnap.get("stats"))
            if (fromRankingMap.isNotEmpty()) {
                return normalizeStats(fromRankingMap)
            }

            normalizeStats(
                mapOf(
                    "Fuerza" to (rankingSnap.getLong("fuerza")?.toInt() ?: 10),
                    "Destreza" to (rankingSnap.getLong("destreza")?.toInt() ?: 10),
                    "Inteligencia" to (rankingSnap.getLong("inteligencia")?.toInt() ?: 10),
                    "Sabiduría" to (
                            rankingSnap.getLong("sabiduria")?.toInt()
                                ?: rankingSnap.getLong("sabiduría")?.toInt()
                                ?: 10
                            ),
                    "Constitución" to (
                            rankingSnap.getLong("constitucion")?.toInt()
                                ?: rankingSnap.getLong("constitución")?.toInt()
                                ?: 10
                            ),
                    "Carisma" to (rankingSnap.getLong("carisma")?.toInt() ?: 10)
                )
            )
        } catch (e: Exception) {
            Log.w("INVENTORY_DEBUG", "No se pudieron cargar stats para $gameId: ${e.message}")
            emptyMap()
        }
    }

    private fun mapFromAny(raw: Any?): Map<String, Int> {
        val map = raw as? Map<*, *> ?: return emptyMap()
        return map.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val value = (v as? Number)?.toInt() ?: return@mapNotNull null
            key to value
        }.toMap()
    }

    private fun normalizeStats(raw: Map<String, Int>): Map<String, Int> {
        return buildMap {
            put("Fuerza", raw["Fuerza"] ?: raw["fuerza"] ?: 10)
            put("Destreza", raw["Destreza"] ?: raw["destreza"] ?: 10)
            put("Inteligencia", raw["Inteligencia"] ?: raw["inteligencia"] ?: 10)
            put(
                "Sabiduría",
                raw["Sabiduría"] ?: raw["Sabiduria"] ?: raw["sabiduria"] ?: 10
            )
            put(
                "Constitución",
                raw["Constitución"] ?: raw["Constitucion"] ?: raw["constitucion"] ?: 10
            )
            put("Carisma", raw["Carisma"] ?: raw["carisma"] ?: 10)
        }
    }

    private fun friendlySlotName(slot: String): String = when (slot.lowercase()) {
        "head" -> "cabeza"
        "chest" -> "pecho"
        "legs" -> "piernas"
        "feet" -> "pies"
        "hands" -> "manos"
        "main_hand" -> "mano principal"
        "off_hand" -> "mano secundaria"
        "ring" -> "anillo"
        "amulet" -> "amuleto"
        else -> slot
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOTOR DE DADOS
    // ─────────────────────────────────────────────────────────────────────────
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
        } catch (_: Exception) {
            0
        }
    }
}