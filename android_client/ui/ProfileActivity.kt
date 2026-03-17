package com.college.sportsmeet.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.college.sportsmeet.databinding.ActivityProfileBinding
import com.college.sportsmeet.models.ProfileResponse
import com.college.sportsmeet.network.ApiClient
import com.college.sportsmeet.repository.ProfileRepository
import com.college.sportsmeet.utils.TokenManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var adapter: HistoryAdapter

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(ProfileRepository(ApiClient.apiService))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupLogoutButton()
        observeViewModel()

        binding.layoutError.btnRetry.setOnClickListener {
            viewModel.fetchProfile()
        }
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter()
        binding.recyclerViewHistory.adapter = adapter
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            // Clear securely stored JWT and Role
            TokenManager.clearAuthData()

            // Route to Login and clear backstack
            val intent = Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
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

    private fun handleUiState(state: ProfileUiState) {
        when (state) {
            is ProfileUiState.Loading -> {
                binding.layoutError.root.visibility = View.GONE
                binding.layoutEmpty.root.visibility = View.GONE
                binding.recyclerViewHistory.visibility = View.GONE
                binding.cardProfileInfo.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            }
            is ProfileUiState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.layoutError.root.visibility = View.GONE

                populateProfileCard(state.data)

                if (state.data.history.isEmpty()) {
                    binding.recyclerViewHistory.visibility = View.GONE
                    binding.layoutEmpty.root.visibility = View.VISIBLE
                    binding.layoutEmpty.tvEmptyMessage.text = "You haven't made any predictions yet."
                } else {
                    binding.layoutEmpty.root.visibility = View.GONE
                    binding.recyclerViewHistory.visibility = View.VISIBLE
                    adapter.submitList(state.data.history)
                }
            }
            is ProfileUiState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewHistory.visibility = View.GONE
                binding.cardProfileInfo.visibility = View.GONE
                binding.layoutEmpty.root.visibility = View.GONE

                binding.layoutError.root.visibility = View.VISIBLE
                binding.layoutError.tvErrorMessage.text = state.message
            }
        }
    }

    private fun populateProfileCard(data: ProfileResponse) {
        binding.cardProfileInfo.visibility = View.VISIBLE
        binding.tvUsername.text = data.profile.username
        binding.chipBranch.text = data.profile.branch
        binding.tvTotalCoins.text = data.profile.totalCoins.toString()
        binding.tvRank.text = "#${data.profile.globalRank}"
    }
}