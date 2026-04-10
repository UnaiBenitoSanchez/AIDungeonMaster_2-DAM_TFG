package com.example.aidungeonmaster.python

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