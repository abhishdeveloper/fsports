package com.college.sportsmeet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.college.sportsmeet.databinding.ActivityResolveMatchBinding
import com.college.sportsmeet.models.ResolveMatchRequest
import com.college.sportsmeet.network.ApiClient
import com.college.sportsmeet.repository.MatchRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ResolveMatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResolveMatchBinding

    // We reuse the robust PredictionAdapter and ViewModel because visually,
    // an admin resolving questions looks identical to a user predicting them (answering questions).
    private lateinit var adapter: PredictionAdapter

    private val viewModel: PredictionViewModel by viewModels {
        PredictionViewModel.Factory(MatchRepository(ApiClient.apiService))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResolveMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val matchId = intent.getLongExtra("EXTRA_MATCH_ID", -1L)
        val matchName = intent.getStringExtra("EXTRA_MATCH_NAME") ?: "Match Resolution"

        if (matchId == -1L) {
            Snackbar.make(binding.root, "Invalid match ID.", Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar(matchName)
        setupRecyclerView()
        setupConfirmButton(matchId)

        observeQuestionsState(matchId)

        // Fetch questions initially
        viewModel.fetchQuestions(matchId)
    }

    private fun setupToolbar(title: String) {
        binding.topAppBar.title = "Resolve: $title"
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = PredictionAdapter()
        binding.recyclerViewResolve.adapter = adapter
    }

    private fun setupConfirmButton(matchId: Long) {
        binding.btnConfirmResolution.setOnClickListener {
            val questions = adapter.currentList
            val resolutionsMap = mutableMapOf<String, String>()

            // Ensure the admin has selected a "correct answer" for every question
            for (question in questions) {
                val selected = question.userSelectedOption
                if (selected == null) {
                    Snackbar.make(binding.root, "Please resolve all questions before submitting.", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.btnConfirmResolution)
                        .show()
                    return@setOnClickListener
                }
                resolutionsMap[question.questionId.toString()] = selected
            }

            // Build request and execute
            binding.btnConfirmResolution.isEnabled = false
            binding.btnConfirmResolution.text = "Resolving..."

            lifecycleScope.launch {
                val repo = MatchRepository(ApiClient.apiService)
                val result = repo.resolveMatch(ResolveMatchRequest(matchId, resolutionsMap)) // Note: Requires extending repo

                result.onSuccess {
                    binding.lottieSuccess.visibility = View.VISIBLE
                    binding.lottieSuccess.playAnimation()
                    delay(2000)
                    finish()
                }.onFailure { e ->
                    binding.btnConfirmResolution.isEnabled = true
                    binding.btnConfirmResolution.text = "Confirm Resolutions"
                    Snackbar.make(binding.root, e.localizedMessage ?: "Failed to resolve match", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.btnConfirmResolution).show()
                }
            }
        }
    }

    private fun observeQuestionsState(matchId: Long) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is QuestionsUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.recyclerViewResolve.visibility = View.GONE
                        }
                        is QuestionsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerViewResolve.visibility = View.VISIBLE
                            adapter.submitList(state.questions)
                        }
                        is QuestionsUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_INDEFINITE)
                                .setAction("Retry") { viewModel.fetchQuestions(matchId) }
                                .show()
                        }
                    }
                }
            }
        }
    }
}