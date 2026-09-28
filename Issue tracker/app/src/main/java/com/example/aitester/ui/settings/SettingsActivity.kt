package com.example.aitester.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aitester.AITesterApp
import com.example.aitester.R
import com.example.aitester.data.preferences.PreferencesManager
import com.example.aitester.databinding.ActivitySettingsBinding
import com.example.aitester.ui.common.LocaleHelper
import com.example.aitester.ui.common.ThemeHelper
import com.example.aitester.worker.IssueCheckWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferencesManager: PreferencesManager
    private var selectedLanguage: String = ""

    private val languageOptions = listOf(
        LanguageItem("system", R.string.lang_system),
        LanguageItem("en", R.string.lang_en),
        LanguageItem("cs", R.string.lang_cs),
        LanguageItem("sk", R.string.lang_sk),
        LanguageItem("de", R.string.lang_de),
        LanguageItem("de_AT", R.string.lang_de_at),
        LanguageItem("pl", R.string.lang_pl),
        LanguageItem("it", R.string.lang_it),
        LanguageItem("ru", R.string.lang_ru),
        LanguageItem("uk", R.string.lang_uk)
    )

    data class LanguageItem(val code: String, val stringRes: Int)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)

        setupWindowInsets()
        setupToolbar()
        setupLanguageDropdown()
        setupDarkModeToggle()
        setupThemeToggle()
        loadCurrentSettings()
        setupNotificationButton()
        setupSaveButton()
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

    private fun setupLanguageDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            languageOptions.map { getString(it.stringRes) }
        )
        binding.languageDropdown.setAdapter(adapter)

        binding.languageDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedLanguage = languageOptions[position].code
        }
    }

    private fun setupDarkModeToggle() {
        binding.darkModeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btn_dark_mode_light -> AITesterApp.DARK_MODE_LIGHT
                    R.id.btn_dark_mode_dark -> AITesterApp.DARK_MODE_DARK
                    else -> AITesterApp.DARK_MODE_SYSTEM
                }
                applyDarkMode(mode)
            }
        }
    }

    private fun setupThemeToggle() {
        binding.themeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val theme = when (checkedId) {
                    R.id.btn_theme_one_ui -> PreferencesManager.THEME_ONE_UI
                    else -> PreferencesManager.THEME_MATERIAL_YOU
                }
                if (theme != preferencesManager.getThemeSync()) {
                    preferencesManager.saveTheme(theme)
                    recreate()
                }
            }
        }
    }

    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            val owner = preferencesManager.getRepoOwner()
            val repo = preferencesManager.getRepoName()
            binding.repoInput.setText("$owner/$repo")
        }

        // Set language dropdown
        val language = preferencesManager.getLanguageSync()
        selectedLanguage = language
        val languageIndex = languageOptions.indexOfFirst { it.code == language }
        if (languageIndex >= 0) {
            binding.languageDropdown.setText(getString(languageOptions[languageIndex].stringRes), false)
        }

        // Set theme toggle
        when (preferencesManager.getThemeSync()) {
            PreferencesManager.THEME_ONE_UI, PreferencesManager.THEME_ONE_UI_DYNAMIC ->
                binding.themeToggle.check(R.id.btn_theme_one_ui)
            else -> binding.themeToggle.check(R.id.btn_theme_material_you)
        }

        // Set dark mode toggle
        val darkMode = preferencesManager.getDarkModeSync()
        when (darkMode) {
            AITesterApp.DARK_MODE_LIGHT -> binding.darkModeToggle.check(R.id.btn_dark_mode_light)
            AITesterApp.DARK_MODE_DARK -> binding.darkModeToggle.check(R.id.btn_dark_mode_dark)
            else -> binding.darkModeToggle.check(R.id.btn_dark_mode_system)
        }
    }

    private fun setupNotificationButton() {
        binding.notificationSettingsButton.setOnClickListener {
            openNotificationSettings()
        }
        binding.testNotificationButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openNotificationSettings()
            } else {
                com.example.aitester.worker.IssueCheckWorker.showTestNotification(this)
            }
        }
    }

    private fun openNotificationSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyDarkMode(mode: String) {
        val nightMode = when (mode) {
            AITesterApp.DARK_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AITesterApp.DARK_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        preferencesManager.saveDarkMode(mode)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            val input = binding.repoInput.text.toString().trim()

            if (!input.contains("/")) {
                binding.repoInputLayout.error = getString(R.string.format_error)
                return@setOnClickListener
            }

            val parts = input.split("/")
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                binding.repoInputLayout.error = getString(R.string.format_error)
                return@setOnClickListener
            }

            binding.repoInputLayout.error = null

            lifecycleScope.launch {
                preferencesManager.saveRepo(parts[0], parts[1])
                scheduleIssueCheck()

                // Apply language if changed
                val currentLanguage = preferencesManager.getLanguageSync()
                if (selectedLanguage != currentLanguage) {
                    preferencesManager.saveLanguage(selectedLanguage)
                    applyLanguage(selectedLanguage)
                }

                Toast.makeText(this@SettingsActivity, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun scheduleIssueCheck() {
        val workRequest = PeriodicWorkRequestBuilder<IssueCheckWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            IssueCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun applyLanguage(language: String) {
        // Single source of truth: AppCompat per-app locales (backported to API 24+,
        // on API 33+ delegated to the system LocaleManager). The subsequent
        // restart into MainActivity makes the change visible immediately.
        LocaleHelper.applyAppLanguage(language)
        restartApp()
    }

    private fun restartApp() {
        try {
            val intent = Intent(this, com.example.aitester.MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback - just finish
            finishAffinity()
        }
    }
}
