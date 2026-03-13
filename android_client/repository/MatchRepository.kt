package com.college.sportsmeet.repository

import com.college.sportsmeet.models.MatchesResponse
import com.college.sportsmeet.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository to abstract the network layer and handle API responses cleanly.
 */
class MatchRepository(private val apiService: ApiService) {

    /**
     * Fetches the matches from the backend.
     * Uses withContext(Dispatchers.IO) to ensure it runs off the main thread.
     *
     * @return Result containing either the successful MatchesResponse or an Exception.
     */
    suspend fun fetchMatches(): Result<MatchesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMatches()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    // Extract custom error message from JSON if possible, otherwise use HTTP message
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                // Catches network errors, timeouts, etc.
                Result.failure(e)
            }
        }
    }
}