package com.example.aitester.ui.common

import androidx.appcompat.app.AppCompatActivity
import com.example.aitester.R
import com.example.aitester.data.preferences.PreferencesManager
import com.google.android.material.color.DynamicColors

/**
 * Applies the user-selected app theme. Must be called before
 * super.onCreate() in every activity.
 *
 * - Material You: default M3 theme + system dynamic colors (Android 12+).
 * - One UI: own OneUI-style theme, no Samsung libraries.
 */
object ThemeHelper {

    const val THEME_MATERIAL_YOU = "material_you"
    const val THEME_ONE_UI = "one_ui"
    const val THEME_ONE_UI_DYNAMIC = "one_ui_dynamic"

    fun applyTheme(activity: AppCompatActivity) {
        val theme = PreferencesManager(activity).getThemeSync()
        if (theme == THEME_ONE_UI || theme == THEME_ONE_UI_DYNAMIC) {
            // Fixed official Samsung OneUI tokens. Android exposes only the
            // wallpaper palette to third-party apps; Samsung Settings/Theme
            // colors have no public API, so static tokens are correct here.
            activity.setTheme(R.style.Theme_AITESTER_OneUI)
        } else {
            activity.setTheme(R.style.Theme_AITESTER)
            DynamicColors.applyToActivityIfAvailable(activity)
        }
    }
}
