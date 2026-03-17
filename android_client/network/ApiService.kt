package com.college.sportsmeet.network

import com.college.sportsmeet.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit Interface defining the PHP backend endpoints we've constructed.
 * Utilizing Kotlin coroutines 'suspend' modifier for asynchronous operations.
 */
interface ApiService {

    /**
     * Authenticates a user and returns a JSON response containing the Access Token.
     */
    @POST("Api/Auth/login.php")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Registers a new user. Expects a 201 Created or 409 Conflict.
     */
    @POST("Api/Auth/register.php")
    suspend fun register(@Body request: RegisterRequest): Response<GenericResponse>

    /**
     * Fetches all non-cancelled matches grouped by status (upcoming, live, completed).
     * Automatically handles the Bearer token via the AuthInterceptor.
     */
    @GET("Api/Matches/list.php")
    suspend fun getMatches(): Response<MatchesResponse>

    /**
     * Fetches questions for a specific match ID.
     * The correct_option column is securely excluded by the backend.
     */
    @GET("Api/Matches/questions.php")
    suspend fun getQuestions(@Query("match_id") matchId: Long): Response<QuestionsResponse>

    /**
     * Submits a single prediction for a question.
     * Validates on the backend if the match is still upcoming and the user hasn't already bet.
     */
    @POST("Api/Predictions/submit.php")
    suspend fun submitPrediction(@Body request: PredictionRequest): Response<GenericResponse>

    /**
     * Submits a batch of predictions securely wrapped in a backend PDO transaction.
     */
    @POST("Api/Predictions/submit_bulk.php")
    suspend fun submitBulkPredictions(@Body requests: List<PredictionRequest>): Response<com.college.sportsmeet.models.SubmitPredictionResponse>

    /**
     * Fetches the top 50 users ranked by total coins.
     */
    @GET("Api/Leaderboard/top.php")
    suspend fun getLeaderboard(): Response<LeaderboardResponse>

    // --- Admin Endpoints ---

    @GET("Api/Matches/sports.php")
    suspend fun getSports(): Response<SportsResponse>

    @GET("Api/Matches/teams.php")
    suspend fun getTeams(): Response<TeamsResponse>

    @POST("Api/Rewards/daily_claim.php")
    suspend fun claimDailyReward(): Response<com.college.sportsmeet.models.DailyClaimResponse>

    @GET("Api/Users/history.php")
    suspend fun getProfileAndHistory(): Response<com.college.sportsmeet.models.ProfileResponse>

    @POST("Api/Admin/create_match.php")
    suspend fun createMatch(@Body request: CreateMatchRequest): Response<GenericResponse>

    @POST("Api/Admin/update_status.php")
    suspend fun updateMatchStatus(@Body request: UpdateStatusRequest): Response<GenericResponse>

    @POST("Api/Admin/resolve_match.php")
    suspend fun resolveMatch(@Body request: ResolveMatchRequest): Response<GenericResponse>

    // --- Campus Economy Endpoints (Phase 12) ---
    @GET("Api/Store/list.php")
    suspend fun getRewards(): Response<com.college.sportsmeet.models.RewardsResponse>

    @POST("Api/Store/redeem.php")
    suspend fun redeemReward(@Body request: com.college.sportsmeet.models.RedeemRequest): Response<com.college.sportsmeet.models.RedeemResponse>

    @GET("Api/Store/my_passes.php")
    suspend fun getMyPasses(): Response<com.college.sportsmeet.models.MyPassesResponse>

    @POST("Api/Admin/claim_reward.php")
    suspend fun adminClaimReward(@Body request: com.college.sportsmeet.models.ClaimRewardRequest): Response<GenericResponse>
}