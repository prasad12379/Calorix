package com.example.run

data class ActivityListResponse(
    val data: List<ActivityItem>
)

data class ActivityItem(
    val email: String,
    val workout_mode: String,
    val duration: String,
    val distance: String,
    val calories: String,
    val pace: String,
    val steps: Int,
    val best_pace: String,
    val date: String
)