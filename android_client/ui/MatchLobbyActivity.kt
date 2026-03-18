package com.college.sportsmeet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.college.sportsmeet.databinding.ActivityMatchLobbyBinding
import android.content.Intent
import com.college.sportsmeet.models.MatchesResponse
import com.college.sportsmeet.network.ApiClient
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.college.sportsmeet.repository.MatchRepository
import com.college.sportsmeet.utils.TokenManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MatchLobbyActivity : AppCompatActivity() {

    // Registering the permission launcher for Android 13+ Push Notifications
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            subscribeToGlobalMatchAlerts()
            syncFcmToken()
        } else {
            // User denied push notifications. We can show an educational Snackbar.
            Snackbar.make(binding.root, "You will miss live match alerts!", Snackbar.LENGTH_LONG).show()
        }
    }


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

        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        observeViewModel()
        checkAdminAccess()
        checkDailyReward()
        setupErrorRetry()
        askNotificationPermission()
    }

    /**
     * Phase 14: Android 13 (API 33) Runtime Permissions for POST_NOTIFICATIONS
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                    subscribeToGlobalMatchAlerts()
                    syncFcmToken()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show educational UI before asking again
                    Snackbar.make(binding.root, "Enable notifications to know when matches go LIVE!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Allow") {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }.show()
                }
                else -> {
                    // Ask directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 and below automatically grant permission at install time
            subscribeToGlobalMatchAlerts()
            syncFcmToken()
        }
    }

    /**
     * Subscribes the user to the global topic so they receive the 'Match is Live!' alerts.
     */
    private fun subscribeToGlobalMatchAlerts() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Log failure silently.
                }
            }
    }

    /**
     * Fetches the current FCM token manually (in case onNewToken missed it) and syncs with the PHP backend.
     */
    private fun syncFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val token = task.result
            if (token != null && TokenManager.getAccessToken() != null) {
                lifecycleScope.launch {
                    try {
                        ApiClient.apiService.updateFcmToken(com.college.sportsmeet.models.UpdateTokenRequest(token))
                    } catch (e: Exception) {
                        // Silent fail
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // We use a raw integer ID from the dynamically generated R.id (via view binding)
                // In actual Android Studio, R.id.action_profile is available. For raw compiling:
                else -> {
                    if (menuItem.title == "Rewards Store") {
                        startActivity(Intent(this, RewardsStoreActivity::class.java))
                        true
                    } else if (menuItem.title == "Profile") {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        true
                    } else false
                }
            }
        }
    }

    private fun setupErrorRetry() {
        binding.layoutError.btnRetry.setOnClickListener {
            viewModel.fetchMatches()
        }
    }

    /**
     * Checks locally if the Daily Reward Bottom Sheet should be shown today.
     * Prevents it from popping up every time the user navigates back to the lobby.
     */
    private fun checkDailyReward() {
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val lastClaimShown = sharedPrefs.getLong("last_reward_shown_time", 0)
        val currentTime = System.currentTimeMillis()

        // 24 hours in milliseconds = 86400000
        if (currentTime - lastClaimShown > 86400000) {
            val bottomSheet = DailyRewardBottomSheet()
            bottomSheet.show(supportFragmentManager, DailyRewardBottomSheet.TAG)

            sharedPrefs.edit().putLong("last_reward_shown_time", currentTime).apply()
        }
    }

    /**
     * Role-Based Access Control: Reveals the Admin Command Center FAB if role is 'admin'.
     */
    private fun checkAdminAccess() {
        val role = TokenManager.getUserRole()
        if (role == "admin") {
            binding.fabAdmin.visibility = View.VISIBLE
            binding.fabAdmin.setOnClickListener {
                // Navigate to Admin Dashboard (Placeholder for now)
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            }
        } else {
            binding.fabAdmin.visibility = View.GONE
        }
    }

    /**
     * Initializes the RecyclerView and its Adapter.
     */
    private fun setupRecyclerView() {
        adapter = MatchAdapter { selectedMatch ->
            // Match Routing: Navigate directly to PredictionActivity
            val intent = Intent(this, PredictionActivity::class.java).apply {
                putExtra("EXTRA_MATCH_ID", selectedMatch.id)
                putExtra("EXTRA_MATCH_NAME", "${selectedMatch.teamA.name} vs ${selectedMatch.teamB.name}")
            }
            startActivity(intent)
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
                binding.layoutError.root.visibility = View.GONE
                binding.layoutEmpty.root.visibility = View.GONE
                binding.recyclerViewMatches.visibility = View.GONE

                binding.shimmerViewContainer.visibility = View.VISIBLE
                binding.shimmerViewContainer.startShimmer()
            }
            is MatchUiState.Success -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.layoutError.root.visibility = View.GONE

                // Cache the response and update the list for the currently selected tab
                currentMatchesResponse = state.data
                updateListForSelectedTab()
            }
            is MatchUiState.Error -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                binding.recyclerViewMatches.visibility = View.GONE
                binding.layoutEmpty.root.visibility = View.GONE

                binding.layoutError.root.visibility = View.VISIBLE
                binding.layoutError.tvErrorMessage.text = state.message
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

        if (selectedList.isEmpty()) {
            binding.recyclerViewMatches.visibility = View.GONE
            binding.layoutEmpty.root.visibility = View.VISIBLE
        } else {
            binding.layoutEmpty.root.visibility = View.GONE
            binding.recyclerViewMatches.visibility = View.VISIBLE
            adapter.submitList(selectedList)
        }
    }
}