package com.example.aidungeonmaster.python

import com.chaquo.python.Python
import com.google.gson.Gson

// Puente de integración para python journal.
object PythonJournalBridge {

    private val gson = Gson()

    // Ejecuta la lógica de summarize entries.
    fun summarizeEntries(entries: List<Map<String, Any?>>): String {
        return runCatching {
            val py = Python.getInstance()
            val module = py.getModule("journal_tools")
            val json = gson.toJson(entries)
            module.callAttr("summarize_entries", json).toJava(String::class.java) ?: ""
        }.getOrElse {
            "No se pudo generar el resumen."
        }
    }

    // Ejecuta la lógica de summarize entries by chapter.
    fun summarizeEntriesByChapter(entries: List<Map<String, Any?>>): String {
        return runCatching {
            val py = Python.getInstance()
            val module = py.getModule("journal_tools")
            val json = gson.toJson(entries)
            module.callAttr("summarize_entries_by_chapter", json).toJava(String::class.java) ?: ""
        }.getOrElse {
            "No se pudo generar el resumen del capítulo."
        }
    }

    // Ejecuta la lógica de make chapter title.
    fun makeChapterTitle(entries: List<Map<String, Any?>>): String {
        return runCatching {
            val py = Python.getInstance()
            val module = py.getModule("journal_tools")
            val json = gson.toJson(entries)
            module.callAttr("make_chapter_title", json).toJava(String::class.java) ?: ""
        }.getOrElse {
            "Crónicas del viaje"
        }
    }

    // Ejecuta la lógica de rewrite epic.
    fun rewriteEpic(entry: Map<String, Any?>): String {
        return runCatching {
            val py = Python.getInstance()
            val module = py.getModule("journal_tools")
            val json = gson.toJson(entry)
            module.callAttr("rewrite_entry_epic", json).toJava(String::class.java) ?: ""
        }.getOrElse {
            "No se pudo reescribir la entrada."
        }
    }

    // Construye repeat group key.
    fun buildRepeatGroupKey(entry: Map<String, Any?>): String {
        return runCatching {
            val py = Python.getInstance()
            val module = py.getModule("journal_tools")
            val json = gson.toJson(entry)
            module.callAttr("build_repeat_group_key", json).toJava(String::class.java) ?: ""
        }.getOrElse {
            ""
        }
    }
}
