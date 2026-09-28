package com.example.aitester.ui.debug

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.aitester.R
import com.example.aitester.data.preferences.PreferencesManager
import com.example.aitester.databinding.ActivityDebugBinding
import com.example.aitester.ui.common.LocaleHelper
import com.example.aitester.ui.common.ThemeHelper
import kotlinx.coroutines.launch
import java.util.Locale

class DebugActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugBinding
    private lateinit var preferencesManager: PreferencesManager

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)

        setupWindowInsets()
        setupToolbar()
        loadDebugInfo()
        setupActions()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = insets.top)
            windowInsets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadDebugInfo() {
        // Device Info
        val locale = Locale.getDefault()
        binding.debugLocale.text = "Locale: ${locale.language}_${locale.country} (${locale.displayName})"
        binding.debugLanguage.text = "System Language: ${Locale.getDefault().displayLanguage}"
        binding.debugAndroidVersion.text = "Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"

        // App Settings
        val savedLang = preferencesManager.getLanguageSync()
        binding.debugSavedLanguage.text = "Saved Language: $savedLang"
        binding.debugDarkMode.text = "Dark Mode: ${preferencesManager.getDarkModeSync()}"
        binding.debugTheme.text = "Theme: ${preferencesManager.getThemeSync()}"

        lifecycleScope.launch {
            val (owner, repo) = preferencesManager.getFullRepo()
            binding.debugRepo.text = "Repo: $owner/$repo"
        }
    }

    private fun setupActions() {
        binding.btnClearPrefs.setOnClickListener {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply()
            Toast.makeText(this, "Preferences cleared", Toast.LENGTH_SHORT).show()
            loadDebugInfo()
        }

        binding.btnTestCrash.setOnClickListener {
            throw Exception("Test crash from DebugActivity")
        }
    }
}
