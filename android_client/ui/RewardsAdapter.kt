package com.college.sportsmeet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.college.sportsmeet.databinding.ItemRewardCardBinding
import com.college.sportsmeet.models.RewardData

class RewardsAdapter(
    private val onRedeemClicked: (RewardData) -> Unit
) : ListAdapter<RewardData, RewardsAdapter.RewardViewHolder>(RewardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val binding = ItemRewardCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RewardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RewardViewHolder(private val binding: ItemRewardCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRedeem.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRedeemClicked(getItem(position))
                }
            }
        }

        fun bind(reward: RewardData) {
            binding.apply {
                tvRewardTitle.text = reward.title
                tvRewardDescription.text = reward.description
                tvStock.text = "${reward.stockQuantity} left!"
                btnRedeem.text = "Redeem • ${reward.cost} Coins"
            }
        }
    }

    class RewardDiffCallback : DiffUtil.ItemCallback<RewardData>() {
        override fun areItemsTheSame(oldItem: RewardData, newItem: RewardData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RewardData, newItem: RewardData): Boolean {
            return oldItem == newItem
        }
    }
}