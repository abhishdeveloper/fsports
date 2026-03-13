package com.college.sportsmeet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.college.sportsmeet.databinding.ActivityMatchLobbyBinding
import com.college.sportsmeet.models.MatchesResponse
import com.college.sportsmeet.network.ApiClient
import com.college.sportsmeet.repository.MatchRepository
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MatchLobbyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchLobbyBinding
    private lateinit var adapter: MatchAdapter

    // Manual Dependency Injection using Factory
    private val viewModel: MatchViewModel by viewModels {
        MatchViewModel.Factory(MatchRepository(ApiClient.apiService))
    }

    // Cache the latest successful response to easily switch tabs without refetching
    private var currentMatchesResponse: MatchesResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding prevents manual NullPointerExceptions on View Lookups
        binding = ActivityMatchLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupTabLayout()
        observeViewModel()
    }

    /**
     * Initializes the RecyclerView and its Adapter.
     */
    private fun setupRecyclerView() {
        adapter = MatchAdapter { selectedMatch ->
            // Handle clicking a match (e.g., Navigate to Match Details / Questions)
            Snackbar.make(binding.root, "Clicked ${selectedMatch.teamA.name} vs ${selectedMatch.teamB.name}", Snackbar.LENGTH_SHORT).show()
        }
        binding.recyclerViewMatches.adapter = adapter
    }

    /**
     * Handles switching between "Upcoming", "Live", and "Completed" tabs.
     */
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateListForSelectedTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Optionally scroll to top
                binding.recyclerViewMatches.smoothScrollToPosition(0)
            }
        })
    }

    /**
     * Safely collects the StateFlow from the ViewModel.
     * repeatOnLifecycle(STARTED) ensures we only process state changes when the UI is visible,
     * saving battery and preventing crashes.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    /**
     * Updates the UI based on the distinct Sealed Class state emitted.
     */
    private fun handleUiState(state: MatchUiState) {
        when (state) {
            is MatchUiState.Loading -> {
                binding.shimmerViewContainer.visibility = View.VISIBLE
                binding.shimmerViewContainer.startShimmer()
                binding.recyclerViewMatches.visibility = View.GONE
            }
            is MatchUiState.Success -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.recyclerViewMatches.visibility = View.VISIBLE

                // Cache the response and update the list for the currently selected tab
                currentMatchesResponse = state.data
                updateListForSelectedTab()
            }
            is MatchUiState.Error -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                // Optionally, show a retry button or an empty state view here

                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { viewModel.fetchMatches() }
                    .show()
            }
        }
    }

    /**
     * Filters the cached matches based on the currently selected TabLayout tab.
     */
    private fun updateListForSelectedTab() {
        val matches = currentMatchesResponse ?: return

        val selectedList = when (binding.tabLayout.selectedTabPosition) {
            0 -> matches.upcoming
            1 -> matches.live
            2 -> matches.completed
            else -> emptyList()
        }

        // Submits the new list to the DiffUtil adapter
        adapter.submitList(selectedList)
    }
}