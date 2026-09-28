package com.example.aitester

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.aitester.ui.common.LocaleHelper

class AITesterApp : Application() {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"

        const val DARK_MODE_SYSTEM = "system"
        const val DARK_MODE_LIGHT = "light"
        const val DARK_MODE_DARK = "dark"

        const val LANG_SYSTEM = "system"
        const val LANG_EN = "en"
        const val LANG_CS = "cs"
        const val LANG_SK = "sk"
        const val LANG_DE = "de"
        const val LANG_DE_AT = "de_AT"
        const val LANG_PL = "pl"
        const val LANG_IT = "it"
        const val LANG_RU = "ru"
        const val LANG_UK = "uk"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applyDarkMode()
        // Apply saved per-app language (AppCompat backport works on API 24+,
        // on API 33+ it delegates to the system LocaleManager).
        LocaleHelper.applyAppLanguage(
            prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        )
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    private fun applyDarkMode() {
        val mode = prefs.getString(KEY_DARK_MODE, DARK_MODE_SYSTEM) ?: DARK_MODE_SYSTEM
        val nightMode = when (mode) {
            DARK_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            DARK_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

}
