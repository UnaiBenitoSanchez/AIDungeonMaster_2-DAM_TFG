package com.example.aidungeonmaster.python

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.gson.Gson

object PythonJournalBridge {

    private val gson = Gson()

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
}