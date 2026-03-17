package com.college.sportsmeet.repository

import com.college.sportsmeet.models.MyPassesResponse
import com.college.sportsmeet.models.RedeemRequest
import com.college.sportsmeet.models.RedeemResponse
import com.college.sportsmeet.models.RewardsResponse
import com.college.sportsmeet.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RewardsRepository(private val apiService: ApiService) {

    suspend fun getAvailableRewards(): Result<RewardsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRewards()
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

    suspend fun redeemReward(rewardId: Long): Result<RedeemResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.redeemReward(RedeemRequest(rewardId))
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

    suspend fun getMyPasses(): Result<MyPassesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMyPasses()
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