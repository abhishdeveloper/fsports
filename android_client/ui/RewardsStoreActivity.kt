package com.college.sportsmeet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.college.sportsmeet.databinding.ActivityRewardsStoreBinding
import com.college.sportsmeet.models.RewardData
import com.college.sportsmeet.network.ApiClient
import com.college.sportsmeet.repository.RewardsRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RewardsStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsStoreBinding
    private lateinit var rewardsAdapter: RewardsAdapter
    private lateinit var passesAdapter: PassesAdapter

    private val viewModel: RewardsViewModel by viewModels {
        RewardsViewModel.Factory(RewardsRepository(ApiClient.apiService))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupTabLayout()

        observeViewModel()

        binding.layoutError.btnRetry.setOnClickListener {
            loadCurrentTab()
        }

        // Initially load 'Available Perks'
        viewModel.fetchRewards()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerViews() {
        // Initialize both adapters
        rewardsAdapter = RewardsAdapter { reward ->
            showConfirmRedeemDialog(reward)
        }

        passesAdapter = PassesAdapter()

        // Default to Rewards
        binding.recyclerViewRewards.adapter = rewardsAdapter
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                loadCurrentTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadCurrentTab() {
        if (binding.tabLayout.selectedTabPosition == 0) {
            binding.recyclerViewRewards.adapter = rewardsAdapter
            viewModel.fetchRewards()
        } else {
            binding.recyclerViewRewards.adapter = passesAdapter
            viewModel.fetchMyPasses()
        }
    }

    private fun showConfirmRedeemDialog(reward: RewardData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Redeem ${reward.title}?")
            .setMessage("This will deduct ${reward.cost} coins from your balance. This action cannot be undone.")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Confirm") { dialog, _ ->
                viewModel.redeemReward(reward.id)
                dialog.dismiss()
            }
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                launch {
                    viewModel.redemptionState.collect { state ->
                        handleRedemptionState(state)
                    }
                }
            }
        }
    }

    private fun handleUiState(state: RewardsUiState) {
        when (state) {
            is RewardsUiState.Loading -> {
                binding.layoutError.root.visibility = View.GONE
                binding.layoutEmpty.root.visibility = View.GONE
                binding.recyclerViewRewards.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            }
            is RewardsUiState.SuccessRewards -> {
                binding.progressBar.visibility = View.GONE
                binding.layoutError.root.visibility = View.GONE

                if (state.rewards.isEmpty()) {
                    binding.recyclerViewRewards.visibility = View.GONE
                    binding.layoutEmpty.root.visibility = View.VISIBLE
                    binding.layoutEmpty.tvEmptyMessage.text = "No perks available right now."
                } else {
                    binding.layoutEmpty.root.visibility = View.GONE
                    binding.recyclerViewRewards.visibility = View.VISIBLE
                    rewardsAdapter.submitList(state.rewards)
                }
            }
            is RewardsUiState.SuccessPasses -> {
                binding.progressBar.visibility = View.GONE
                binding.layoutError.root.visibility = View.GONE

                if (state.passes.isEmpty()) {
                    binding.recyclerViewRewards.visibility = View.GONE
                    binding.layoutEmpty.root.visibility = View.VISIBLE
                    binding.layoutEmpty.tvEmptyMessage.text = "You haven't claimed any passes yet."
                } else {
                    binding.layoutEmpty.root.visibility = View.GONE
                    binding.recyclerViewRewards.visibility = View.VISIBLE
                    passesAdapter.submitList(state.passes)
                }
            }
            is RewardsUiState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewRewards.visibility = View.GONE
                binding.layoutEmpty.root.visibility = View.GONE

                binding.layoutError.root.visibility = View.VISIBLE
                binding.layoutError.tvErrorMessage.text = state.message
            }
        }
    }

    private fun handleRedemptionState(state: RedemptionState) {
        when (state) {
            is RedemptionState.Submitting -> {
                binding.progressBar.visibility = View.VISIBLE
            }
            is RedemptionState.Success -> {
                binding.progressBar.visibility = View.GONE

                // Play animation
                binding.lottieSuccess.visibility = View.VISIBLE
                binding.lottieSuccess.playAnimation()

                Snackbar.make(binding.root, "Code: ${state.code} generated!", Snackbar.LENGTH_LONG).show()

                lifecycleScope.launch {
                    delay(2500)
                    binding.lottieSuccess.visibility = View.GONE

                    // Switch to 'My Passes' tab to view the new ticket
                    binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
                }
            }
            is RedemptionState.Error -> {
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is RedemptionState.Idle -> {}
        }
    }
}