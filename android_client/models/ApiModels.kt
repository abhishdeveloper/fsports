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

// Auth Login Request & Response (login.php)
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
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
    @SerializedName("start_time") val startTime: String
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
    @SerializedName("points_multiplier") val pointsMultiplier: Double
    // Note: 'correct_option' is intentionally omitted by the backend for security.
)

// Prediction Submission Request (submit.php)
data class PredictionRequest(
    @SerializedName("question_id") val questionId: Long,
    @SerializedName("selected_option") val selectedOption: String // Expected values: "A", "B", "C", "D"
)
