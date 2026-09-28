package com.example.aitester.ui.common

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "language"

    fun toLocaleList(language: String): LocaleListCompat {
        return when (language) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            "cs" -> LocaleListCompat.forLanguageTags("cs")
            "sk" -> LocaleListCompat.forLanguageTags("sk")
            "de" -> LocaleListCompat.forLanguageTags("de")
            "de_AT" -> LocaleListCompat.forLanguageTags("de-AT")
            "pl" -> LocaleListCompat.forLanguageTags("pl")
            "it" -> LocaleListCompat.forLanguageTags("it")
            "ru" -> LocaleListCompat.forLanguageTags("ru")
            "uk" -> LocaleListCompat.forLanguageTags("uk")
            else -> LocaleListCompat.getEmptyLocaleList() // "system" = follow system
        }
    }

    /** Applies the language app-wide (backported to API 24+ via AppCompat). */
    fun applyAppLanguage(language: String) {
        AppCompatDelegate.setApplicationLocales(toLocaleList(language))
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "system") ?: "system"
    }

    /**
     * Wraps context with the saved locale so that Activities show the
     * selected language right away (needed mainly on API < 33).
     * "system" (or unknown) returns the base context unchanged.
     */
    fun wrap(base: Context): Context {
        val lang = getSavedLanguage(base)
        val locale = when (lang) {
            "en" -> Locale("en")
            "cs" -> Locale("cs")
            "sk" -> Locale("sk")
            "de" -> Locale("de")
            "de_AT" -> Locale("de", "AT")
            "pl" -> Locale("pl")
            "it" -> Locale("it")
            "ru" -> Locale("ru")
            "uk" -> Locale("uk")
            else -> return base
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
