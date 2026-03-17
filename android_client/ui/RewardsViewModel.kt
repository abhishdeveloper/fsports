package com.college.sportsmeet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.college.sportsmeet.models.PassData
import com.college.sportsmeet.models.RewardData
import com.college.sportsmeet.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RewardsUiState {
    object Loading : RewardsUiState()
    data class SuccessRewards(val rewards: List<RewardData>) : RewardsUiState()
    data class SuccessPasses(val passes: List<PassData>) : RewardsUiState()
    data class Error(val message: String) : RewardsUiState()
}

sealed class RedemptionState {
    object Idle : RedemptionState()
    object Submitting : RedemptionState()
    data class Success(val code: String) : RedemptionState()
    data class Error(val message: String) : RedemptionState()
}

class RewardsViewModel(private val repository: RewardsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RewardsUiState>(RewardsUiState.Loading)
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    private val _redemptionState = MutableSharedFlow<RedemptionState>()
    val redemptionState: SharedFlow<RedemptionState> = _redemptionState.asSharedFlow()

    fun fetchRewards() {
        _uiState.value = RewardsUiState.Loading
        viewModelScope.launch {
            repository.getAvailableRewards().onSuccess { response ->
                _uiState.value = RewardsUiState.SuccessRewards(response.data)
            }.onFailure { exception ->
                _uiState.value = RewardsUiState.Error(exception.localizedMessage ?: "Failed to load rewards.")
            }
        }
    }

    fun fetchMyPasses() {
        _uiState.value = RewardsUiState.Loading
        viewModelScope.launch {
            repository.getMyPasses().onSuccess { response ->
                _uiState.value = RewardsUiState.SuccessPasses(response.data)
            }.onFailure { exception ->
                _uiState.value = RewardsUiState.Error(exception.localizedMessage ?: "Failed to load passes.")
            }
        }
    }

    fun redeemReward(rewardId: Long) {
        viewModelScope.launch {
            _redemptionState.emit(RedemptionState.Submitting)
            repository.redeemReward(rewardId).onSuccess { response ->
                _redemptionState.emit(RedemptionState.Success(response.redemptionCode ?: ""))
            }.onFailure { exception ->
                _redemptionState.emit(RedemptionState.Error(exception.localizedMessage ?: "Redemption failed."))
            }
        }
    }

    class Factory(private val repository: RewardsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RewardsViewModel::class.java)) {
                return RewardsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}