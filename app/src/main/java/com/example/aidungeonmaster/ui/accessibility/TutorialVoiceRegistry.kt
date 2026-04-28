package com.example.aidungeonmaster.ui.accessibility

object TutorialVoiceRegistry {
    private var handler: ((String) -> String?)? = null

    fun register(handler: (String) -> String?) {
        this.handler = handler
    }

    fun unregister() {
        handler = null
    }

    fun tryHandle(rawCommand: String): String? {
        return handler?.invoke(rawCommand)
    }
}