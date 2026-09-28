package com.example.aitester.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aitester.R
import com.example.aitester.data.model.GitHubIssue
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Locale

class IssuesAdapter(
    private val onIssueClick: (GitHubIssue) -> Unit
) : ListAdapter<GitHubIssue, IssuesAdapter.IssueViewHolder>(IssueDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_issue, parent, false)
        return IssueViewHolder(view)
    }

    override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IssueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val numberText: TextView = itemView.findViewById(R.id.issue_number)
        private val titleText: TextView = itemView.findViewById(R.id.issue_title)
        private val labelsGroup: ChipGroup = itemView.findViewById(R.id.issue_labels)
        private val bodyText: TextView = itemView.findViewById(R.id.issue_body)
        private val authorText: TextView = itemView.findViewById(R.id.issue_author)
        private val dateText: TextView = itemView.findViewById(R.id.issue_date)
        private val commentsText: TextView = itemView.findViewById(R.id.issue_comments)

        fun bind(issue: GitHubIssue) {
            numberText.text = "#${issue.number}"
            titleText.text = issue.title

            // Labels
            labelsGroup.removeAllViews()
            issue.labels.take(3).forEach { label ->
                @Suppress("DEPRECATION")
                val chip = Chip(itemView.context).apply {
                    text = label.name
                    isClickable = false
                    setChipBackgroundColorResource(android.R.color.transparent)
                    chipStrokeWidth = 1f
                    try {
                        val color = Color.parseColor("#${label.color}")
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(color)
                        setTextColor(color)
                    } catch (e: Exception) {
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(
                            MaterialColors.getColor(
                                itemView,
                                com.google.android.material.R.attr.colorOutline
                            )
                        )
                    }
                    textSize = 10f
                    chipCornerRadius = 8f
                }
                labelsGroup.addView(chip)
            }

            // Body
            if (issue.body.isNotBlank()) {
                bodyText.visibility = View.VISIBLE
                bodyText.text = issue.body
            } else {
                bodyText.visibility = View.GONE
            }

            // Author
            authorText.text = issue.user.login

            // Date
            dateText.text = formatDate(issue.updatedAt)

            // Comments
            commentsText.text = itemView.context.getString(R.string.comments_count, issue.comments)

            // Click
            card.setOnClickListener { onIssueClick(issue) }
        }

        private fun formatDate(dateStr: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                val output = SimpleDateFormat("d.M.yyyy", Locale.getDefault())
                val date = input.parse(dateStr)
                date?.let { output.format(it) } ?: dateStr
            } catch (e: Exception) {
                dateStr.take(10)
            }
        }
    }

    class IssueDiffCallback : DiffUtil.ItemCallback<GitHubIssue>() {
        override fun areItemsTheSame(oldItem: GitHubIssue, newItem: GitHubIssue): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GitHubIssue, newItem: GitHubIssue): Boolean {
            return oldItem == newItem
        }
    }
}
