package com.college.sportsmeet.ui

import android.view.LayoutInflater
import android.view.View
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