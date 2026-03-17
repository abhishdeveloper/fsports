package com.college.sportsmeet.models

import com.google.gson.annotations.SerializedName

/**
 * Common network models based on PHP backend responses
 */

// Generic Success/Error Response (e.g., from register.php or submit.php)
data class GenericResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)

// Daily Claim Response
data class DailyClaimResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("reward_coins") val rewardCoins: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("max_streak") val maxStreak: Int,
    @SerializedName("hours_left") val hoursLeft: Double?, // Exists if 403
    @SerializedName("error") val error: String?
)

// Auth Login Request & Response (login.php)
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
    @SerializedName("role") val role: String?,
    @SerializedName("error") val error: String?
)

// Auth Registration Request (register.php)
data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("roll_number") val rollNumber: String,
    @SerializedName("branch") val branch: String,
    @SerializedName("password") val password: String
)

// Matches List Response (list.php)
data class MatchesResponse(
    @SerializedName("upcoming") val upcoming: List<MatchData>,
    @SerializedName("live") val live: List<MatchData>,
    @SerializedName("completed") val completed: List<MatchData>
)

data class MatchData(
    @SerializedName("id") val id: Long,
    @SerializedName("sport") val sport: String,
    @SerializedName("status") val status: String,
    @SerializedName("team_a") val teamA: TeamData,
    @SerializedName("team_b") val teamB: TeamData,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("total_predictions") val totalPredictions: Int
)

data class TeamData(
    @SerializedName("name") val name: String,
    @SerializedName("branch") val branch: String?
)

// Questions Fetch Response (questions.php)
data class QuestionsResponse(
    @SerializedName("data") val data: List<QuestionData>
)

data class QuestionData(
    @SerializedName("question_id") val questionId: Long,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("option_a") val optionA: String,
    @SerializedName("option_b") val optionB: String,
    @SerializedName("option_c") val optionC: String?,
    @SerializedName("option_d") val optionD: String?,
    @SerializedName("points_multiplier") val pointsMultiplier: Double,

    // Mutable property to track user selection in the RecyclerView.
    // @Transient ensures Gson completely ignores it during serialization/deserialization.
    @Transient var userSelectedOption: String? = null
)

// Prediction Submission Request (submit.php)
data class PredictionRequest(
    @SerializedName("question_id") val questionId: Long,
    @SerializedName("selected_option") val selectedOption: String // Expected values: "A", "B", "C", "D"
)

data class SubmitPredictionResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("rivalry_stats") val rivalryStats: Map<String, RivalryStat>?
)

data class RivalryStat(
    @SerializedName("A") val optA: Int,
    @SerializedName("B") val optB: Int,
    @SerializedName("C") val optC: Int,
    @SerializedName("D") val optD: Int
)

// Leaderboard Top Request (top.php)
data class LeaderboardResponse(
    @SerializedName("data") val data: List<LeaderboardUser>
)

data class LeaderboardUser(
    @SerializedName("rank") val rank: Int,
    @SerializedName("username") val username: String,
    @SerializedName("branch") val branch: String,
    @SerializedName("total_coins") val totalCoins: Long
)

// Profile & History Models (history.php)
data class ProfileResponse(
    @SerializedName("profile") val profile: UserProfile,
    @SerializedName("history") val history: List<PredictionHistoryItem>
)

data class UserProfile(
    @SerializedName("username") val username: String,
    @SerializedName("branch") val branch: String,
    @SerializedName("total_coins") val totalCoins: Long,
    @SerializedName("global_rank") val globalRank: Int
)

data class PredictionHistoryItem(
    @SerializedName("prediction_id") val predictionId: Long,
    @SerializedName("match_name") val matchName: String,
    @SerializedName("sport_name") val sportName: String,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("selected_answer") val selectedAnswer: String?,
    @SerializedName("correct_answer") val correctAnswer: String?, // Nullable: Might be pending
    @SerializedName("status") val status: String, // "pending", "won", "lost"
    @SerializedName("points_multiplier") val pointsMultiplier: Double,
    @SerializedName("locked_at") val lockedAt: String
)

// Admin Flow Models
data class CreateMatchRequest(
    @SerializedName("sport_id") val sportId: Long,
    @SerializedName("team_a_id") val teamAId: Long,
    @SerializedName("team_b_id") val teamBId: Long,
    @SerializedName("start_time") val startTime: String // Expected format: "YYYY-MM-DD HH:MM:SS"
)

data class UpdateStatusRequest(
    @SerializedName("match_id") val matchId: Long,
    @SerializedName("new_status") val newStatus: String // "upcoming", "live", "completed", "cancelled"
)

data class ResolveMatchRequest(
    @SerializedName("match_id") val matchId: Long,
    @SerializedName("resolutions") val resolutions: Map<String, String> // Maps questionId string to correctOption (e.g. "12" to "A")
)

data class SportsResponse(
    @SerializedName("data") val data: List<SportData>
)

data class SportData(
    @SerializedName("id") val id: Long,
    @SerializedName("sport_name") val sportName: String
)

data class TeamsResponse(
    @SerializedName("data") val data: List<TeamDataFull>
)

data class TeamDataFull(
    @SerializedName("id") val id: Long,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("branch_name") val branchName: String?
)

// Campus Economy Models (Phase 12)
data class RewardsResponse(
    @SerializedName("data") val data: List<RewardData>
)

data class RewardData(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("cost") val cost: Long,
    @SerializedName("stock_quantity") val stockQuantity: Int
)

data class RedeemRequest(
    @SerializedName("reward_id") val rewardId: Long
)

data class RedeemResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("redemption_code") val redemptionCode: String?,
    @SerializedName("error") val error: String?
)

data class MyPassesResponse(
    @SerializedName("data") val data: List<PassData>
)

data class PassData(
    @SerializedName("redemption_code") val redemptionCode: String,
    @SerializedName("status") val status: String, // 'active' or 'claimed'
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("redeemed_at") val redeemedAt: String
)

data class ClaimRewardRequest(
    @SerializedName("redemption_code") val redemptionCode: String
)
