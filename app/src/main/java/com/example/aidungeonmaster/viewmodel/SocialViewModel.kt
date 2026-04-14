package com.example.aidungeonmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.AppUser
import com.example.aidungeonmaster.data.model.FriendRequest
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {

    private val repository = SocialRepository()
    private var incomingRequestsListener: ListenerRegistration? = null

    private val _searchResults = MutableStateFlow<List<AppUser>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val incomingRequests = _incomingRequests.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        startIncomingRequestsListener()
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

    private fun startIncomingRequestsListener() {
        incomingRequestsListener?.remove()
        incomingRequestsListener = repository.listenIncomingRequests(
            onChange = { _incomingRequests.value = it },
            onError = { _message.value = it }
        )
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
        incomingRequestsListener?.remove()
        super.onCleared()
    }
}