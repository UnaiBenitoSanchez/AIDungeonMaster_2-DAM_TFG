package com.example.aidungeonmaster.data.repository

import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.EquippedItems
import com.example.aidungeonmaster.data.model.GuildBossAbility
import com.example.aidungeonmaster.data.model.GuildBossAbilityType
import com.example.aidungeonmaster.data.model.GuildBossParticipant
import com.example.aidungeonmaster.data.model.GuildBossRoom
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.data.model.ItemEnchantment
import com.example.aidungeonmaster.data.model.isBossBattleConsumable
import com.example.aidungeonmaster.data.model.normalizeEquipSlot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.random.Random

class GuildRaidRepository {

    companion object {
        const val FINAL_BOSS_ROOM_ID = "final_boss"
        const val BOSS_TURN_UID = "__BOSS__"
        private const val DEFAULT_BOSS_NAME = "Señor del Abismo"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun currentUid(): String? = auth.currentUser?.uid

    private fun guildRef(guildId: String) =
        db.collection("guilds").document(guildId)

    private fun roomRef(guildId: String) =
        guildRef(guildId).collection("boss_rooms").document(FINAL_BOSS_ROOM_ID)

    private fun participantsRef(guildId: String) =
        roomRef(guildId).collection("participants")

    private fun partidaRef(partidaId: String) =
        db.collection("partidas").document(partidaId)

    private fun partidaId(uid: String, characterName: String): String =
        "${uid}_${characterName}"

    private suspend fun isCurrentUserGuildOwner(guildId: String): Boolean {
        val uid = currentUid() ?: return false
        val guildOwnerUid = guildRef(guildId).get().await().getString("ownerUid")
        return guildOwnerUid == uid
    }

    fun listenBossRoom(
        guildId: String,
        onChange: (GuildBossRoom?) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return roomRef(guildId).addSnapshotListener { snap, error ->
            if (error != null) {
                onError(error.message ?: "No se pudo escuchar la sala del jefe")
                return@addSnapshotListener
            }

            if (snap == null || !snap.exists()) {
                onChange(null)
                return@addSnapshotListener
            }

            onChange(
                snap.toObject(GuildBossRoom::class.java)?.copy(guildId = guildId)
            )
        }
    }

    fun listenBossParticipants(
        guildId: String,
        onChange: (List<GuildBossParticipant>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return participantsRef(guildId)
            .orderBy("joinedAt")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    onError(error.message ?: "No se pudo escuchar a los participantes")
                    return@addSnapshotListener
                }

                val participants = snap?.documents.orEmpty()
                    .mapNotNull { it.toObject(GuildBossParticipant::class.java) }

                onChange(participants)
            }
    }

