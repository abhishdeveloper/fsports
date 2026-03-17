package com.college.sportsmeet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.college.sportsmeet.models.PredictionRequest
import com.college.sportsmeet.models.QuestionData
import com.college.sportsmeet.repository.MatchRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QuestionsUiState {
    object Loading : QuestionsUiState()
    data class Success(val questions: List<QuestionData>) : QuestionsUiState()
    data class Error(val message: String) : QuestionsUiState()
}

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Submitting : SubmissionState()
    data class Success(val rivalryStats: Map<String, com.college.sportsmeet.models.RivalryStat>?) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}

class PredictionViewModel(private val matchRepository: MatchRepository) : ViewModel() {

    // StateFlow for fetching and displaying the questions list
    private val _uiState = MutableStateFlow<QuestionsUiState>(QuestionsUiState.Loading)
    val uiState: StateFlow<QuestionsUiState> = _uiState.asStateFlow()

    // SharedFlow for one-time submission events (so rotating device doesn't resubmit)
    private val _submissionState = MutableSharedFlow<SubmissionState>()
    val submissionState: SharedFlow<SubmissionState> = _submissionState.asSharedFlow()

    fun fetchQuestions(matchId: Long) {
        _uiState.value = QuestionsUiState.Loading

        viewModelScope.launch {
            val result = matchRepository.fetchQuestions(matchId)

            result.onSuccess { response ->
                _uiState.value = QuestionsUiState.Success(response.data)
            }.onFailure { exception ->
                _uiState.value = QuestionsUiState.Error(
                    exception.localizedMessage ?: "Failed to load questions."
                )
            }
        }
    }

    /**
     * Submits the gathered predictions from the adapter to the backend.
     */
    fun submitPredictions(predictions: List<PredictionRequest>) {
        if (predictions.isEmpty()) return

        viewModelScope.launch {
            _submissionState.emit(SubmissionState.Submitting)

            val result = matchRepository.submitBulkPredictions(predictions)

            result.onSuccess { response ->
                _submissionState.emit(SubmissionState.Success(response.rivalryStats))
            }.onFailure { exception ->
                _submissionState.emit(
                    SubmissionState.Error(exception.localizedMessage ?: "Submission failed.")
                )
            }
        }
    }

    class Factory(private val matchRepository: MatchRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PredictionViewModel::class.java)) {
                return PredictionViewModel(matchRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}