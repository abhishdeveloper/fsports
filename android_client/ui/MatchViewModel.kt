package com.college.sportsmeet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.college.sportsmeet.models.MatchesResponse
import com.college.sportsmeet.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Defines the distinct UI states for the Match Lobby.
 * Utilizing Kotlin Sealed Classes ensures exhaustive handling.
 */
sealed class MatchUiState {
    object Loading : MatchUiState()
    data class Success(val data: MatchesResponse) : MatchUiState()
    data class Error(val message: String) : MatchUiState()
}

/**
 * Manages the logic, state, and API requests for the Match Lobby UI.
 * Survives configuration changes implicitly.
 */
class MatchViewModel(private val matchRepository: MatchRepository) : ViewModel() {

    // Backing property representing the state
    private val _uiState = MutableStateFlow<MatchUiState>(MatchUiState.Loading)

    // Exposed immutable StateFlow for the UI to observe safely
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    init {
        // Fetch matches immediately when the ViewModel is created
        fetchMatches()
    }

    /**
     * Executes the repository call asynchronously using the ViewModel's CoroutineScope.
     */
    fun fetchMatches() {
        // Only show loading if we are fetching for the first time or manually refreshing
        _uiState.value = MatchUiState.Loading

        viewModelScope.launch {
            val result = matchRepository.fetchMatches()

            result.onSuccess { matches ->
                _uiState.value = MatchUiState.Success(matches)
            }.onFailure { exception ->
                // Provide a user-friendly error string instead of raw stack traces
                _uiState.value = MatchUiState.Error(
                    exception.localizedMessage ?: "Failed to fetch matches. Please try again later."
                )
            }
        }
    }

    /**
     * Factory instance for instantiating MatchViewModel with its required Repository dependency.
     * Prevents the need for complex DI frameworks for simpler architectural flows.
     */
    class Factory(private val matchRepository: MatchRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MatchViewModel::class.java)) {
                return MatchViewModel(matchRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}