    suspend fun getPlayableCharacters(): List<Character> {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")

        val docs = db.collection("users")
            .document(uid)
            .collection("characters")
            .get()
            .await()

        return docs.documents.mapNotNull { doc ->
            doc.toObject(Character::class.java)?.copy(id = doc.id)
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun ensureBossRoom(guildId: String): GuildBossRoom {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val now = System.currentTimeMillis()
        val roomSnapshot = roomRef(guildId).get().await()

        if (roomSnapshot.exists()) {
            return roomSnapshot.toObject(GuildBossRoom::class.java)?.copy(guildId = guildId)
                ?: GuildBossRoom(guildId = guildId, createdBy = uid, updatedAt = now)
        }

        val room = GuildBossRoom(
            guildId = guildId,
            status = "waiting",
            bossName = DEFAULT_BOSS_NAME,
            bossHpMax = 0,
            bossHpCurrent = 0,
            bossAttackMin = 0,
            bossAttackMax = 0,
            currentTurnUid = "",
            turnOrder = emptyList(),
            turnIndex = 0,
            round = 1,
            winner = "",
            battleLog = listOf("🕯️ La sala de espera del jefe final ha sido creada."),
            createdBy = uid,
            updatedAt = now
        )

        roomRef(guildId).set(room).await()
        return room
    }

    suspend fun selectCharacter(guildId: String, character: Character) {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val profile = db.collection("users").document(uid).get().await()
        val displayName = profile.getString("displayName").orEmpty()
        val username = profile.getString("username").orEmpty()
        val photoUrl = profile.getString("photoUrl").orEmpty()
        val now = System.currentTimeMillis()

        ensureBossRoom(guildId)

        val existingSnap = participantsRef(guildId).document(uid).get().await()
        val existingJoinedAt = existingSnap.getLong("joinedAt") ?: now

        val liveCharacter = loadLiveCharacterState(uid, character)
        val basicDamage = computeBasicAttackRange(liveCharacter)

        val participant = GuildBossParticipant(
            uid = uid,
            displayName = displayName,
            username = username,
            photoUrl = photoUrl,
            selectedCharacterDocId = character.id,
            selectedCharacterName = liveCharacter.name,
            selectedCharacterClass = liveCharacter.characterClass,
            hpMax = liveCharacter.hpMax,
            hpCurrent = liveCharacter.hpMax,
            attackMin = basicDamage.first,
            attackMax = basicDamage.second,
            attackBonus = max(liveCharacter.meleeAttackBonus, liveCharacter.rangedAttackBonus),
            armorClass = liveCharacter.armorClass,
            ready = false,
            alive = true,
            cooldowns = emptyMap(),
            defenseBonus = 0,
            advantageCharges = 0,
            joinedAt = existingJoinedAt,
            updatedAt = now
        )

        participantsRef(guildId).document(uid).set(participant).await()
    }

    suspend fun setReady(guildId: String, ready: Boolean) {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val now = System.currentTimeMillis()
        val room = ensureBossRoom(guildId)

        val myParticipantRef = participantsRef(guildId).document(uid)
        val mySnap = myParticipantRef.get().await()
        val currentParticipant = mySnap.toObject(GuildBossParticipant::class.java)
            ?: throw IllegalStateException("Primero debes elegir un personaje.")

        if (!ready) {
            myParticipantRef.update(
                mapOf(
                    "ready" to false,
                    "updatedAt" to now
                )
            ).await()
            return
        }

        // BUG FIX: Antes la condición era solo room.status == "battle", lo que hacía que si una
        // pelea terminaba y el participante tenía HP baja o 0, al presionar Listo en la siguiente
        // pelea (que aún está en status "battle" por un frame) no se recargaba el HP del personaje.
        // Ahora solo saltamos la recarga si estamos realmente en combate activo con HP completa.
        val refreshedParticipant = if (room.status == "battle" &&
            currentParticipant.alive &&
            currentParticipant.hpCurrent > 0 &&
            currentParticipant.hpCurrent == currentParticipant.hpMax) {
            currentParticipant.copy(
                ready = true,
                updatedAt = now
            )
        } else {
            val liveCharacter = if (currentParticipant.selectedCharacterDocId.isNotBlank()) {
                val charSnap = db.collection("users")
                    .document(uid)
                    .collection("characters")
                    .document(currentParticipant.selectedCharacterDocId)
                    .get()
                    .await()

                val baseCharacter = charSnap.toObject(Character::class.java)?.copy(id = charSnap.id)
                if (baseCharacter != null) loadLiveCharacterState(uid, baseCharacter) else null
            } else {
                null
            }

            if (liveCharacter != null) {
                val basicDamage = computeBasicAttackRange(liveCharacter)

                currentParticipant.copy(
                    selectedCharacterName = liveCharacter.name,
                    selectedCharacterClass = liveCharacter.characterClass,
                    hpMax = liveCharacter.hpMax,
                    hpCurrent = liveCharacter.hpMax,
                    attackMin = basicDamage.first,
                    attackMax = basicDamage.second,
                    attackBonus = max(liveCharacter.meleeAttackBonus, liveCharacter.rangedAttackBonus),
                    armorClass = liveCharacter.armorClass,
                    ready = true,
                    alive = true,
                    cooldowns = emptyMap(),
                    defenseBonus = 0,
                    advantageCharges = 0,
                    updatedAt = now
                )
            } else {
                currentParticipant.copy(
                    hpCurrent = currentParticipant.hpMax,
                    ready = true,
                    alive = true,
                    cooldowns = emptyMap(),
                    defenseBonus = 0,
                    advantageCharges = 0,
                    updatedAt = now
                )
            }
        }

        myParticipantRef.set(refreshedParticipant).await()
    }

    suspend fun startBattleIfReady(guildId: String) {
        currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        if (!isCurrentUserGuildOwner(guildId)) {
            throw IllegalStateException("Solo el líder del gremio puede iniciar la pelea.")
        }

        val room = roomRef(guildId).get().await()
            .toObject(GuildBossRoom::class.java)
            ?.copy(guildId = guildId)
            ?: throw IllegalStateException("La sala del jefe no existe.")

        if (room.status == "battle") return
        if (room.status != "waiting" && room.status != "finished") {
            throw IllegalStateException("La sala no está lista para iniciar la batalla.")
        }

        val participants = participantsRef(guildId).get().await().documents
            .mapNotNull { it.toObject(GuildBossParticipant::class.java) }
            .filter { it.selectedCharacterName.isNotBlank() }

        val allReady = participants.isNotEmpty() && participants.all {
            it.ready &&
                    it.alive &&
                    it.hpCurrent > 0 &&
                    it.hpCurrent == it.hpMax
        }

        if (!allReady) {
            throw IllegalStateException(
                "Todos los participantes deben volver a pulsar Listo para restaurar su estado antes de empezar otra pelea."
            )
        }

        val now = System.currentTimeMillis()
        val orderedParticipants = participants.sortedBy { it.joinedAt }
        val turnOrder = listOf(BOSS_TURN_UID) + orderedParticipants.map { it.uid }

        // Calcular estadísticas del jefe en función del grupo
        val (bossHp, bossAtkMin, bossAtkMax) = computeBossStats(participants)

        roomRef(guildId).set(
            GuildBossRoom(
                guildId = guildId,
                status = "battle",
                bossName = DEFAULT_BOSS_NAME,
                bossHpMax = bossHp,
                bossHpCurrent = bossHp,
                bossAttackMin = bossAtkMin,
                bossAttackMax = bossAtkMax,
                currentTurnUid = BOSS_TURN_UID,
                turnOrder = turnOrder,
                turnIndex = 0,
                round = 1,
                winner = "",
                battleLog = listOf(
                    "☠️ El $DEFAULT_BOSS_NAME ha despertado.",
                    "⚔️ HP del jefe: $bossHp | Ataque: $bossAtkMin-$bossAtkMax",
                    "🎯 Todos están listos. Comienza la batalla."
                ),
                createdBy = room.createdBy,
                updatedAt = now
            )
        ).await()
    }

    suspend fun loadBossConsumables(guildId: String): List<Item> {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val participant = participantsRef(guildId).document(uid).get().await()
            .toObject(GuildBossParticipant::class.java)
            ?: return emptyList()

        if (participant.selectedCharacterName.isBlank()) return emptyList()

        val snap = partidaRef(partidaId(uid, participant.selectedCharacterName)).get().await()
        if (!snap.exists()) return emptyList()

        return parseInventory(snap.get("inventory"))
            .filter(::isBossBattleConsumable)
            .sortedBy { it.name.lowercase() }
    }

    suspend fun playerAttack(guildId: String) {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val room = requireMyTurnRoom(guildId, uid)
        val me = requireParticipant(guildId, uid)

        if (!me.alive || me.hpCurrent <= 0) {
            throw IllegalStateException("Tu personaje no puede actuar.")
        }

        val now = System.currentTimeMillis()
        val normalizedMe = normalizeStartOfTurn(me, now)

        val bossAc = bossArmorClass(room)
        val usesAdvantage = normalizedMe.advantageCharges > 0
        val attackRoll = rollAttackAgainst(
            attackBonus = normalizedMe.attackBonus,
            targetArmorClass = bossAc,
            advantage = usesAdvantage
        )

        val updatedParticipant = normalizedMe.copy(
            advantageCharges = if (usesAdvantage) 0 else normalizedMe.advantageCharges,
            updatedAt = now
        )

        val participants = loadParticipants(guildId)
        val participantsAfter = participants.map {
            if (it.uid == uid) updatedParticipant else it
        }

        val logLine = if (!attackRoll.hit) {
            "❌ ${me.displayName.ifBlank { me.username }} falla el ataque al ${room.bossName} (tirada ${attackRoll.rollText}, total ${attackRoll.total}, CA $bossAc)."
        } else {
            val damage = rollIntBetween(normalizedMe.attackMin, normalizedMe.attackMax) *
                    if (attackRoll.crit) 2 else 1
            val newBossHp = (room.bossHpCurrent - damage).coerceAtLeast(0)

            val roomAfter = buildNextRoomAfterPlayerAction(
                room = room,
                participants = participantsAfter,
                newBossHp = newBossHp,
                now = now,
                newLogLine = "⚔️ ${me.displayName.ifBlank { me.username }} golpea al ${room.bossName} por $damage${if (attackRoll.crit) " (¡CRÍTICO!)" else ""}."
            )

            val batch = db.batch()
            batch.set(participantsRef(guildId).document(uid), updatedParticipant)
            batch.set(roomRef(guildId), roomAfter)
            batch.commit().await()
            return
        }

        val roomAfter = buildNextRoomAfterPlayerAction(
            room = room,
            participants = participantsAfter,
            newBossHp = room.bossHpCurrent,
            now = now,
            newLogLine = logLine
        )

        val batch = db.batch()
        batch.set(participantsRef(guildId).document(uid), updatedParticipant)
        batch.set(roomRef(guildId), roomAfter)
        batch.commit().await()
    }

    suspend fun useAbility(guildId: String, ability: GuildBossAbility) {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val room = requireMyTurnRoom(guildId, uid)
        val me = requireParticipant(guildId, uid)

        if (!me.alive || me.hpCurrent <= 0) {
            throw IllegalStateException("Tu personaje no puede actuar.")
        }

        if (ability.type == GuildBossAbilityType.SPECIAL_FLEE) {
            throw IllegalStateException("No puedes huir del jefe final.")
        }

        val now = System.currentTimeMillis()
        val normalizedMe = normalizeStartOfTurn(me, now)
        val currentCooldown = normalizedMe.cooldowns[ability.id] ?: 0
        if (currentCooldown > 0) {
            throw IllegalStateException("La habilidad aún está en recarga.")
        }

        val cooldowns = normalizedMe.cooldowns.toMutableMap()
        if (ability.cooldownTurns > 0) {
            cooldowns[ability.id] = ability.cooldownTurns
        }

        val baseUpdated = normalizedMe.copy(
            cooldowns = cooldowns,
            updatedAt = now
        )

        val participants = loadParticipants(guildId)
        val bossAc = bossArmorClass(room)

        when (ability.type) {
            GuildBossAbilityType.DAMAGE -> {
                val alwaysHits = ability.id == "magic_missile"
                val usesAdvantage = !alwaysHits && normalizedMe.advantageCharges > 0
                val attackRoll = if (alwaysHits) {
                    AttackRollResult(
                        rolls = listOf(20),
                        keptRoll = 20,
                        total = 20 + normalizedMe.attackBonus,
                        hit = true,
                        crit = false,
                        rollText = "auto"
                    )
                } else {
                    rollAttackAgainst(
                        attackBonus = normalizedMe.attackBonus,
                        targetArmorClass = bossAc,
                        advantage = usesAdvantage
                    )
                }

                val updatedParticipant = baseUpdated.copy(
                    advantageCharges = if (usesAdvantage) 0 else baseUpdated.advantageCharges,
                    updatedAt = now
                )

                val participantsAfter = participants.map {
                    if (it.uid == uid) updatedParticipant else it
                }

                val logLine: String
                val newBossHp: Int

                if (!attackRoll.hit) {
                    newBossHp = room.bossHpCurrent
                    logLine =
                        "${ability.emoji} ${me.displayName.ifBlank { me.username }} usa ${ability.name}, pero falla (tirada ${attackRoll.rollText}, total ${attackRoll.total}, CA $bossAc)."
                } else {
                    val damage = rollDiceExpression(
                        expr = ability.diceExpression,
                        crit = attackRoll.crit
                    )
                    newBossHp = (room.bossHpCurrent - damage).coerceAtLeast(0)
                    logLine =
                        "${ability.emoji} ${me.displayName.ifBlank { me.username }} usa ${ability.name} y causa $damage al ${room.bossName}${if (attackRoll.crit) " (¡CRÍTICO!)" else ""}."
                }

                val roomAfter = buildNextRoomAfterPlayerAction(
                    room = room,
                    participants = participantsAfter,
                    newBossHp = newBossHp,
                    now = now,
                    newLogLine = logLine
                )

                val batch = db.batch()
                batch.set(participantsRef(guildId).document(uid), updatedParticipant)
                batch.set(roomRef(guildId), roomAfter)
                batch.commit().await()
            }

            GuildBossAbilityType.HEAL -> {
                val heal = rollDiceExpression(ability.diceExpression).coerceAtLeast(1)
                val newHp = (normalizedMe.hpCurrent + heal).coerceAtMost(normalizedMe.hpMax)

                val updatedParticipant = baseUpdated.copy(
                    hpCurrent = newHp,
                    alive = newHp > 0,
                    updatedAt = now
                )

                val participantsAfter = participants.map {
                    if (it.uid == uid) updatedParticipant else it
                }

                val roomAfter = buildNextRoomAfterPlayerAction(
                    room = room,
                    participants = participantsAfter,
                    newBossHp = room.bossHpCurrent,
                    now = now,
                    newLogLine = "${ability.emoji} ${me.displayName.ifBlank { me.username }} usa ${ability.name} y recupera $heal HP."
                )

                val batch = db.batch()
                batch.set(participantsRef(guildId).document(uid), updatedParticipant)
                batch.set(roomRef(guildId), roomAfter)
                batch.commit().await()
            }

            GuildBossAbilityType.BUFF_DEFENSE -> {
                val bonus = ability.diceExpression.toIntOrNull() ?: 2

                val updatedParticipant = baseUpdated.copy(
                    defenseBonus = bonus,
                    updatedAt = now
                )

                val participantsAfter = participants.map {
                    if (it.uid == uid) updatedParticipant else it
                }

                val roomAfter = buildNextRoomAfterPlayerAction(
                    room = room,
                    participants = participantsAfter,
                    newBossHp = room.bossHpCurrent,
                    now = now,
                    newLogLine = "${ability.emoji} ${me.displayName.ifBlank { me.username }} usa ${ability.name} y gana +$bonus CA hasta su próximo turno."
                )

                val batch = db.batch()
                batch.set(participantsRef(guildId).document(uid), updatedParticipant)
                batch.set(roomRef(guildId), roomAfter)
                batch.commit().await()
            }

            GuildBossAbilityType.BUFF_ATTACK -> {
                val updatedParticipant = baseUpdated.copy(
                    advantageCharges = max(baseUpdated.advantageCharges, 1),
                    updatedAt = now
                )

                val participantsAfter = participants.map {
                    if (it.uid == uid) updatedParticipant else it
                }

                val roomAfter = buildNextRoomAfterPlayerAction(
                    room = room,
                    participants = participantsAfter,
                    newBossHp = room.bossHpCurrent,
                    now = now,
                    newLogLine = "${ability.emoji} ${me.displayName.ifBlank { me.username }} usa ${ability.name} y gana ventaja en su siguiente ataque."
                )

                val batch = db.batch()
                batch.set(participantsRef(guildId).document(uid), updatedParticipant)
                batch.set(roomRef(guildId), roomAfter)
                batch.commit().await()
            }

            GuildBossAbilityType.SPECIAL_FLEE -> {
                throw IllegalStateException("No puedes huir del jefe final.")
            }
        }
    }

    suspend fun useConsumable(guildId: String, item: Item) {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val room = requireMyTurnRoom(guildId, uid)
        val me = requireParticipant(guildId, uid)

        if (!me.alive || me.hpCurrent <= 0) {
            throw IllegalStateException("Tu personaje no puede actuar.")
        }

        val now = System.currentTimeMillis()
        val normalizedMe = normalizeStartOfTurn(me, now)
        val partidaDocId = partidaId(uid, normalizedMe.selectedCharacterName)
        val partidaSnap = partidaRef(partidaDocId).get().await()
        if (!partidaSnap.exists()) {
            throw IllegalStateException("No se encontró la aventura del personaje.")
        }

        val inventory = parseInventory(partidaSnap.get("inventory"))
        val currentItem = inventory.firstOrNull {
            if (item.id.isNotBlank()) it.id == item.id else it.name == item.name
        } ?: throw IllegalStateException("El objeto ya no está en el inventario.")

        if (!isBossBattleConsumable(currentItem)) {
            throw IllegalStateException("Ese objeto no se puede usar en la pelea del jefe.")
        }

        val updatedInventory = removeMatchingItem(inventory.toMutableList(), currentItem)
        val participants = loadParticipants(guildId)

        val effect = currentItem.effect.lowercase().trim()
        val looksHealingItem =
            currentItem.type.contains("pocion", ignoreCase = true) ||
                    currentItem.type.contains("consum", ignoreCase = true) ||
                    currentItem.name.contains("poción", ignoreCase = true) ||
                    currentItem.name.contains("pocion", ignoreCase = true) ||
                    currentItem.name.contains("elixir", ignoreCase = true) ||
                    currentItem.description.contains("cura", ignoreCase = true) ||
                    currentItem.description.contains("restaura", ignoreCase = true) ||
                    currentItem.description.contains("regenera", ignoreCase = true)

        val batch = db.batch()

        when {
            effect.startsWith("cura:") || looksHealingItem -> {
                val healExpr = if (effect.startsWith("cura:")) {
                    effect.removePrefix("cura:").trim()
                } else {
                    extractHealingExpression(currentItem)
                }

                val heal = rollDiceExpression(healExpr).coerceAtLeast(1)
                val newHp = (normalizedMe.hpCurrent + heal).coerceAtMost(normalizedMe.hpMax)

                val updatedParticipant = normalizedMe.copy(
                    hpCurrent = newHp,
                    alive = newHp > 0,
                    updatedAt = now
                )

                val participantsAfter = participants.map {
                    if (it.uid == uid) updatedParticipant else it
                }

                val roomAfter = buildNextRoomAfterPlayerAction(
                    room = room,
                    participants = participantsAfter,
                    newBossHp = room.bossHpCurrent,
                    now = now,
                    newLogLine = "🧪 ${me.displayName.ifBlank { me.username }} usa ${currentItem.name} y recupera $heal HP."
                )

                batch.set(participantsRef(guildId).document(uid), updatedParticipant)
                batch.set(roomRef(guildId), roomAfter)
                batch.set(
                    partidaRef(partidaDocId),
                    mapOf("inventory" to updatedInventory.map(::itemToMap)),
                    SetOptions.merge()
                )
                batch.commit().await()
            }

            effect.startsWith("daño:") || effect.startsWith("dano:") ||
                    effect.startsWith("veneno:") || effect.startsWith("explosivo:") -> {
                val expr = when {
                    effect.startsWith("daño:") -> effect.substringAfter(":").trim()
                    effect.startsWith("dano:") -> effect.substringAfter(":").trim()
                    effect.startsWith("veneno:") -> effect.substringAfter(":").trim()
                    else -> effect.substringAfter(":").trim()
                }

                val damage = rollDiceExpression(expr).coerceAtLeast(1)
                val newBossHp = (room.bossHpCurrent - damage).coerceAtLeast(0)

                val participantsAfter = participants.map {
                    if (it.uid == uid) normalizedMe else it
                }

                val roomAfter = buildNextRoomAfterPlayerAction(
                    room = room,
                    participants = participantsAfter,
                    newBossHp = newBossHp,
                    now = now,
                    newLogLine = "🧪 ${me.displayName.ifBlank { me.username }} usa ${currentItem.name} y causa $damage al ${room.bossName}."
                )

                batch.set(participantsRef(guildId).document(uid), normalizedMe)
                batch.set(roomRef(guildId), roomAfter)
                batch.set(
                    partidaRef(partidaDocId),
                    mapOf("inventory" to updatedInventory.map(::itemToMap)),
                    SetOptions.merge()
                )
                batch.commit().await()
            }

            else -> {
                throw IllegalStateException("Ese objeto no tiene un efecto válido para esta pelea.")
            }
        }
    }

    suspend fun resolveBossTurn(guildId: String) {
        currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        if (!isCurrentUserGuildOwner(guildId)) return

        val roomReference = roomRef(guildId)
        val room = roomReference.get().await().toObject(GuildBossRoom::class.java)
            ?.copy(guildId = guildId)
            ?: return

        if (room.status != "battle" || room.currentTurnUid != BOSS_TURN_UID) return

        val participantsBeforeHit = loadParticipants(guildId)
        val living = participantsBeforeHit.filter { it.alive && it.hpCurrent > 0 }

        if (living.isEmpty()) {
            roomReference.set(
                room.copy(
                    status = "finished",
                    winner = "boss",
                    currentTurnUid = "",
                    battleLog = trimLog(room.battleLog + "☠️ El ${room.bossName} ha arrasado con todo el gremio."),
                    updatedAt = System.currentTimeMillis()
                )
            ).await()
            return
        }

        val target = living.sortedWith(
            compareBy<GuildBossParticipant> { it.hpCurrent }.thenBy { it.joinedAt }
        ).first()

        val now = System.currentTimeMillis()
        val bossAttack = rollAttackAgainst(
            attackBonus = bossAttackBonus(room),
            targetArmorClass = target.armorClass + target.defenseBonus,
            advantage = false
        )

        val targetUpdated: GuildBossParticipant
        val roomAfter: GuildBossRoom

        if (!bossAttack.hit) {
            targetUpdated = target.copy(updatedAt = now)

            roomAfter = buildNextRoomAfterBossAction(
                room = room,
                participants = participantsBeforeHit,
                now = now,
                newLogLine = "🛡️ ${target.displayName.ifBlank { target.username }} bloquea el golpe de ${room.bossName} (tirada ${bossAttack.rollText}, total ${bossAttack.total}, CA ${target.armorClass + target.defenseBonus})."
            )

            val batch = db.batch()
            batch.update(participantsRef(guildId).document(target.uid), mapOf("updatedAt" to now))
            batch.set(roomReference, roomAfter)
            batch.commit().await()
            return
        }

        val damage = rollIntBetween(room.bossAttackMin, room.bossAttackMax) *
                if (bossAttack.crit) 2 else 1

        val newHp = (target.hpCurrent - damage).coerceAtLeast(0)
        val stillAlive = newHp > 0

        targetUpdated = target.copy(
            hpCurrent = newHp,
            alive = stillAlive,
            updatedAt = now
        )

        val participantsAfterHit = participantsBeforeHit.map {
            if (it.uid == target.uid) targetUpdated else it
        }

        val livingAfter = participantsAfterHit.filter { it.alive && it.hpCurrent > 0 }
        if (livingAfter.isEmpty()) {
            roomAfter = room.copy(
                status = "finished",
                winner = "boss",
                currentTurnUid = "",
                battleLog = trimLog(
                    room.battleLog + if (stillAlive) {
                        "🩸 ${room.bossName} golpea a ${target.displayName.ifBlank { target.username }} por $damage."
                    } else {
                        "💀 ${room.bossName} derrota a ${target.displayName.ifBlank { target.username }} por $damage."
                    } + " ☠️ No queda nadie en pie."
                ),
                updatedAt = now
            )
        } else {
            roomAfter = buildNextRoomAfterBossAction(
                room = room,
                participants = participantsAfterHit,
                now = now,
                newLogLine = if (stillAlive) {
                    "🩸 ${room.bossName} golpea a ${target.displayName.ifBlank { target.username }} por $damage${if (bossAttack.crit) " (¡CRÍTICO!)" else ""}."
                } else {
                    "💀 ${room.bossName} derrota a ${target.displayName.ifBlank { target.username }} por $damage${if (bossAttack.crit) " (¡CRÍTICO!)" else ""}."
                }
            )
        }

        val batch = db.batch()
        batch.update(
            participantsRef(guildId).document(target.uid),
            mapOf(
                "hpCurrent" to newHp,
                "alive" to stillAlive,
                "updatedAt" to now
            )
        )
        batch.set(roomReference, roomAfter)
        batch.commit().await()
    }

    /**
     * El líder puede forzar el fin de la batalla en cualquier momento.
     * Marca la sala como "finished" con ganador "boss" y añade una entrada al log.
     */
    suspend fun forceEndBattle(guildId: String) {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        if (!isCurrentUserGuildOwner(guildId)) {
            throw IllegalStateException("Solo el líder del gremio puede terminar la batalla.")
        }

        val room = roomRef(guildId).get().await().toObject(GuildBossRoom::class.java)
            ?.copy(guildId = guildId)
            ?: throw IllegalStateException("La sala del jefe no existe.")

        if (room.status != "battle") {
            throw IllegalStateException("No hay una batalla activa.")
        }

        val now = System.currentTimeMillis()
        roomRef(guildId).set(
            room.copy(
                status = "finished",
                winner = "boss",
                currentTurnUid = "",
                battleLog = trimLog(room.battleLog + "🚩 El líder ha terminado la batalla manualmente."),
                updatedAt = now
            )
        ).await()
    }

    suspend fun leaveBossRoom(guildId: String) {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val room = roomRef(guildId).get().await().toObject(GuildBossRoom::class.java)
        if (room?.status == "battle") {
            throw IllegalStateException("No puedes salir durante la batalla.")
        }
        participantsRef(guildId).document(uid).delete().await()
    }

    suspend fun resetBossRoom(guildId: String) {
        val uid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val roomSnap = roomRef(guildId).get().await()
        val existingCreatedBy = roomSnap.getString("createdBy").orEmpty().ifBlank { uid }
        val participants = participantsRef(guildId).get().await().documents
        val now = System.currentTimeMillis()

        db.runBatch { batch ->
            participants.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.set(
                roomRef(guildId),
                GuildBossRoom(
                    guildId = guildId,
                    status = "waiting",
                    bossName = DEFAULT_BOSS_NAME,
                    bossHpMax = 0,
                    bossHpCurrent = 0,
                    bossAttackMin = 0,
                    bossAttackMax = 0,
                    currentTurnUid = "",
                    turnOrder = emptyList(),
                    turnIndex = 0,
                    round = 1,
                    winner = "",
                    battleLog = listOf("🔁 La sala del jefe final ha sido reiniciada."),
                    createdBy = existingCreatedBy,
                    updatedAt = now
                )
            )
        }.await()
    }

    private suspend fun requireMyTurnRoom(guildId: String, uid: String): GuildBossRoom {
        val room = roomRef(guildId).get().await().toObject(GuildBossRoom::class.java)
            ?.copy(guildId = guildId)
            ?: throw IllegalStateException("La sala no existe.")

        if (room.status != "battle") throw IllegalStateException("La batalla no está activa.")
        if (room.currentTurnUid != uid) throw IllegalStateException("No es tu turno.")
        return room
    }

    private suspend fun requireParticipant(guildId: String, uid: String): GuildBossParticipant {
        return participantsRef(guildId).document(uid).get().await()
            .toObject(GuildBossParticipant::class.java)
            ?: throw IllegalStateException("No participas en esta batalla.")
    }

    private suspend fun loadParticipants(guildId: String): List<GuildBossParticipant> {
        return participantsRef(guildId).get().await().documents
            .mapNotNull { it.toObject(GuildBossParticipant::class.java) }
    }

    private fun normalizeStartOfTurn(
        participant: GuildBossParticipant,
        now: Long
    ): GuildBossParticipant {
        val reducedCooldowns = participant.cooldowns
            .mapValues { (_, value) -> value - 1 }
            .filterValues { it > 0 }

        return participant.copy(
            cooldowns = reducedCooldowns,
            defenseBonus = 0,
            updatedAt = now
        )
    }

    private fun buildNextRoomAfterPlayerAction(
        room: GuildBossRoom,
        participants: List<GuildBossParticipant>,
        newBossHp: Int,
        now: Long,
        newLogLine: String
    ): GuildBossRoom {
        val updatedLog = trimLog(room.battleLog + newLogLine)

        if (newBossHp <= 0) {
            return room.copy(
                bossHpCurrent = 0,
                status = "finished",
                winner = "guild",
                currentTurnUid = "",
                battleLog = trimLog(updatedLog + "🏆 El gremio ha derrotado al ${room.bossName}."),
                updatedAt = now
            )
        }

        val nextTurn = computeNextTurn(room, participants)
        val nextUid = nextTurn.first
        val nextIndex = nextTurn.second

        return room.copy(
            bossHpCurrent = newBossHp,
            currentTurnUid = nextUid,
            turnIndex = nextIndex,
            round = if (nextUid == BOSS_TURN_UID) room.round + 1 else room.round,
            battleLog = updatedLog,
            updatedAt = now
        )
    }

    private fun buildNextRoomAfterBossAction(
        room: GuildBossRoom,
        participants: List<GuildBossParticipant>,
        now: Long,
        newLogLine: String
    ): GuildBossRoom {
        val updatedLog = trimLog(room.battleLog + newLogLine)
        val nextTurn = computeNextTurn(room, participants)
        val nextUid = nextTurn.first
        val nextIndex = nextTurn.second

        return room.copy(
            currentTurnUid = nextUid,
            turnIndex = nextIndex,
            round = if (nextUid == BOSS_TURN_UID) room.round + 1 else room.round,
            battleLog = updatedLog,
            updatedAt = now
        )
    }

    private fun computeNextTurn(
        room: GuildBossRoom,
        participants: List<GuildBossParticipant>
    ): Pair<String, Int> {
        if (room.turnOrder.isEmpty()) return BOSS_TURN_UID to 0

        val aliveMap = participants.associateBy { it.uid }
        val total = room.turnOrder.size
        var idx = room.turnIndex

        repeat(total) {
            idx = (idx + 1) % total
            val candidate = room.turnOrder[idx]

            if (candidate == BOSS_TURN_UID) {
                return BOSS_TURN_UID to idx
            }

            val participant = aliveMap[candidate]
            if (participant != null && participant.alive && participant.hpCurrent > 0) {
                return candidate to idx
            }
        }

        return BOSS_TURN_UID to 0
    }

    private fun trimLog(log: List<String>): List<String> {
        return if (log.size <= 30) log else log.takeLast(30)
    }

    /**
     * Calcula HP y rango de ataque del jefe en función del grupo.
     *
     * HP del jefe = suma de hpMax de todos los participantes × 1.4, redondeado
     * al múltiplo de 5 más cercano. Rango [80, 800].
     *
     * Ataque del jefe = basado en el hpMax promedio de los participantes:
     *   min ≈ 12% del hpMax promedio, max ≈ 28% del hpMax promedio.
     */
    private fun computeBossStats(participants: List<GuildBossParticipant>): Triple<Int, Int, Int> {
        if (participants.isEmpty()) return Triple(250, 12, 22)

        val totalHp = participants.sumOf { it.hpMax }
        val avgHp = totalHp / participants.size

        // HP del jefe: la suma total × 1.4 redondeada a múltiplos de 5
        val rawBossHp = (totalHp * 1.4).toInt()
        val bossHp = ((rawBossHp + 4) / 5 * 5).coerceIn(80, 800)

        // Ataque del jefe: escala con la HP promedio del grupo
        val bossAtkMin = (avgHp * 0.12).toInt().coerceIn(5, 40)
        val bossAtkMax = (avgHp * 0.28).toInt().coerceIn(bossAtkMin + 3, 70)

        return Triple(bossHp, bossAtkMin, bossAtkMax)
    }

    private fun bossArmorClass(room: GuildBossRoom): Int {
        return (10 + (room.bossHpMax / 50).coerceIn(2, 6)).coerceAtLeast(12)
    }

    private fun bossAttackBonus(room: GuildBossRoom): Int {
        return (4 + (room.round / 3)).coerceAtLeast(4)
    }

    private data class AttackRollResult(
        val rolls: List<Int>,
        val keptRoll: Int,
        val total: Int,
        val hit: Boolean,
        val crit: Boolean,
        val rollText: String
    )

    private fun rollAttackAgainst(
        attackBonus: Int,
        targetArmorClass: Int,
        advantage: Boolean
    ): AttackRollResult {
        val rolls = if (advantage) {
            listOf(rollD20(), rollD20())
        } else {
            listOf(rollD20())
        }

        val kept = if (advantage) rolls.maxOrNull() ?: rolls.first() else rolls.first()
        val crit = kept == 20
        val autoMiss = kept == 1
        val total = kept + attackBonus
        val hit = crit || (!autoMiss && total >= targetArmorClass)

        return AttackRollResult(
            rolls = rolls,
            keptRoll = kept,
            total = total,
            hit = hit,
            crit = crit,
            rollText = if (advantage) "${rolls[0]}/${rolls[1]}" else rolls[0].toString()
        )
    }

    private fun rollD20(): Int = Random.nextInt(1, 21)

    private fun rollIntBetween(min: Int, max: Int): Int {
        val realMin = min.coerceAtLeast(1)
        val realMax = max.coerceAtLeast(realMin)
        return Random.nextInt(realMin, realMax + 1)
    }

    private fun parseDice(expr: String): Triple<Int, Int, Int> {
        val clean = expr.trim().lowercase()
        val match = """(\d*)d(\d+)(?:\+(\d+))?""".toRegex().find(clean)
            ?: return Triple(1, 6, 0)

        val count = match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
        val sides = match.groupValues[2].toIntOrNull()?.coerceAtLeast(2) ?: 6
        val bonus = match.groupValues[3].toIntOrNull() ?: 0
        return Triple(count, sides, bonus)
    }

    private fun rollDiceExpression(expr: String, crit: Boolean = false): Int {
        val (count, sides, bonus) = parseDice(expr)
        val actualCount = if (crit) count * 2 else count
        val total = (1..actualCount).sumOf { Random.nextInt(1, sides + 1) } + bonus
        return total.coerceAtLeast(1)
    }

    private fun diceMinMax(expr: String, flatBonus: Int = 0): Pair<Int, Int> {
        val (count, sides, bonus) = parseDice(expr)
        val min = (count + bonus + flatBonus).coerceAtLeast(1)
        val max = (count * sides + bonus + flatBonus).coerceAtLeast(min)
        return min to max
    }

    private suspend fun loadLiveCharacterState(
        uid: String,
        baseCharacter: Character
    ): Character {
        val snap = partidaRef(partidaId(uid, baseCharacter.name)).get().await()
        if (!snap.exists()) return baseCharacter

        val inventory = parseInventory(snap.get("inventory"))
        val equipment = parseEquippedItems(snap.get("equipment"))
        val hpMax = snap.getLong("hpMax")?.toInt() ?: baseCharacter.hpMax
        val hpCurrent = snap.getLong("hpCurrent")?.toInt() ?: baseCharacter.hpCurrent
        val level = snap.getLong("level")?.toInt() ?: baseCharacter.level
        val xp = snap.getLong("xp")?.toInt() ?: baseCharacter.xp
        val coins = snap.getLong("coins")?.toInt() ?: baseCharacter.coins
        val lastPlayed = snap.getLong("lastPlayed") ?: baseCharacter.lastPlayed

        return baseCharacter.copy(
            hpMax = hpMax,
            hpCurrent = hpCurrent,
            inventory = inventory,
            equipment = equipment,
            level = level,
            xp = xp,
            coins = coins,
            lastPlayed = lastPlayed
        )
    }

    private fun computeBasicAttackRange(character: Character): Pair<Int, Int> {
        val weaponDamageExpr = character.equippedWeapon?.resolvedWeaponDamage ?: "1d4"
        val bonus = character.weaponDamageBonus
        return diceMinMax(weaponDamageExpr, flatBonus = bonus)
    }

    private fun parseInventory(raw: Any?): List<Item> {
        val rawList = raw as? List<*> ?: return emptyList()
        return rawList.mapNotNull { entry ->
            (entry as? Map<*, *>)?.let { parseItemMap(it) }
        }
    }

    private fun parseEquippedItems(raw: Any?): EquippedItems {
        val map = raw as? Map<*, *> ?: return EquippedItems()

        fun parseSlot(vararg keys: String): Item? {
            val rawItem = keys
                .asSequence()
                .mapNotNull { key -> map[key] }
                .firstOrNull() ?: return null
            return parseItemMap(rawItem as Map<*, *>)
        }

        return EquippedItems(
            head = parseSlot("head", "cabeza"),
            chest = parseSlot("chest", "pecho", "torso"),
            legs = parseSlot("legs", "piernas"),
            feet = parseSlot("feet", "pies"),
            hands = parseSlot("hands", "manos"),
            mainHand = parseSlot("main_hand", "mano_principal", "mano principal"),
            offHand = parseSlot("off_hand", "mano_secundaria", "mano secundaria"),
            ring = parseSlot("ring", "anillo", "ring1", "anillo1"),
            ring2 = parseSlot("ring2", "anillo2"),
            amulet = parseSlot("amulet", "amuleto")
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

        val enchantmentsRaw = m["enchantments"] as? List<*>
        val enchantments = enchantmentsRaw?.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null

            val enchStatBonusesRaw = map["statBonuses"] as? Map<*, *>
            val enchStatBonuses = enchStatBonusesRaw
                ?.mapNotNull { (k, v) ->
                    val key = k as? String ?: return@mapNotNull null
                    val value = (v as? Number)?.toInt() ?: return@mapNotNull null
                    key to value
                }
                ?.toMap()
                ?: emptyMap()

            ItemEnchantment(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                statBonuses = enchStatBonuses,
                armorBonus = (map["armorBonus"] as? Number)?.toInt() ?: 0,
                attackBonus = (map["attackBonus"] as? Number)?.toInt() ?: 0,
                weaponDamageBonus = (map["weaponDamageBonus"] as? Number)?.toInt() ?: 0
            )
        } ?: emptyList()

        return Item(
            id = m["id"] as? String ?: System.currentTimeMillis().toString(),
            name = m["name"] as? String ?: "Objeto sin nombre",
            description = m["description"] as? String ?: "",
            type = m["type"] as? String ?: "consumible",
            effect = m["effect"] as? String ?: "",
            equipSlot = normalizeEquipSlot(m["equipSlot"] as? String ?: ""),
            weaponDamage = m["weaponDamage"] as? String ?: "",
            armorBase = (m["armorBase"] as? Number)?.toInt(),
            armorBonus = (m["armorBonus"] as? Number)?.toInt() ?: 0,
            maxDexBonus = (m["maxDexBonus"] as? Number)?.toInt(),
            handedness = m["handedness"] as? String ?: "one_hand",
            statBonuses = statBonuses,
            rarity = m["rarity"] as? String ?: "common",
            enchantments = enchantments,
            setId = m["setId"] as? String ?: "",
            setName = m["setName"] as? String ?: ""
        )
    }

    private fun itemToMap(item: Item): Map<String, Any> {
        val out = mutableMapOf<String, Any>(
            "id" to item.id.ifBlank { System.currentTimeMillis().toString() },
            "name" to item.name,
            "description" to item.description,
            "type" to item.type,
            "effect" to item.effect,
            "rarity" to item.rarity
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
        if (item.enchantments.isNotEmpty()) {
            out["enchantments"] = item.enchantments.map { ench ->
                mutableMapOf<String, Any>(
                    "id" to ench.id,
                    "name" to ench.name,
                    "description" to ench.description
                ).apply {
                    if (ench.statBonuses.isNotEmpty()) this["statBonuses"] = ench.statBonuses
                    if (ench.armorBonus != 0) this["armorBonus"] = ench.armorBonus
                    if (ench.attackBonus != 0) this["attackBonus"] = ench.attackBonus
                    if (ench.weaponDamageBonus != 0) this["weaponDamageBonus"] = ench.weaponDamageBonus
                }
            }
        }
        if (item.setId.isNotBlank()) out["setId"] = item.setId
        if (item.setName.isNotBlank()) out["setName"] = item.setName

        return out
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

    private fun extractHealingExpression(item: Item): String {
        val allText = listOf(item.effect, item.description, item.name)
            .joinToString(" ")
            .lowercase()

        val match = """\d+d\d+(?:\+\d+)?""".toRegex().find(allText)
        return match?.value ?: "1d6+2"
    }
}