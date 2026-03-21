package com.example.aidungeonmaster.viewmodel

import android.util.Log
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

    // ── CARGA INVENTARIO DESDE FIREBASE ──────────────────────────────────────
    fun loadInventory(gameId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Resetear SIEMPRE antes de cargar para que nunca persista
            // el estado de un personaje anterior (singleton compartido)
            _character.value = null
            try {
                val snapshot = db.collection("partidas").document(gameId).get().await()
                if (snapshot.exists()) {
                    val hpMax     = snapshot.getLong("hpMax")?.toInt()     ?: 20
                    val hpCurrent = snapshot.getLong("hpCurrent")?.toInt() ?: 20
                    val rawName   = snapshot.getString("characterName")    ?: ""
                    val rawClass  = snapshot.getString("characterClass")   ?: ""

                    val rawInv = snapshot.get("inventory") as? List<Map<String, Any>> ?: emptyList()
                    val inventory = rawInv.map { m ->
                        Item(
                            id          = m["id"]          as? String ?: System.currentTimeMillis().toString(),
                            name        = m["name"]        as? String ?: "Objeto sin nombre",
                            description = m["description"] as? String ?: "",
                            type        = m["type"]        as? String ?: "consumible",
                            effect      = m["effect"]      as? String ?: ""
                        )
                    }

                    _character.value = Character(
                        id             = gameId,
                        name           = rawName,
                        characterClass = rawClass,
                        inventory      = inventory,
                        hpMax          = hpMax,
                        hpCurrent      = hpCurrent
                    )
                } else {
                    // Personaje nuevo sin documento de partida aún → valores por defecto
                    _character.value = Character(
                        id        = gameId,
                        hpMax     = 20,
                        hpCurrent = 20,
                        inventory = emptyList()
                    )
                    Log.d("INVENTORY_DEBUG", "loadInventory: doc $gameId no existe, inicializando con defaults")
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
                Log.d("INVENTORY_DEBUG", "HP actualizado: $newHp")
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "updateHp: ${e.message}")
            }
        }
    }

    // ── USA UN OBJETO DEL INVENTARIO ─────────────────────────────────────────
    //
    //  Efectos soportados en el campo `effect`:
    //   cura:XdY+Z   → cura esa cantidad de HP
    //   cura:N       → cura N HP fijos
    //   daño:XdY     → se guarda para el próximo combate (solo info por ahora)
    //   +N CA        → bonus de armadura (info)
    //   veneno:XdY   → daño de veneno (info)
    //
    //  Devuelve un String con el resultado para mostrarlo en pantalla.
    fun useItem(gameId: String, item: Item, hpCurrent: Int, hpMax: Int): String {
        val effect = item.effect.lowercase().trim()
        return when {
            // ── Pociones y objetos de curación ─────────────────────────────
            effect.startsWith("cura:") -> {
                val expr    = effect.removePrefix("cura:").trim()
                val healed  = rollDiceExpression(expr)
                val newHp   = (hpCurrent + healed).coerceAtMost(hpMax)
                updateHp(gameId, newHp)
                removeItemFromInventory(gameId, item)
                "💚 ${item.name}: recuperas $healed HP ($newHp/$hpMax)"
            }

            // ── Pergaminos de daño ────────────────────────────────────────
            effect.startsWith("daño:") || effect.startsWith("dano:") -> {
                val expr   = effect.substringAfter(":").trim()
                val damage = rollDiceExpression(expr)
                removeItemFromInventory(gameId, item)
                "💥 ${item.name}: causa $damage de daño al usarlo"
            }

            // ── Veneno ────────────────────────────────────────────────────
            effect.startsWith("veneno:") -> {
                val expr   = effect.removePrefix("veneno:").trim()
                val dmg    = rollDiceExpression(expr)
                removeItemFromInventory(gameId, item)
                "☠️ ${item.name}: veneno activo — $dmg de daño por turno"
            }

            // ── Explosivos ────────────────────────────────────────────────
            effect.startsWith("explosivo:") -> {
                val expr   = effect.removePrefix("explosivo:").trim()
                val dmg    = rollDiceExpression(expr)
                removeItemFromInventory(gameId, item)
                "💣 ${item.name} explota: $dmg de daño en área"
            }

            // ── Objetos sin efecto activo ─────────────────────────────────
            item.type == "armadura" -> "🛡️ ${item.name} ya está equipado"
            item.type == "arma"     -> "⚔️ ${item.name} ya está en tu mano"
            else -> {
                removeItemFromInventory(gameId, item)
                "✨ Usaste ${item.name}"
            }
        }
    }

    // ── REINICIA PERSONAJE AL MORIR (vida a 20, inventario vacío) ────────────
    fun resetCharacter(charId: String) {
        viewModelScope.launch {
            try {
                // Obtener el hpMax original del personaje
                val snap = db.collection("partidas").document(charId).get().await()
                val originalHpMax = snap.getLong("hpMax")?.toInt() ?: 20

                db.collection("partidas").document(charId)
                    .update(mapOf(
                        "hpCurrent" to originalHpMax,
                        "hpMax"     to originalHpMax,
                        "inventory" to emptyList<Any>()
                    )).await()

                _character.value = _character.value?.copy(
                    hpCurrent = originalHpMax,
                    hpMax     = originalHpMax,
                    inventory = emptyList()
                )
                Log.d("INVENTORY_DEBUG", "Personaje reiniciado: HP=$originalHpMax, inventario vacío")
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "resetCharacter: ${e.message}")
            }
        }
    }

    // ── GUARDA ITEM DEL QR EN FIREBASE ───────────────────────────────────────
    fun addItemToInventory(gameId: String, newItem: Item) {
        viewModelScope.launch {
            try {
                val docRef   = db.collection("partidas").document(gameId)
                val snapshot = docRef.get().await()

                // Obtenemos el inventario actual en crudo para no machacarlo
                val rawInv = snapshot.get("inventory") as? List<Map<String, Any>> ?: emptyList()
                val newEntry = mapOf(
                    "id"          to (newItem.id.ifBlank { System.currentTimeMillis().toString() }),
                    "name"        to newItem.name,
                    "description" to newItem.description,
                    "type"        to newItem.type,
                    "effect"      to newItem.effect
                )
                val updatedInv = rawInv + newEntry

                docRef.update(
                    "inventory", updatedInv,
                    "hpMax",     snapshot.getLong("hpMax") ?: 20,
                    "hpCurrent", snapshot.getLong("hpCurrent") ?: 20
                ).await()

                // Refrescamos el estado local
                loadInventory(gameId)
            } catch (e: Exception) {
                // Si el doc no tiene los campos aún, usamos set con merge
                try {
                    db.collection("partidas").document(gameId).set(
                        mapOf(
                            "inventory" to listOf(mapOf(
                                "id" to newItem.id, "name" to newItem.name,
                                "description" to newItem.description,
                                "type" to newItem.type, "effect" to newItem.effect
                            )),
                            "hpMax" to 20, "hpCurrent" to 20
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
                    // Filtramos por id si es único, o por nombre+tipo como fallback
                    if (item.id.isNotBlank()) it.id != item.id
                    else it.name != item.name || it.type != item.type
                }
                val updatedMaps = updated.map { mapOf(
                    "id" to it.id, "name" to it.name,
                    "description" to it.description,
                    "type" to it.type, "effect" to it.effect
                )}
                db.collection("partidas").document(gameId)
                    .update("inventory", updatedMaps).await()
                _character.value = char.copy(inventory = updated)
            } catch (e: Exception) {
                Log.e("INVENTORY_ERROR", "removeItem: ${e.message}")
            }
        }
    }

    // ── MOTOR DE DADOS ────────────────────────────────────────────────────────
    //  Soporta: "2d6+3", "1d8", "D4", "10" (valor fijo)
    private fun rollDiceExpression(expr: String): Int {
        return try {
            val clean = expr.uppercase().trim()
            val rx    = Regex("""(\d*)D(\d+)(?:\+(\d+))?""")
            val m     = rx.find(clean)
            if (m != null) {
                val cnt   = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                val sides = m.groupValues[2].toIntOrNull() ?: 6
                val bonus = m.groupValues[3].toIntOrNull() ?: 0
                List(cnt) { (1..sides).random() }.sum() + bonus
            } else {
                clean.toIntOrNull() ?: 0
            }
        } catch (e: Exception) { 0 }
    }
}