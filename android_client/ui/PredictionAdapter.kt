package com.college.sportsmeet.ui

import android.view.LayoutInflater
import android.view.View
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.college.sportsmeet.databinding.ItemQuestionCardBinding
import com.college.sportsmeet.models.QuestionData
import com.college.sportsmeet.models.RivalryStat

/**
 * Adapter for handling Question Lists and their corresponding Option selections.
 * Crucially stores state inside the `QuestionData` model to survive RecyclerView recycling.
 */
class PredictionAdapter : ListAdapter<QuestionData, PredictionAdapter.QuestionViewHolder>(QuestionDiffCallback()) {

    private var rivalryStats: Map<String, RivalryStat>? = null

    // Track which question ID currently holds the single 2x Boost
    private var boostedQuestionId: Long? = null

    fun updateRivalryStats(stats: Map<String, RivalryStat>?) {
        this.rivalryStats = stats
        notifyDataSetChanged() // Rebind all to show stats
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuestionViewHolder(private val binding: ItemQuestionCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Listen to RadioGroup changes and save directly back to the data model
            binding.radioGroupOptions.setOnCheckedChangeListener { group, checkedId ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Provide a light haptic tick (HapticFeedbackConstants.CLOCK_TICK requires API 21)
                    group.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)

                    val question = getItem(position)

                    question.userSelectedOption = when (checkedId) {
                        binding.radioOptionA.id -> "A"
                        binding.radioOptionB.id -> "B"
                        binding.radioOptionC.id -> "C"
                        binding.radioOptionD.id -> "D"
                        else -> null
                    }
                }
            }
        }

        fun bind(question: QuestionData) {
            binding.apply {
                // Remove listener temporarily to avoid triggering it while rebinding recycled views
                radioGroupOptions.setOnCheckedChangeListener(null)

                tvQuestionTitle.text = question.questionText
                tvPoints.text = "${question.pointsMultiplier}x Pts"

                // Always set A and B
                radioOptionA.text = question.optionA
                radioOptionB.text = question.optionB
                radioOptionA.visibility = View.VISIBLE
                radioOptionB.visibility = View.VISIBLE

                // Handle dynamic Option C
                if (!question.optionC.isNullOrBlank()) {
                    radioOptionC.text = question.optionC
                    radioOptionC.visibility = View.VISIBLE
                } else {
                    radioOptionC.visibility = View.GONE
                }

                // Handle dynamic Option D
                if (!question.optionD.isNullOrBlank()) {
                    radioOptionD.text = question.optionD
                    radioOptionD.visibility = View.VISIBLE
                } else {
                    radioOptionD.visibility = View.GONE
                }

                // Restore previous selection from model state to survive view recycling
                radioGroupOptions.clearCheck()
                when (question.userSelectedOption) {
                    "A" -> radioOptionA.isChecked = true
                    "B" -> radioOptionB.isChecked = true
                    "C" -> radioOptionC.isChecked = true
                    "D" -> radioOptionD.isChecked = true
                }

                // Handle Rivalry Stats Visibility
                val stat = rivalryStats?.get(question.questionId.toString())
                if (stat != null) {
                    llRivalryStats.visibility = View.VISIBLE

                    // Modify radio button text to show percentages natively next to options
                    radioOptionA.text = "${question.optionA} (${stat.optA}%)"
                    radioOptionB.text = "${question.optionB} (${stat.optB}%)"
                    if (!question.optionC.isNullOrBlank()) radioOptionC.text = "${question.optionC} (${stat.optC}%)"
                    if (!question.optionD.isNullOrBlank()) radioOptionD.text = "${question.optionD} (${stat.optD}%)"

                    // Optional: Animate progressRivalryA if you want a visual bar indicator
                    // Using option A's percentage for a simple two-way split representation.
                    progressRivalryA.setProgressCompat(stat.optA, true)
                } else {
                    llRivalryStats.visibility = View.GONE
                }

                // Re-attach listener after binding
                radioGroupOptions.setOnCheckedChangeListener { _, checkedId ->
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val currentQuestion = getItem(position)
                        currentQuestion.userSelectedOption = when (checkedId) {
                            binding.radioOptionA.id -> "A"
                            binding.radioOptionB.id -> "B"
                            binding.radioOptionC.id -> "C"
                            binding.radioOptionD.id -> "D"
                            else -> null
                        }
                    }
                }

                // Phase 15: 2x Captain Boost Logic
                btnBoost.setOnClickListener { view ->
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) // Heavy haptic

                        val clickedQuestionId = getItem(position).questionId

                        if (boostedQuestionId == clickedQuestionId) {
                            // User clicked the active boost to remove it
                            getItem(position).isBoosted = false
                            boostedQuestionId = null
                            notifyItemChanged(position)
                        } else {
                            // User clicked to boost a new question.
                            // 1. Remove boost from old question if it exists
                            val oldBoostedId = boostedQuestionId
                            boostedQuestionId = clickedQuestionId
                            getItem(position).isBoosted = true

                            notifyItemChanged(position) // Update the newly boosted row

                            // If another row had the boost, find its position and update it so it turns gray
                            if (oldBoostedId != null) {
                                val oldPosition = currentList.indexOfFirst { it.questionId == oldBoostedId }
                                if (oldPosition != -1) {
                                    getItem(oldPosition).isBoosted = false
                                    notifyItemChanged(oldPosition)
                                }
                            }
                        }
                    }
                }

                // UI Styling for Boost Button
                if (question.isBoosted) {
                    btnBoost.setBackgroundColor(Color.parseColor("#FFD700")) // Vibrant Gold
                    btnBoost.setTextColor(Color.BLACK)
                    btnBoost.setIconTintResource(android.R.color.black)
                    btnBoost.text = "2x Captain Boost Active"
                } else {
                    // Reset to standard gray tonal style
                    val typedValue = android.util.TypedValue()
                    binding.root.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSecondaryContainer, typedValue, true)
                    btnBoost.setBackgroundColor(typedValue.data)

                    binding.root.context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSecondaryContainer, typedValue, true)
                    btnBoost.setTextColor(typedValue.data)
                    btnBoost.setIconTintResource(com.google.android.material.R.color.design_default_color_on_secondary)
                    btnBoost.text = "Apply 2x Captain Boost"
                }
            }
        }
    }

    class QuestionDiffCallback : DiffUtil.ItemCallback<QuestionData>() {
        override fun areItemsTheSame(oldItem: QuestionData, newItem: QuestionData): Boolean {
            return oldItem.questionId == newItem.questionId
        }

        override fun areContentsTheSame(oldItem: QuestionData, newItem: QuestionData): Boolean {
            // Because userSelectedOption is mutable and changes often, DiffUtil will recognize changes to it here
            return oldItem == newItem && oldItem.userSelectedOption == newItem.userSelectedOption
        }
    }
}