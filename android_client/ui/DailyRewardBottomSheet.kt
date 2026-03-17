package com.college.sportsmeet.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.college.sportsmeet.databinding.FragmentDailyRewardBinding
import com.college.sportsmeet.network.ApiClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.college.sportsmeet.models.DailyClaimResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DailyRewardBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentDailyRewardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyRewardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClaimReward.setOnClickListener {
            claimReward()
        }
    }

    private fun claimReward() {
        binding.btnClaimReward.isEnabled = false
        binding.btnClaimReward.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.claimDailyReward()

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    showSuccess(data)
                } else if (response.code() == 403) {
                    // Already claimed
                    val errorBodyStr = response.errorBody()?.string()
                    val errorData = Gson().fromJson(errorBodyStr, DailyClaimResponse::class.java)

                    binding.lottieChest.cancelAnimation()
                    binding.tvRewardTitle.text = "Come back tomorrow!"
                    binding.tvRewardMessage.text = "You've already claimed today's reward.\nAvailable in ${errorData.hoursLeft} hours."
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Snackbar.make(binding.root, "Error: $errorMsg", Snackbar.LENGTH_LONG).show()
                    binding.btnClaimReward.isEnabled = true
                    binding.btnClaimReward.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnClaimReward.isEnabled = true
                binding.btnClaimReward.visibility = View.VISIBLE
                Snackbar.make(binding.root, "Network Error: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun showSuccess(data: DailyClaimResponse) {
        binding.tvRewardTitle.text = "+${data.rewardCoins} Coins!"
        binding.tvRewardMessage.text = data.message

        binding.streakContainer.visibility = View.VISIBLE
        binding.tvStreakCount.text = "🔥 ${data.currentStreak} Day Streak!"

        // Animate progress bar to the current streak percentage
        val progressPercent = ((data.currentStreak.toFloat() / data.maxStreak.toFloat()) * 100).toInt()
        binding.streakProgressBar.setProgressCompat(progressPercent, true)

        binding.lottieChest.playAnimation() // Triggers the success animation

        delay(3000)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DailyRewardBottomSheet"
    }
}