package com.example.aitester.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.aitester.AITesterApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferencesManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_THEME = "app_theme"

        const val THEME_MATERIAL_YOU = "material_you"
        const val THEME_ONE_UI = "one_ui"
        const val THEME_ONE_UI_DYNAMIC = "one_ui_dynamic"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_REPO_OWNER = "repo_owner"
        private const val KEY_REPO_NAME = "repo_name"
        private const val KEY_LAST_ISSUE_ID = "last_known_issue_id"

        private const val DEFAULT_OWNER = "StormPatrikCZ"
        private const val DEFAULT_REPO = "LandawasOS"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Synchronous dark mode access for Application class
    fun getDarkModeSync(): String {
        return prefs.getString(KEY_DARK_MODE, AITesterApp.DARK_MODE_SYSTEM) ?: AITesterApp.DARK_MODE_SYSTEM
    }

    fun saveDarkMode(mode: String) {
        prefs.edit().putString(KEY_DARK_MODE, mode).apply()
    }

    // App theme (material_you, one_ui)
    fun getThemeSync(): String {
        return prefs.getString(KEY_THEME, THEME_MATERIAL_YOU) ?: THEME_MATERIAL_YOU
    }

    fun saveTheme(theme: String) {
        // Use commit() for synchronous save so theme is available on recreate()
        prefs.edit().putString(KEY_THEME, theme).commit()
    }

    // Language (system, en, cs, sk, de, de_AT, pl, it, ru, uk)
    fun getLanguageSync(): String {
        return prefs.getString(KEY_LANGUAGE, AITesterApp.LANG_SYSTEM) ?: AITesterApp.LANG_SYSTEM
    }

    fun saveLanguage(language: String) {
        // Use commit() for synchronous save so language is available on recreate()
        prefs.edit().putString(KEY_LANGUAGE, language).commit()
    }

    // Repo settings (can be async)
    suspend fun getRepoOwner(): String = withContext(Dispatchers.IO) {
        prefs.getString(KEY_REPO_OWNER, DEFAULT_OWNER) ?: DEFAULT_OWNER
    }

    suspend fun getRepoName(): String = withContext(Dispatchers.IO) {
        prefs.getString(KEY_REPO_NAME, DEFAULT_REPO) ?: DEFAULT_REPO
    }

    suspend fun saveRepo(owner: String, repo: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_REPO_OWNER, owner).putString(KEY_REPO_NAME, repo).apply()
    }

    suspend fun getFullRepo(): Pair<String, String> {
        return Pair(getRepoOwner(), getRepoName())
    }

    suspend fun getLastKnownIssueId(): Long = withContext(Dispatchers.IO) {
        prefs.getLong(KEY_LAST_ISSUE_ID, 0L)
    }

    suspend fun saveLastKnownIssueId(id: Long) = withContext(Dispatchers.IO) {
        prefs.edit().putLong(KEY_LAST_ISSUE_ID, id).apply()
    }
}
