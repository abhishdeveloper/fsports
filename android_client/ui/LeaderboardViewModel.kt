package com.college.sportsmeet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.college.sportsmeet.models.LeaderboardUser
import com.college.sportsmeet.repository.LeaderboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LeaderboardUiState {
    object Loading : LeaderboardUiState()
    data class Success(val users: List<LeaderboardUser>) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
}

class LeaderboardViewModel(private val repository: LeaderboardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        _uiState.value = LeaderboardUiState.Loading

        viewModelScope.launch {
            val result = repository.fetchTopLeaderboard()

            result.onSuccess { response ->
                _uiState.value = LeaderboardUiState.Success(response.data)
            }.onFailure { exception ->
                _uiState.value = LeaderboardUiState.Error(
                    exception.localizedMessage ?: "Failed to load leaderboard."
                )
            }
        }
    }

    class Factory(private val repository: LeaderboardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
                return LeaderboardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}