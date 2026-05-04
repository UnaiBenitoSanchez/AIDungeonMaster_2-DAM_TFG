package com.example.aidungeonmaster.utils

// Clase que encapsula la lógica de dice roller.
object DiceRoller {

    // Ejecuta la lógica de roll stat.
    private fun rollStat(): Int {
        val rolls = List(4) { (1..6).random() }
        return rolls.sortedDescending().take(3).sum()
    }

    // Ejecuta la lógica de roll all stats.
    fun rollAllStats(): Map<String, Int> {
        return mapOf(
            "Fuerza" to rollStat(),
            "Destreza" to rollStat(),
            "Constitución" to rollStat(),
            "Inteligencia" to rollStat(),
            "Sabiduría" to rollStat(),
            "Carisma" to rollStat()
        )
    }
}
