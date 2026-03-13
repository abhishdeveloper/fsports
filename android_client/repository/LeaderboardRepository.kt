package com.college.sportsmeet.repository

import com.college.sportsmeet.models.LeaderboardResponse
import com.college.sportsmeet.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeaderboardRepository(private val apiService: ApiService) {

    suspend fun fetchTopLeaderboard(): Result<LeaderboardResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLeaderboard()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}