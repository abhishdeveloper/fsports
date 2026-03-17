package com.college.sportsmeet.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.college.sportsmeet.databinding.ItemHistoryCardBinding
import com.college.sportsmeet.models.PredictionHistoryItem

/**
 * Adapter for displaying the User's Prediction History.
 */
class HistoryAdapter : ListAdapter<PredictionHistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PredictionHistoryItem) {
            binding.apply {
                tvMatchName.text = "${item.sportName}: ${item.matchName}"
                tvPoints.text = "x${item.pointsMultiplier} Pts"
                tvQuestion.text = item.questionText
                chipSelectedAnswer.text = "Pick: ${item.selectedAnswer ?: "Unknown"}"

                // Style the Outcome Chip based on 'pending', 'won', or 'lost'
                chipOutcome.text = item.status.uppercase()
                when (item.status) {
                    "won" -> {
                        chipOutcome.setChipBackgroundColorResource(android.R.color.holo_green_light)
                        chipOutcome.setTextColor(Color.WHITE)
                    }
                    "lost" -> {
                        chipOutcome.setChipBackgroundColorResource(android.R.color.holo_red_light)
                        chipOutcome.setTextColor(Color.WHITE)
                    }
                    else -> { // Pending
                        chipOutcome.setChipBackgroundColorResource(android.R.color.darker_gray)
                        chipOutcome.setTextColor(Color.WHITE)
                    }
                }
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<PredictionHistoryItem>() {
        override fun areItemsTheSame(oldItem: PredictionHistoryItem, newItem: PredictionHistoryItem): Boolean {
            return oldItem.predictionId == newItem.predictionId
        }

        override fun areContentsTheSame(oldItem: PredictionHistoryItem, newItem: PredictionHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}