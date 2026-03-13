package com.college.sportsmeet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.college.sportsmeet.databinding.ActivityLeaderboardBinding
import com.college.sportsmeet.network.ApiClient
import com.college.sportsmeet.repository.LeaderboardRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private lateinit var adapter: LeaderboardAdapter

    private val viewModel: LeaderboardViewModel by viewModels {
        LeaderboardViewModel.Factory(LeaderboardRepository(ApiClient.apiService))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = LeaderboardAdapter()
        binding.recyclerViewLeaderboard.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: LeaderboardUiState) {
        when (state) {
            is LeaderboardUiState.Loading -> {
                binding.shimmerViewContainer.visibility = View.VISIBLE
                binding.shimmerViewContainer.startShimmer()
                binding.recyclerViewLeaderboard.visibility = View.GONE
            }
            is LeaderboardUiState.Success -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.recyclerViewLeaderboard.visibility = View.VISIBLE
                adapter.submitList(state.users)
            }
            is LeaderboardUiState.Error -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE

                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { viewModel.fetchLeaderboard() }
                    .show()
            }
        }
    }
}