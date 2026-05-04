package com.example.aidungeonmaster.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.AppUser
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.FriendRequest
import com.example.aidungeonmaster.data.model.FriendWithProfile
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.data.model.GuildBossAbility
import com.example.aidungeonmaster.data.model.GuildBossParticipant
import com.example.aidungeonmaster.data.model.GuildBossRoom
import com.example.aidungeonmaster.data.model.GuildChatMessage
import com.example.aidungeonmaster.data.model.GuildMemberSummary
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.data.repository.GuildDetailsRepository
import com.example.aidungeonmaster.data.repository.GuildRaidRepository
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel que coordina el estado y la lógica de social.
class SocialViewModel : ViewModel() {

    private val repository = SocialRepository()
    private val guildDetailsRepository = GuildDetailsRepository()
    private val guildRaidRepository = GuildRaidRepository()

    private var incomingRequestsListener: ListenerRegistration? = null
    private var friendsListener: ListenerRegistration? = null
    private var guildsListener: ListenerRegistration? = null
    private var guildChatListener: ListenerRegistration? = null

    private var guildBossRoomListener: ListenerRegistration? = null
    private var guildBossParticipantsListener: ListenerRegistration? = null
    private var activeGuildBossGuildId: String? = null

    private val _guildBossRoom = MutableStateFlow<GuildBossRoom?>(null)
    val guildBossRoom = _guildBossRoom.asStateFlow()

    private val _guildBossParticipants = MutableStateFlow<List<GuildBossParticipant>>(emptyList())
    val guildBossParticipants = _guildBossParticipants.asStateFlow()

    private val _guildBossCharacters = MutableStateFlow<List<Character>>(emptyList())
    val guildBossCharacters = _guildBossCharacters.asStateFlow()

    private val _isGuildBossLoading = MutableStateFlow(false)
    val isGuildBossLoading = _isGuildBossLoading.asStateFlow()

    private val _isGuildBossActing = MutableStateFlow(false)
    val isGuildBossActing = _isGuildBossActing.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AppUser>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val incomingRequests = _incomingRequests.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendWithProfile>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _profile = MutableStateFlow<AppUser?>(null)
    val profile = _profile.asStateFlow()

    private val _profileCharacters = MutableStateFlow<List<Character>>(emptyList())
    val profileCharacters = _profileCharacters.asStateFlow()

    private val _myGuilds = MutableStateFlow<List<Guild>>(emptyList())
    val myGuilds = _myGuilds.asStateFlow()

    private val _guildSearchResults = MutableStateFlow<List<Guild>>(emptyList())
    val guildSearchResults = _guildSearchResults.asStateFlow()

    private val _selectedGuild = MutableStateFlow<Guild?>(null)
    val selectedGuild = _selectedGuild.asStateFlow()

    private val _selectedGuildMembers = MutableStateFlow<List<GuildMemberSummary>>(emptyList())
    val selectedGuildMembers = _selectedGuildMembers.asStateFlow()

    private val _selectedGuildChat = MutableStateFlow<List<GuildChatMessage>>(emptyList())
    val selectedGuildChat = _selectedGuildChat.asStateFlow()

    private val _isGuildMembersLoading = MutableStateFlow(false)
    val isGuildMembersLoading = _isGuildMembersLoading.asStateFlow()

    private val _isGuildChatLoading = MutableStateFlow(false)
    val isGuildChatLoading = _isGuildChatLoading.asStateFlow()

    private val _isSendingGuildMessage = MutableStateFlow(false)
    val isSendingGuildMessage = _isSendingGuildMessage.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _lastGuildQuery = MutableStateFlow("")
    val lastGuildQuery: StateFlow<String> = _lastGuildQuery.asStateFlow()

    private val _guildBossConsumables = MutableStateFlow<List<Item>>(emptyList())
    val guildBossConsumables = _guildBossConsumables.asStateFlow()

    private val _userExplicitlyLeftBattle = MutableStateFlow(false)
    val userExplicitlyLeftBattle = _userExplicitlyLeftBattle.asStateFlow()

