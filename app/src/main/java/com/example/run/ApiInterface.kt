package com.example.run

import ActivityRequest
import UserResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {
    // ── Chatbot — now passes email for personalized context ──────────────
    @GET("chat")   // or whatever your endpoint is
    fun getFitnessResponse(
        @Query("prompt") prompt: String,
        @Query("email")  email:  String
    ): Call<MyData>

    // ✅ Signup API (NEW)
    @POST("auth/signup")
    fun signupUser(
        @Body signupRequest: UserSignupRequest
    ): Call<SignupResponse>

    @POST("auth/signin")
    fun signinUser(
        @Body signinRequest: SigninRequest
    ): Call<SigninResponse>

    @GET("user")
    fun getUser(
        @Query("email") email: String
    ): Call<UserResponse>

    @POST("activity")
    fun saveActivity(
        @Body activity: ActivityRequest
    ): Call<SimpleResponse>

    @GET("activities/{email}")
    fun getActivities(
        @Path("email") email: String
    ): Call<ActivityListResponse>

}