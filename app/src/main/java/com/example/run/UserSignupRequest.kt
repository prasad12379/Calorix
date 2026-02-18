package com.example.run

data class UserSignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val age: Int,
    val gender: String,
    val height: Double,
    val weight: Double
)