    private val _guildLeaveCompleted = MutableStateFlow(false)
    val guildLeaveCompleted = _guildLeaveCompleted.asStateFlow()

    // Ejecuta la lógica de refresh boss consumables.
    fun refreshBossConsumables() {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                guildRaidRepository.loadBossConsumables(guildId)
            }.onSuccess {
                _guildBossConsumables.value = it
            }.onFailure {
                _guildBossConsumables.value = emptyList()
            }
        }
    }

    // Ejecuta la lógica de use boss ability.
    fun useBossAbility(ability: GuildBossAbility) {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.useAbility(guildId, ability)
            }.onFailure {
                _message.value = it.message ?: "No se pudo usar la habilidad"
            }
            _isGuildBossActing.value = false
        }
    }

    // Ejecuta la lógica de use boss consumable.
    fun useBossConsumable(item: Item) {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.useConsumable(guildId, item)
            }.onSuccess {
                refreshBossConsumables()
            }.onFailure {
                _message.value = it.message ?: "No se pudo usar el objeto"
            }
            _isGuildBossActing.value = false
        }
    }

    // Inicia boss battle.
    fun startBossBattle() {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.startBattleIfReady(guildId)
            }.onSuccess {
                _message.value = "La pelea ha comenzado"
            }.onFailure {
                _message.value = it.message ?: "No se pudo iniciar la pelea"
            }
            _isGuildBossActing.value = false
        }
    }

    // Ejecuta la lógica de upload my profile photo.
    fun uploadMyProfilePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                repository.updateMyProfilePhoto(context, uri)
            }.onSuccess {
                _message.value = "Foto actualizada"
                repository.currentUid()?.let { loadProfile(it) }
            }.onFailure {
                _message.value = it.message ?: "Error al guardar la foto"
            }
        }
    }

    // Carga my profile.
    fun loadMyProfile() {
        val uid = repository.currentUid() ?: return
        loadProfile(uid)
    }

    // Ejecuta la lógica de search users.
    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                _isSearching.value = true
                _searchResults.value = repository.searchUsers(query)
            } catch (e: Exception) {
                _message.value = e.message ?: "Error buscando usuarios"
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Carga profile.
    fun loadProfile(userUid: String) {
        viewModelScope.launch {
            try {
                _profile.value = repository.getUserProfile(userUid)
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo cargar el perfil"
            }
        }
    }

    // Carga profile characters.
    fun loadProfileCharacters(userUid: String) {
        viewModelScope.launch {
            if (userUid.isBlank()) {
                _profileCharacters.value = emptyList()
                return@launch
            }

            runCatching {
                repository.getUserCharacters(userUid)
            }.onSuccess { characters ->
                _profileCharacters.value = characters
            }.onFailure {
                _profileCharacters.value = emptyList()
                _message.value = it.message ?: "No se pudieron cargar los personajes"
            }
        }
    }

    // Guarda my profile.
    fun saveMyProfile(
        displayName: String,
        bio: String,
        accentColor: String,
        backgroundColor: String
    ) {
        viewModelScope.launch {
            try {
                repository.updateMyProfile(displayName, bio, accentColor, backgroundColor)
                repository.currentUid()?.let { loadProfile(it) }
                _message.value = "Perfil actualizado"
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo actualizar el perfil"
            }
        }
    }

    // Inicia incoming requests listener.
    fun startIncomingRequestsListener() {
        if (incomingRequestsListener != null) return
        incomingRequestsListener = repository.listenIncomingRequests(
            onChange = { _incomingRequests.value = it },
            onError = { _message.value = it }
        )
    }

    // Detiene incoming requests listener.
    fun stopIncomingRequestsListener() {
        incomingRequestsListener?.remove()
        incomingRequestsListener = null
    }

    // Inicia friends listener.
    fun startFriendsListener() {
        if (friendsListener != null) return
        friendsListener = repository.listenFriends(
            onChange = { _friends.value = it },
            onError = { _message.value = it }
        )
    }

    // Detiene friends listener.
    fun stopFriendsListener() {
        friendsListener?.remove()
        friendsListener = null
        repository.clearFriendListeners()
        _friends.value = emptyList()
    }
    // Inicia guilds listener.
    fun startGuildsListener() {
        if (guildsListener != null) return

        guildsListener = repository.listenMyGuilds(
            onChange = { guilds ->
                _myGuilds.value = guilds

                val currentSelected = _selectedGuild.value
                if (currentSelected != null) {
                    val liveGuild = guilds.firstOrNull { it.id == currentSelected.id }
                    if (liveGuild != null) {
                        val changed =
                            liveGuild.ownerUid != currentSelected.ownerUid ||
                                    liveGuild.ownerDisplayName != currentSelected.ownerDisplayName ||
                                    liveGuild.memberCount != currentSelected.memberCount ||
                                    liveGuild.name != currentSelected.name ||
                                    liveGuild.description != currentSelected.description ||
                                    liveGuild.accentColor != currentSelected.accentColor ||
                                    liveGuild.bannerColor != currentSelected.bannerColor ||
                                    liveGuild.joined != currentSelected.joined

                        _selectedGuild.value = liveGuild

                        if (changed) {
                            refreshGuildMembers(liveGuild)
                            restartGuildChatIfNeeded(liveGuild)
                        }
                    } else {
                        closeGuildDetails()
                    }
                }
            },
            onError = { _message.value = it }
        )
    }

    // Detiene guilds listener.
    fun stopGuildsListener() {
        guildsListener?.remove()
        guildsListener = null
    }

    // Limpia guild search.
    fun clearGuildSearch() {
        _lastGuildQuery.value = ""
        _guildSearchResults.value = emptyList()
        _isSearching.value = false
    }

    // Ejecuta la lógica de search guilds.
    fun searchGuilds(query: String) {
        val cleanQuery = query.trim()
        _lastGuildQuery.value = query

        if (cleanQuery.length < 2) {
            _guildSearchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        viewModelScope.launch {
            _isSearching.value = true

            runCatching {
                repository.searchGuilds(cleanQuery)
            }.onSuccess { results ->
                _guildSearchResults.value = results
            }.onFailure {
                _guildSearchResults.value = emptyList()
                _message.value = it.message ?: "No se pudieron buscar gremios"
            }

            _isSearching.value = false
        }
    }

    // Carga guild search results.
    fun loadGuildSearchResults(query: String) {
        _lastGuildQuery.value = query

        if (query.trim().length < 2) {
            _guildSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.searchGuilds(query)
            }.onSuccess { results ->
                _guildSearchResults.value = results
            }.onFailure {
                _message.value = it.message ?: "Error al buscar gremios"
            }
        }
    }

    // Crea guild.
    fun createGuild(
        name: String,
        description: String,
        accentColor: String,
        bannerColor: String
    ) {
        viewModelScope.launch {
            try {
                repository.createGuild(name, description, accentColor, bannerColor)
                _message.value = "Gremio creado correctamente"
                startGuildsListener()
                loadGuildSearchResults(_lastGuildQuery.value)
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo crear el gremio"
            }
        }
    }

    // Actualiza guild colors.
    fun updateGuildColors(guildId: String, accentColor: String, bannerColor: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateGuildColors(guildId, accentColor, bannerColor)
            }.onSuccess {
                _message.value = "Colores del gremio actualizados"
                _selectedGuild.value = _selectedGuild.value?.copy(
                    accentColor = accentColor,
                    bannerColor = bannerColor
                )
                startGuildsListener()
                loadGuildSearchResults(_lastGuildQuery.value)
            }.onFailure {
                _message.value = it.message ?: "No se pudieron actualizar los colores del gremio"
            }
        }
    }

    // Gestiona la unión a guild.
    fun joinGuild(guild: Guild) {
        viewModelScope.launch {
            runCatching {
                repository.joinGuild(guild)
            }.onSuccess {
                _message.value = "Te has unido al gremio"
                startGuildsListener()
                loadGuildSearchResults(_lastGuildQuery.value)

                if (_selectedGuild.value?.id == guild.id) {
                    openGuildDetails(guild.copy(joined = true))
                }
            }.onFailure {
                _message.value = it.message ?: "No se pudo unir al gremio"
            }
        }
    }

    // Gestiona la salida de guild.
    fun leaveGuild(guild: Guild) {
        viewModelScope.launch {
            runCatching {
                repository.leaveGuild(guild)
            }.onSuccess {
                _message.value = "Has abandonado el gremio"

                closeGuildDetails()
                clearGuildSearch()
                startGuildsListener()

                _guildLeaveCompleted.value = true
            }.onFailure {
                _message.value = it.message ?: "No se pudo abandonar el gremio"
            }
        }
    }

    // Ejecuta la lógica de transfer leadership.
    fun transferLeadership(guild: Guild, newLeaderUid: String) {
        viewModelScope.launch {
            runCatching {
                repository.transferGuildLeadership(guild, newLeaderUid)
            }.onSuccess {
                val newLeaderName = _selectedGuildMembers.value
                    .firstOrNull { it.uid == newLeaderUid }
                    ?.displayName
                    .orEmpty()

                _message.value = "Liderazgo transferido correctamente"

                openGuildDetails(
                    guild.copy(
                        ownerUid = newLeaderUid,
                        ownerDisplayName = newLeaderName.ifBlank { guild.ownerDisplayName }
                    )
                )

                startGuildsListener()
                loadGuildSearchResults(_lastGuildQuery.value)
            }.onFailure {
                _message.value = it.message ?: "No se pudo transferir el liderazgo"
            }
        }
    }

    // Abre guild details.
    fun openGuildDetails(guild: Guild) {
        val previousId = _selectedGuild.value?.id
        val newId = guild.id

        _selectedGuild.value = guild

        if (previousId != newId) {
            _selectedGuildMembers.value = emptyList()
            _selectedGuildChat.value = emptyList()
        }

        refreshGuildMembers(guild)
        restartGuildChatIfNeeded(guild)

        if (guild.joined) {
            startGuildBossListeners(guild.id)
            loadBossCharacters()
            refreshBossConsumables()
        } else {
            stopGuildBossListeners()
            _guildBossRoom.value = null
            _guildBossParticipants.value = emptyList()
            _guildBossCharacters.value = emptyList()
            _guildBossConsumables.value = emptyList()
        }
    }

    // Abre guild details by id.
    fun openGuildDetailsById(guildId: String) {
        val fromMine = _myGuilds.value.firstOrNull { it.id == guildId }
        val fromSearch = _guildSearchResults.value.firstOrNull { it.id == guildId }
        val cachedGuild = fromMine ?: fromSearch

        if (cachedGuild != null) {
            openGuildDetails(cachedGuild)
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.getGuildById(guildId)
            }.onSuccess { found ->
                if (found != null) {
                    openGuildDetails(found)
                } else {
                    _message.value = "No se encontró el gremio"
                    _selectedGuild.value = null
                }
            }.onFailure {
                _message.value = it.message ?: "No se pudo abrir el gremio"
                _selectedGuild.value = null
            }
        }
    }

    // Ejecuta la lógica de refresh guild members.
    private fun refreshGuildMembers(guild: Guild) {
        viewModelScope.launch {
            _isGuildMembersLoading.value = true
            runCatching {
                guildDetailsRepository.getGuildMembers(guild)
            }.onSuccess { members ->
                _selectedGuildMembers.value = members
            }.onFailure {
                _message.value = it.message ?: "No se pudieron cargar los integrantes del gremio"
            }
            _isGuildMembersLoading.value = false
        }
    }

    // Ejecuta la lógica de restart guild chat if needed.
    private fun restartGuildChatIfNeeded(guild: Guild) {
        stopGuildChatListener()
        _selectedGuildChat.value = emptyList()

        if (!guild.joined) {
            _isGuildChatLoading.value = false
            return
        }

        _isGuildChatLoading.value = true
        guildChatListener = guildDetailsRepository.listenGuildChat(
            guildId = guild.id,
            onChange = { messages ->
                _selectedGuildChat.value = messages
                _isGuildChatLoading.value = false
            },
            onError = { error ->
                _isGuildChatLoading.value = false
                _message.value = error
            }
        )
    }

    // Envía guild message.
    fun sendGuildMessage(text: String) {
        val guild = _selectedGuild.value ?: return
        if (!guild.joined) {
            _message.value = "Debes unirte al gremio para escribir en su chat."
            return
        }

        viewModelScope.launch {
            _isSendingGuildMessage.value = true
            runCatching {
                guildDetailsRepository.sendGuildChatMessage(guild.id, text.trim())
            }.onFailure {
                _message.value = it.message ?: "No se pudo enviar el mensaje"
            }
            _isSendingGuildMessage.value = false
        }
    }

    // Inicia guild boss listeners.
    private fun startGuildBossListeners(guildId: String) {
        if (activeGuildBossGuildId == guildId &&
            guildBossRoomListener != null &&
            guildBossParticipantsListener != null
        ) {
            return
        }

        stopGuildBossListeners()
        _guildBossRoom.value = null
        _guildBossParticipants.value = emptyList()

        activeGuildBossGuildId = guildId

        guildBossRoomListener = guildRaidRepository.listenBossRoom(
            guildId = guildId,
            onChange = { room -> _guildBossRoom.value = room },
            onError = { _message.value = it }
        )

        guildBossParticipantsListener = guildRaidRepository.listenBossParticipants(
            guildId = guildId,
            onChange = { participants -> _guildBossParticipants.value = participants },
            onError = { _message.value = it }
        )
    }

    // Detiene guild boss listeners.
    private fun stopGuildBossListeners() {
        guildBossRoomListener?.remove()
        guildBossRoomListener = null
        guildBossParticipantsListener?.remove()
        guildBossParticipantsListener = null
        activeGuildBossGuildId = null
    }

    // Carga boss characters.
    fun loadBossCharacters() {
        viewModelScope.launch {
            _isGuildBossLoading.value = true
            runCatching {
                guildRaidRepository.getPlayableCharacters()
            }.onSuccess {
                _guildBossCharacters.value = it
            }.onFailure {
                _message.value = it.message ?: "No se pudieron cargar los personajes"
            }
            _isGuildBossLoading.value = false
        }
    }

    // Selecciona boss character.
    fun selectBossCharacter(character: Character) {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.selectCharacter(guildId, character)
            }.onSuccess {
                _message.value = "Personaje seleccionado: ${character.name}"
                refreshBossConsumables()
            }.onFailure {
                _message.value = it.message ?: "No se pudo elegir el personaje"
            }
            _isGuildBossActing.value = false
        }
    }

    // Actualiza boss ready.
    fun setBossReady(ready: Boolean) {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.setReady(guildId, ready)
            }.onFailure {
                _message.value = it.message ?: "No se pudo actualizar el estado de listo"
            }
            _isGuildBossActing.value = false
        }
    }

    // Ejecuta la lógica de perform boss attack.
    fun performBossAttack() {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.playerAttack(guildId)
            }.onFailure {
                _message.value = it.message ?: "No se pudo ejecutar el ataque"
            }
            _isGuildBossActing.value = false
        }
    }

    // Ejecuta la lógica de resolve boss turn if needed.
    fun resolveBossTurnIfNeeded() {
        val guild = _selectedGuild.value ?: return
        val guildId = guild.id
        if (!guild.joined || !isGuildOwner(guild)) return

        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.resolveBossTurn(guildId)
            }.onFailure {
                _message.value = it.message ?: "No se pudo resolver el turno del jefe"
            }
            _isGuildBossActing.value = false
        }
    }

    // Gestiona la salida de boss room.
    fun leaveBossRoom() {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.leaveBossRoom(guildId)
            }.onFailure {
                _message.value = it.message ?: "No se pudo salir de la sala"
            }
            _isGuildBossActing.value = false
        }
    }

    // Gestiona la salida de boss room silently.
    fun leaveBossRoomSilently() {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                guildRaidRepository.leaveBossRoomIfPresent(guildId)
            }
        }
    }

    // Reinicia boss room.
    fun resetBossRoom() {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.resetBossRoom(guildId)
            }.onFailure {
                _message.value = it.message ?: "No se pudo reiniciar la sala"
            }
            _isGuildBossActing.value = false
        }
    }

    // FIX: Permite al líder terminar la batalla manualmente.
    fun forceEndBattle() {
        val guildId = _selectedGuild.value?.id ?: return
        viewModelScope.launch {
            _isGuildBossActing.value = true
            runCatching {
                guildRaidRepository.forceEndBattle(guildId)
            }.onFailure {
                _message.value = it.message ?: "No se pudo terminar la batalla"
            }
            _isGuildBossActing.value = false
        }
    }

    // Ejecuta la lógica de mark user left battle.
    fun markUserLeftBattle() {
        _userExplicitlyLeftBattle.value = true
    }

    // Limpia user left battle.
    fun clearUserLeftBattle() {
        _userExplicitlyLeftBattle.value = false
    }

    // Detiene guild chat listener.
    private fun stopGuildChatListener() {
        guildChatListener?.remove()
        guildChatListener = null
    }

    // Cierra guild details.
    fun closeGuildDetails() {
        stopGuildChatListener()
        _selectedGuild.value = null
        _selectedGuildMembers.value = emptyList()
        _selectedGuildChat.value = emptyList()
        _isGuildMembersLoading.value = false
        _isGuildChatLoading.value = false
        _isSendingGuildMessage.value = false
        stopGuildBossListeners()
        _guildBossRoom.value = null
        _guildBossParticipants.value = emptyList()
        _guildBossCharacters.value = emptyList()
        _isGuildBossLoading.value = false
        _isGuildBossActing.value = false
        _guildBossConsumables.value = emptyList()
    }

    // Comprueba si leave guild.
    fun canLeaveGuild(guild: Guild): Boolean {
        val myUid = repository.currentUid().orEmpty()
        return guild.joined && guild.ownerUid != myUid
    }

    // Comprueba si guild owner.
    fun isGuildOwner(guild: Guild): Boolean {
        return repository.currentUid() == guild.ownerUid
    }

    // Ejecuta la lógica de current user uid.
    fun currentUserUid(): String = repository.currentUid().orEmpty()

    // Envía friend request.
    fun sendFriendRequest(user: AppUser) {
        viewModelScope.launch {
            try {
                repository.sendFriendRequest(user)
                _message.value = "Solicitud enviada a @${user.username}"
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo enviar la solicitud"
            }
        }
    }

    // Ejecuta la lógica de accept request.
    fun acceptRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                repository.acceptFriendRequest(request)
                _message.value = "Solicitud aceptada"
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo aceptar la solicitud"
            }
        }
    }

    // Ejecuta la lógica de reject request.
    fun rejectRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                repository.rejectFriendRequest(request)
                _message.value = "Solicitud rechazada"
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo rechazar la solicitud"
            }
        }
    }

    // Limpia message.
    fun clearMessage() {
        _message.value = null
    }

    // Ejecuta la lógica de consume guild leave completed.
    fun consumeGuildLeaveCompleted() {
        _guildLeaveCompleted.value = false
    }

    // Gestiona el evento de cleared.
    override fun onCleared() {
        stopIncomingRequestsListener()
        stopFriendsListener()
        stopGuildsListener()
        stopGuildChatListener()
        stopGuildBossListeners()
        super.onCleared()
    }
}
