package com.example.aidungeonmaster.utils

object DiceRoller {

    private fun rollStat(): Int {
        val rolls = List(4) { (1..6).random() }
        return rolls.sortedDescending().take(3).sum()
    }

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
