package com.college.sportsmeet.repository

import com.college.sportsmeet.models.GenericResponse
import com.college.sportsmeet.models.MatchesResponse
import com.college.sportsmeet.models.PredictionRequest
import com.college.sportsmeet.models.QuestionsResponse
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

    /**
     * Fetches questions for a given match.
     */
    suspend fun fetchQuestions(matchId: Long): Result<QuestionsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getQuestions(matchId)
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

    /**
     * Submits bulk predictions in a single transactional request.
     */
    suspend fun submitBulkPredictions(requests: List<PredictionRequest>): Result<GenericResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.submitBulkPredictions(requests)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    // Extract exact error msg like 403 or 409
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}