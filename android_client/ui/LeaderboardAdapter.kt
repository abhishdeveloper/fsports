package com.college.sportsmeet.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.college.sportsmeet.databinding.ItemLeaderboardCardBinding
import com.college.sportsmeet.models.LeaderboardUser

/**
 * Adapter for displaying the Global Leaderboard.
 * Applies specific styling rules for ranks 1, 2, and 3.
 */
class LeaderboardAdapter : ListAdapter<LeaderboardUser, LeaderboardAdapter.LeaderboardViewHolder>(LeaderboardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LeaderboardViewHolder(private val binding: ItemLeaderboardCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: LeaderboardUser) {
            binding.apply {
                tvRank.text = user.rank.toString()
                tvUsername.text = user.username
                tvBranch.text = user.branch
                tvTotalCoins.text = user.totalCoins.toString()

                // Dynamic UI Polish for Top 3 Ranks
                when (user.rank) {
                    1 -> applyRankStyling("#FFD700", true) // Gold
                    2 -> applyRankStyling("#C0C0C0", true) // Silver
                    3 -> applyRankStyling("#CD7F32", true) // Bronze
                    else -> applyRankStyling(null, false)
                }

                // ⚔️ Campus Rivalry Detection
                // Check if the branch above or below this one is different (meaning a fierce rivalry competition)
                val isRivalry = checkRivalry(bindingAdapterPosition)
                chipRivalry.visibility = if (isRivalry) View.VISIBLE else View.GONE
            }
        }

        private fun checkRivalry(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false

            val currentBranch = getItem(position).branch

            val prevBranch = if (position > 0) getItem(position - 1).branch else currentBranch
            val nextBranch = if (position < itemCount - 1) getItem(position + 1).branch else currentBranch

            // It's a rivalry if they are fighting closely with a different department
            return currentBranch != prevBranch || currentBranch != nextBranch
        }

        private fun applyRankStyling(hexColor: String?, showIcon: Boolean) {
            if (showIcon && hexColor != null) {
                binding.ivRankIcon.visibility = View.VISIBLE
                binding.tvRank.visibility = View.GONE
                binding.ivRankIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(hexColor))
            } else {
                binding.ivRankIcon.visibility = View.GONE
                binding.tvRank.visibility = View.VISIBLE
            }
        }
    }

    class LeaderboardDiffCallback : DiffUtil.ItemCallback<LeaderboardUser>() {
        override fun areItemsTheSame(oldItem: LeaderboardUser, newItem: LeaderboardUser): Boolean {
            return oldItem.username == newItem.username
        }

        override fun areContentsTheSame(oldItem: LeaderboardUser, newItem: LeaderboardUser): Boolean {
            return oldItem == newItem
        }
    }
}