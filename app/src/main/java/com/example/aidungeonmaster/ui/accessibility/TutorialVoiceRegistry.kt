package com.example.aidungeonmaster.ui.accessibility

// Clase que encapsula la lógica de tutorial voice registry.
object TutorialVoiceRegistry {
    private var handler: ((String) -> String?)? = null

    // Ejecuta la lógica de register.
    fun register(handler: (String) -> String?) {
        this.handler = handler
    }

    // Ejecuta la lógica de unregister.
    fun unregister() {
        handler = null
    }

    // Ejecuta la lógica de try handle.
    fun tryHandle(rawCommand: String): String? {
        return handler?.invoke(rawCommand)
    }
}
