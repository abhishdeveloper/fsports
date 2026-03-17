package com.college.sportsmeet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.college.sportsmeet.databinding.ItemMatchCardBinding
import com.college.sportsmeet.models.MatchData
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Highly efficient RecyclerView Adapter utilizing ListAdapter and DiffUtil
 * to animate updates and prevent unnecessary complete UI redraws.
 */
class MatchAdapter(
    private val onMatchClicked: (MatchData) -> Unit
) : ListAdapter<MatchData, MatchAdapter.MatchViewHolder>(MatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = getItem(position)
        holder.bind(match)
    }

    inner class MatchViewHolder(private val binding: ItemMatchCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMatchClicked(getItem(position))
                }
            }
        }

        fun bind(match: MatchData) {
            binding.apply {
                tvSportName.text = match.sport
                tvMatchStatus.text = match.status.uppercase(Locale.getDefault())
                tvTeamA.text = match.teamA.name
                tvTeamB.text = match.teamB.name

                if (match.totalPredictions > 0) {
                    llTrending.visibility = android.view.View.VISIBLE
                    tvTrendingCount.text = "🔥 ${match.totalPredictions} predictions locked!"
                } else {
                    llTrending.visibility = android.view.View.GONE
                }

                // Format the backend timestamp (e.g., "2023-10-24 14:00:00") into a readable format
                tvStartTime.text = formatDateTime(match.startTime)
            }
        }

        /**
         * Helper to format SQL DATETIME to a user-friendly string.
         */
        private fun formatDateTime(dateTimeString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Assuming backend stores UTC

                val date = inputFormat.parse(dateTimeString) ?: return dateTimeString

                val outputFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getDefault() // Convert to user's local timezone

                outputFormat.format(date)
            } catch (e: Exception) {
                dateTimeString // Fallback to raw string if parsing fails
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class MatchDiffCallback : DiffUtil.ItemCallback<MatchData>() {
        override fun areItemsTheSame(oldItem: MatchData, newItem: MatchData): Boolean {
            // Check if items represent the same entity by ID
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MatchData, newItem: MatchData): Boolean {
            // Check if the data content is exactly the same
            return oldItem == newItem
        }
    }
}