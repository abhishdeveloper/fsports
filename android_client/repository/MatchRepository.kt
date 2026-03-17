package com.college.sportsmeet.repository

import com.college.sportsmeet.models.CreateMatchRequest
import com.college.sportsmeet.models.GenericResponse
import com.college.sportsmeet.models.MatchesResponse
import com.college.sportsmeet.models.PredictionRequest
import com.college.sportsmeet.models.QuestionsResponse
import com.college.sportsmeet.models.SportsResponse
import com.college.sportsmeet.models.TeamsResponse
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
     * Admin: Fetches sports list for dropdowns.
     */
    suspend fun fetchSports(): Result<SportsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSports()
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
     * Admin: Fetches teams list for dropdowns.
     */
    suspend fun fetchTeams(): Result<TeamsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTeams()
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
     * Admin: Creates a new match.
     */
    suspend fun createMatch(request: CreateMatchRequest): Result<GenericResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createMatch(request)
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
     * Admin: Updates match status dynamically.
     */
    suspend fun updateMatchStatus(request: com.college.sportsmeet.models.UpdateStatusRequest): Result<GenericResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateMatchStatus(request)
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
     * Admin: Resolves match by passing the map of correct answers securely.
     */
    suspend fun resolveMatch(request: com.college.sportsmeet.models.ResolveMatchRequest): Result<GenericResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.resolveMatch(request)
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