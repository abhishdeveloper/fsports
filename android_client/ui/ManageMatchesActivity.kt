package com.college.sportsmeet.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.college.sportsmeet.databinding.ActivityManageMatchesBinding
import com.college.sportsmeet.models.MatchData
import com.college.sportsmeet.models.UpdateStatusRequest
import com.college.sportsmeet.network.ApiClient
import com.college.sportsmeet.repository.MatchRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ManageMatchesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageMatchesBinding
    private lateinit var adapter: MatchAdapter

    private val viewModel: MatchViewModel by viewModels {
        MatchViewModel.Factory(MatchRepository(ApiClient.apiService))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageMatchesBinding.inflate(layoutInflater)
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
        // We reuse MatchAdapter from Phase 5, overriding the click behavior
        adapter = MatchAdapter { selectedMatch ->
            showAdminActionDialog(selectedMatch)
        }
        binding.recyclerViewManageMatches.adapter = adapter
    }

    private fun showAdminActionDialog(match: MatchData) {
        val options = arrayOf("Mark as Live", "Resolve Match", "Cancel Match")

        MaterialAlertDialogBuilder(this)
            .setTitle("Manage: ${match.teamA.name} vs ${match.teamB.name}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> updateMatchStatus(match.id, "live")
                    1 -> navigateToResolveMatch(match)
                    2 -> updateMatchStatus(match.id, "cancelled")
                }
            }
            .show()
    }

    private fun updateMatchStatus(matchId: Long, newStatus: String) {
        lifecycleScope.launch {
            val repo = MatchRepository(ApiClient.apiService)
            val result = repo.updateMatchStatus(UpdateStatusRequest(matchId, newStatus)) // Note: Requires adding this to Repo

            result.onSuccess {
                Snackbar.make(binding.root, "Status updated to $newStatus", Snackbar.LENGTH_SHORT).show()
                // Refresh list
                viewModel.fetchMatches()
            }.onFailure { e ->
                Snackbar.make(binding.root, e.localizedMessage ?: "Failed to update status", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToResolveMatch(match: MatchData) {
        val intent = Intent(this, ResolveMatchActivity::class.java).apply {
            putExtra("EXTRA_MATCH_ID", match.id)
            putExtra("EXTRA_MATCH_NAME", "${match.teamA.name} vs ${match.teamB.name}")
        }
        startActivity(intent)
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

    private fun handleUiState(state: MatchUiState) {
        when (state) {
            is MatchUiState.Loading -> {
                binding.shimmerViewContainer.visibility = View.VISIBLE
                binding.shimmerViewContainer.startShimmer()
                binding.recyclerViewManageMatches.visibility = View.GONE
            }
            is MatchUiState.Success -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.recyclerViewManageMatches.visibility = View.VISIBLE

                // For admin view, display all matches in a single list
                val allMatches = state.data.upcoming + state.data.live + state.data.completed
                adapter.submitList(allMatches.sortedBy { it.startTime })
            }
            is MatchUiState.Error -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { viewModel.fetchMatches() }
                    .show()
            }
        }
    }
}