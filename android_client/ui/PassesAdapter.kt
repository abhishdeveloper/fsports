package com.college.sportsmeet.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.college.sportsmeet.databinding.ItemPassCardBinding
import com.college.sportsmeet.models.PassData

class PassesAdapter : ListAdapter<PassData, PassesAdapter.PassesViewHolder>(PassesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassesViewHolder {
        val binding = ItemPassCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PassesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PassesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PassesViewHolder(private val binding: ItemPassCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pass: PassData) {
            binding.apply {
                tvPassTitle.text = pass.title
                tvPassDescription.text = pass.description
                tvRedemptionCode.text = pass.redemptionCode

                tvPassStatus.text = pass.status.uppercase()

                // Desaturate 'claimed' passes to act as a receipt
                if (pass.status == "claimed") {
                    root.setCardBackgroundColor(Color.parseColor("#E0E0E0")) // Light Gray
                    tvRedemptionCode.alpha = 0.5f
                    tvPassStatus.setTextColor(Color.DKGRAY)
                } else {
                    // Active style defaults. Must explicitly reset due to RecyclerView recycling.
                    // To fetch the original theme colors dynamically, we resolve the attributes from Context
                    val context = root.context
                    val typedValue = android.util.TypedValue()

                    context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
                    root.setCardBackgroundColor(typedValue.data)

                    context.theme.resolveAttribute(com.google.android.material.R.attr.colorTertiary, typedValue, true)
                    tvPassStatus.setTextColor(typedValue.data)

                    tvRedemptionCode.alpha = 1.0f
                }
            }
        }
    }

    class PassesDiffCallback : DiffUtil.ItemCallback<PassData>() {
        override fun areItemsTheSame(oldItem: PassData, newItem: PassData): Boolean {
            return oldItem.redemptionCode == newItem.redemptionCode
        }

        override fun areContentsTheSame(oldItem: PassData, newItem: PassData): Boolean {
            return oldItem == newItem
        }
    }
}