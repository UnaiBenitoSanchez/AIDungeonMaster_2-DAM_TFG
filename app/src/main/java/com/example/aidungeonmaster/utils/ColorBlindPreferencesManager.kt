package com.example.aidungeonmaster.utils

import android.content.Context
import com.example.aidungeonmaster.ui.theme.ColorBlindType

/**
 * Gestiona la persistencia local de la preferencia de modo daltónico.
 * Utiliza SharedPreferences bajo la clave "accessibility_prefs" para
 * no interferir con otras preferencias ya existentes en la app.
 */
object ColorBlindPreferencesManager {

    private const val PREFS_NAME = "accessibility_prefs"
    private const val KEY_COLOR_BLIND_TYPE = "color_blind_type"

    /**
     * Guarda el tipo de daltonismo seleccionado por el usuario.
     * La operación es asíncrona (apply) para no bloquear el hilo principal.
     */
    fun save(context: Context, type: ColorBlindType) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COLOR_BLIND_TYPE, type.name)
            .apply()
    }

    /**
     * Recupera el tipo guardado. Devuelve [ColorBlindType.NONE] si no
     * hay ninguna preferencia almacenada o si el valor almacenado es inválido
     * (p. ej., tras una actualización que renombre los tipos).
     */
    fun load(context: Context): ColorBlindType {
        val raw = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COLOR_BLIND_TYPE, ColorBlindType.NONE.name)
        return runCatching { ColorBlindType.valueOf(raw.orEmpty()) }
            .getOrDefault(ColorBlindType.NONE)
    }
}
