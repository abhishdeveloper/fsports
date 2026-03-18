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
import android.media.MediaPlayer
import android.view.HapticFeedbackConstants
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
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

        binding.layoutError.btnRetry.setOnClickListener {
            viewModel.fetchQuestions(matchId)
        }

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
        binding.btnLockPredictions.setOnClickListener { view ->
            // Heavy Haptic Feedback
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            // Play custom click sound
            // val mediaPlayer = MediaPlayer.create(this, R.raw.custom_click)
            // mediaPlayer?.start()

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
                predictionRequests.add(PredictionRequest(
                    questionId = question.questionId,
                    selectedOption = selected,
                    isBoosted = question.isBoosted
                ))
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
                            binding.layoutError.root.visibility = View.GONE
                            binding.layoutEmpty.root.visibility = View.GONE
                            binding.recyclerViewQuestions.visibility = View.GONE

                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is QuestionsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.layoutError.root.visibility = View.GONE

                            if (state.questions.isEmpty()) {
                                binding.recyclerViewQuestions.visibility = View.GONE
                                binding.btnLockPredictions.visibility = View.GONE
                                binding.layoutEmpty.root.visibility = View.VISIBLE
                                binding.layoutEmpty.tvEmptyMessage.text = "No questions available for this match yet."
                            } else {
                                binding.layoutEmpty.root.visibility = View.GONE
                                binding.recyclerViewQuestions.visibility = View.VISIBLE
                                binding.btnLockPredictions.visibility = View.VISIBLE
                                adapter.submitList(state.questions)
                            }
                        }
                        is QuestionsUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerViewQuestions.visibility = View.GONE
                            binding.btnLockPredictions.visibility = View.GONE
                            binding.layoutEmpty.root.visibility = View.GONE

                            binding.layoutError.root.visibility = View.VISIBLE
                            binding.layoutError.tvErrorMessage.text = state.message
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
                            // Trigger Rivalry Stats update in the Adapter!
                            adapter.updateRivalryStats(state.rivalryStats)

                            // Execute premium success animation
                            binding.lottieSuccess.visibility = View.VISIBLE
                            binding.lottieSuccess.playAnimation()

                            // Konfetti Explosion
                            val party = Party(
                                speed = 0f,
                                maxSpeed = 30f,
                                damping = 0.9f,
                                spread = 360,
                                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                                position = Position.Relative(0.5, 0.3)
                            )
                            binding.konfettiView.start(party)

                            // Let the animations and rivalry stats display for 3.5 seconds before popping the backstack
                            delay(3500)
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