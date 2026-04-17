package com.example.aidungeonmaster.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.AppUser
import com.example.aidungeonmaster.data.model.FriendRequest
import com.example.aidungeonmaster.data.model.FriendWithProfile
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.data.model.GuildMemberSummary
import com.example.aidungeonmaster.data.repository.GuildDetailsRepository
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {

    private val repository = SocialRepository()
    private val guildDetailsRepository = GuildDetailsRepository()

    private var incomingRequestsListener: ListenerRegistration? = null
    private var friendsListener: ListenerRegistration? = null
    private var guildsListener: ListenerRegistration? = null

    private val _searchResults = MutableStateFlow<List<AppUser>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val incomingRequests = _incomingRequests.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendWithProfile>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _profile = MutableStateFlow<AppUser?>(null)
    val profile = _profile.asStateFlow()

    private val _myGuilds = MutableStateFlow<List<Guild>>(emptyList())
    val myGuilds = _myGuilds.asStateFlow()

    private val _guildSearchResults = MutableStateFlow<List<Guild>>(emptyList())
    val guildSearchResults = _guildSearchResults.asStateFlow()

    private val _selectedGuild = MutableStateFlow<Guild?>(null)
    val selectedGuild = _selectedGuild.asStateFlow()

    private val _selectedGuildMembers = MutableStateFlow<List<GuildMemberSummary>>(emptyList())
    val selectedGuildMembers = _selectedGuildMembers.asStateFlow()

    private val _isGuildMembersLoading = MutableStateFlow(false)
    val isGuildMembersLoading = _isGuildMembersLoading.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _lastGuildQuery = MutableStateFlow("")
    val lastGuildQuery: StateFlow<String> = _lastGuildQuery

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

    fun loadMyProfile() {
        val uid = repository.currentUid() ?: return
        loadProfile(uid)
    }

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

    fun loadProfile(userUid: String) {
        viewModelScope.launch {
            try {
                _profile.value = repository.getUserProfile(userUid)
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo cargar el perfil"
            }
        }
    }

    fun saveMyProfile(displayName: String, bio: String, accentColor: String, backgroundColor: String) {
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

    fun startIncomingRequestsListener() {
        if (incomingRequestsListener != null) return
        incomingRequestsListener = repository.listenIncomingRequests(
            onChange = { _incomingRequests.value = it },
            onError = { _message.value = it }
        )
    }

    fun stopIncomingRequestsListener() {
        incomingRequestsListener?.remove()
        incomingRequestsListener = null
    }

    fun startFriendsListener() {
        if (friendsListener != null) return
        friendsListener = repository.listenFriends(
            onChange = { _friends.value = it },
            onError = { _message.value = it }
        )
    }

    fun stopFriendsListener() {
        friendsListener?.remove()
        friendsListener = null
    }

    fun startGuildsListener() {
        if (guildsListener != null) return
        guildsListener = repository.listenMyGuilds(
            onChange = { _myGuilds.value = it },
            onError = { _message.value = it }
        )
    }

    fun stopGuildsListener() {
        guildsListener?.remove()
        guildsListener = null
    }

    fun searchGuilds(query: String) {
        _lastGuildQuery.value = query
        viewModelScope.launch {
            try {
                _guildSearchResults.value = repository.searchGuilds(query)
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudieron buscar gremios"
            }
        }
    }

    fun createGuild(name: String, description: String, accentColor: String, bannerColor: String) {
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

    fun joinGuild(guild: Guild) {
        viewModelScope.launch {
            runCatching {
                repository.joinGuild(guild)
            }.onSuccess {
                _message.value = "Te has unido al gremio"
                startGuildsListener()
                loadGuildSearchResults(_lastGuildQuery.value)
                if (_selectedGuild.value?.id == guild.id) {
                    openGuildDetails(guild.copy(joined = true, memberCount = guild.memberCount + 1))
                }
            }.onFailure {
                _message.value = it.message ?: "No se pudo unir al gremio"
            }
        }
    }

    fun leaveGuild(guild: Guild) {
        viewModelScope.launch {
            runCatching {
                repository.leaveGuild(guild)
            }.onSuccess {
                _message.value = "Has abandonado el gremio"
                closeGuildDetails()
                startGuildsListener()
                loadGuildSearchResults(_lastGuildQuery.value)
            }.onFailure {
                _message.value = it.message ?: "No se pudo abandonar el gremio"
            }
        }
    }

    fun transferLeadership(guild: Guild, newLeaderUid: String) {
        viewModelScope.launch {
            runCatching {
                repository.transferGuildLeadership(guild, newLeaderUid)
            }.onSuccess {
                _message.value = "Liderazgo transferido correctamente"
                val updatedGuild = guild.copy(ownerUid = newLeaderUid)
                openGuildDetails(updatedGuild)
                startGuildsListener()
                loadGuildSearchResults(_lastGuildQuery.value)
            }.onFailure {
                _message.value = it.message ?: "No se pudo transferir el liderazgo"
            }
        }
    }

    fun loadGuildSearchResults(query: String) {
        _lastGuildQuery.value = query
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

    fun openGuildDetails(guild: Guild) {
        _selectedGuild.value = guild
        _selectedGuildMembers.value = emptyList()

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

    fun closeGuildDetails() {
        _selectedGuild.value = null
        _selectedGuildMembers.value = emptyList()
        _isGuildMembersLoading.value = false
    }

    fun canLeaveGuild(guild: Guild): Boolean {
        val myUid = repository.currentUid().orEmpty()
        return guild.joined && guild.ownerUid != myUid
    }

    fun isGuildOwner(guild: Guild): Boolean {
        return repository.currentUid() == guild.ownerUid
    }

    fun currentUserUid(): String = repository.currentUid().orEmpty()

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

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        stopIncomingRequestsListener()
        stopFriendsListener()
        stopGuildsListener()
        super.onCleared()
    }
}