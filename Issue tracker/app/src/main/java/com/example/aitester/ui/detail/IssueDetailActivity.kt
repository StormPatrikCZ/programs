package com.example.aitester.ui.detail

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.aitester.R
import com.example.aitester.data.model.GitHubIssue
import com.example.aitester.data.network.GitHubService
import com.example.aitester.data.preferences.PreferencesManager
import com.example.aitester.databinding.ActivityIssueDetailBinding
import com.example.aitester.ui.common.LocaleHelper
import com.example.aitester.ui.common.ThemeHelper
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class IssueDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIssueDetailBinding
    private lateinit var gitHubService: GitHubService
    private lateinit var preferencesManager: PreferencesManager

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityIssueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Slide in animation
        overridePendingTransitionCompat(R.anim.slide_in_right, R.anim.slide_out_left)

        preferencesManager = PreferencesManager(this)

        setupWindowInsets()
        setupToolbar()

        val issue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("issue", GitHubIssue::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("issue")
        }
        if (issue != null) {
            displayIssue(issue)
            lifecycleScope.launch {
                val (owner, repo) = preferencesManager.getFullRepo()
                gitHubService = GitHubService(owner, repo)
                loadComments(issue.number)
            }
        } else {
            finish()
        }
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

    private fun displayIssue(issue: GitHubIssue) {
        binding.toolbar.title = "#${issue.number}"

        // State badge (tint keeps the OneUI pill shape)
        if (issue.state == "open") {
            binding.issueState.text = "○ " + getString(R.string.state_open)
            binding.issueState.backgroundTintList = ColorStateList.valueOf(
                MaterialColors.getColor(
                    binding.issueState,
                    com.google.android.material.R.attr.colorSecondary
                )
            )
        } else {
            binding.issueState.text = "✓ " + getString(R.string.state_closed)
            binding.issueState.backgroundTintList = ColorStateList.valueOf(
                MaterialColors.getColor(
                    binding.issueState,
                    com.google.android.material.R.attr.colorError
                )
            )
        }

        binding.issueTitle.text = issue.title
        binding.issueAuthor.text = issue.user.login
        binding.issueDate.text = formatDate(issue.createdAt)
        binding.issueCommentsCount.text = getString(R.string.comments_count, issue.comments)

        if (issue.body.isNotBlank()) {
            binding.issueBody.visibility = View.VISIBLE
            binding.issueBody.text = issue.body
        } else {
            binding.issueBody.visibility = View.GONE
        }

        // Labels
        binding.issueLabels.removeAllViews()
        issue.labels.forEach { label ->
            val chip = Chip(this).apply {
                text = label.name
                isClickable = false
                try {
                    val color = Color.parseColor("#${label.color}")
                    chipBackgroundColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ColorStateList.valueOf(color)
                    } else {
                        @Suppress("DEPRECATION")
                        ColorStateList(arrayOf(IntArray(0)), intArrayOf(color))
                    }
                    setTextColor(getContrastColor(color))
                } catch (e: Exception) {
                    chipStrokeWidth = 1f
                }
                textSize = 10f
            }
            binding.issueLabels.addView(chip)
        }
    }

    private fun loadComments(issueNumber: Int) {
        binding.commentsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = gitHubService.getIssueComments(issueNumber)

            binding.commentsLoading.visibility = View.GONE

            result.onSuccess { comments ->
                if (comments.isEmpty()) {
                    binding.commentsError.visibility = View.VISIBLE
                    binding.commentsError.text = getString(R.string.no_comments)
                    binding.commentsError.setTextColor(
                        MaterialColors.getColor(
                            binding.commentsError,
                            com.google.android.material.R.attr.colorOnSurfaceVariant
                        )
                    )
                } else {
                    displayComments(comments)
                }
            }.onFailure { error ->
                binding.commentsError.visibility = View.VISIBLE
                binding.commentsError.text = error.message
            }
        }
    }

    private fun displayComments(comments: List<com.example.aitester.data.model.GitHubComment>) {
        binding.commentsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        comments.forEach { comment ->
            val view = inflater.inflate(R.layout.item_comment, binding.commentsContainer, false)
            view.findViewById<TextView>(R.id.comment_author).text = comment.user.login
            view.findViewById<TextView>(R.id.comment_date).text = formatDate(comment.createdAt)
            view.findViewById<TextView>(R.id.comment_body).text = comment.body
            binding.commentsContainer.addView(view)
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val output = SimpleDateFormat("d. MMM yyyy, HH:mm", Locale.getDefault())
            val date = input.parse(dateStr)
            date?.let { output.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr.take(10)
        }
    }

    private fun getContrastColor(color: Int): Int {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    override fun finish() {
        super.finish()
        overridePendingTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    @Suppress("DEPRECATION")
    private fun overridePendingTransitionCompat(enterAnim: Int, exitAnim: Int) {
        overridePendingTransition(enterAnim, exitAnim)
    }
}
