package com.example.aidungeonmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.ChatMessage
import com.example.aidungeonmaster.data.repository.ChatRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel que coordina el estado y la lógica de private chat.
class PrivateChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private var messagesListener: ListenerRegistration? = null

    private val _chatId = MutableStateFlow<String?>(null)
    val chatId = _chatId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    // Abre chat.
    fun openChat(friendUid: String, guildId: String? = null) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val resolvedChatId = repository.getOrCreatePrivateChat(friendUid, guildId)
                _chatId.value = resolvedChatId

                messagesListener?.remove()
                messagesListener = repository.listenMessages(
                    chatId = resolvedChatId,
                    onChange = { newMessages ->
                        _messages.value = newMessages
                        markIncomingMessagesAsSeen()
                    },
                    onError = { _message.value = it }
                )
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo abrir el chat"
            } finally {
                _loading.value = false
            }
        }
    }

    // Envía message.
    fun sendMessage(text: String) {
        val currentChatId = _chatId.value ?: return

        viewModelScope.launch {
            try {
                repository.sendMessage(currentChatId, text)
            } catch (e: Exception) {
                _message.value = e.message ?: "No se pudo enviar el mensaje"
            }
        }
    }

    // Ejecuta la lógica de mark incoming messages as seen.
    fun markIncomingMessagesAsSeen() {
        val currentChatId = _chatId.value ?: return
        val currentMessages = _messages.value

        viewModelScope.launch {
            try {
                repository.markMessagesAsSeen(currentChatId, currentMessages)
            } catch (_: Exception) {
            }
        }
    }

    // Limpia message.
    fun clearMessage() {
        _message.value = null
    }

    // Gestiona el evento de cleared.
    override fun onCleared() {
        messagesListener?.remove()
        super.onCleared()
    }
}
