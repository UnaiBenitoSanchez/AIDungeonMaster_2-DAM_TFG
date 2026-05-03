package com.example.aidungeonmaster.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.aidungeonmaster.R
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val labelRes: Int
) {
    SPANISH("es", R.string.language_spanish),
    ENGLISH("en", R.string.language_english),
    CATALAN("ca", R.string.language_catalan),
    BASQUE("eu", R.string.language_basque),
    GERMAN("de", R.string.language_german),
    FRENCH("fr", R.string.language_french),
    GALICIAN("gl", R.string.language_galician);

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: SPANISH
    }
}

object AppLanguageManager {
    private const val PREFS_NAME = "app_language_prefs"
    private const val KEY_LANGUAGE_CODE = "language_code"

    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE_CODE, AppLanguage.SPANISH.code))
    }

    fun wrapContext(base: Context): Context =
        updateContextLocale(base, getSavedLanguage(base))

    fun applySavedLanguage(context: Context) {
        Locale.setDefault(localeFor(getSavedLanguage(context)))
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_CODE, language.code)
            .apply()

        Locale.setDefault(localeFor(language))
        context.findActivity()?.recreate()
    }

    private fun updateContextLocale(context: Context, language: AppLanguage): Context {
        val locale = localeFor(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(LocaleList(locale))
            }
        }

        return context.createConfigurationContext(configuration)
    }

    private fun localeFor(language: AppLanguage): Locale =
        Locale.forLanguageTag(language.code)

    private tailrec fun Context.findActivity(): Activity? =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
}
