package com.college.sportsmeet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.college.sportsmeet.databinding.ActivityPredictionBinding
import com.college.sportsmeet.models.PredictionRequest
import com.college.sportsmeet.network.ApiClient
import com.college.sportsmeet.repository.MatchRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PredictionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPredictionBinding
    private lateinit var adapter: PredictionAdapter

    private val viewModel: PredictionViewModel by viewModels {
        PredictionViewModel.Factory(MatchRepository(ApiClient.apiService))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPredictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve intent extras passed from MatchLobbyActivity
        val matchId = intent.getLongExtra("EXTRA_MATCH_ID", -1L)
        val matchName = intent.getStringExtra("EXTRA_MATCH_NAME") ?: "Match Predictions"

        if (matchId == -1L) {
            Snackbar.make(binding.root, "Invalid match ID.", Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar(matchName)
        setupRecyclerView()
        setupLockPredictionsButton()

        observeQuestionsState()
        observeSubmissionState()

        // Fetch questions initially
        viewModel.fetchQuestions(matchId)
    }

    private fun setupToolbar(title: String) {
        binding.topAppBar.title = title
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = PredictionAdapter()
        binding.recyclerViewQuestions.adapter = adapter
    }

    private fun setupLockPredictionsButton() {
        binding.btnLockPredictions.setOnClickListener {
            val questions = adapter.currentList
            val predictionRequests = mutableListOf<PredictionRequest>()

            // Ensure every question has been answered before proceeding
            for (question in questions) {
                val selected = question.userSelectedOption
                if (selected == null) {
                    Snackbar.make(binding.root, "Please answer all questions before locking.", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.btnLockPredictions) // Anchor above the button
                        .show()
                    return@setOnClickListener
                }
                predictionRequests.add(PredictionRequest(question.questionId, selected))
            }

            // Fire bulk submission transaction
            viewModel.submitPredictions(predictionRequests)
        }
    }

    private fun observeQuestionsState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is QuestionsUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.recyclerViewQuestions.visibility = View.GONE
                        }
                        is QuestionsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerViewQuestions.visibility = View.VISIBLE
                            adapter.submitList(state.questions)
                        }
                        is QuestionsUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_INDEFINITE)
                                .setAction("Retry") {
                                    val matchId = intent.getLongExtra("EXTRA_MATCH_ID", -1L)
                                    viewModel.fetchQuestions(matchId)
                                }
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun observeSubmissionState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect SharedFlow to handle one-off submission events (no resubmission on device rotation)
                viewModel.submissionState.collect { state ->
                    when (state) {
                        is SubmissionState.Submitting -> {
                            binding.btnLockPredictions.isEnabled = false
                            binding.btnLockPredictions.text = "Locking..."
                        }
                        is SubmissionState.Success -> {
                            // Execute premium success animation
                            binding.lottieSuccess.visibility = View.VISIBLE
                            binding.lottieSuccess.playAnimation()

                            // Delay for 2 seconds to let the animation play before popping the backstack
                            delay(2000)
                            finish()
                        }
                        is SubmissionState.Error -> {
                            binding.btnLockPredictions.isEnabled = true
                            binding.btnLockPredictions.text = "Lock Predictions"

                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                                .setAnchorView(binding.btnLockPredictions)
                                .show()
                        }
                        is SubmissionState.Idle -> { /* Initial state, do nothing */ }
                    }
                }
            }
        }
    }
}