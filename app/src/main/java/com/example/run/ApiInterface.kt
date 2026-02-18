package com.example.run

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiInterface {
    @GET("chat")
    fun getFitnessResponse(
        @Query("prompt") prompt: String
    ): Call<MyData>

    // ✅ Signup API (NEW)
    @POST("signup")
    fun signupUser(
        @Body signupRequest: UserSignupRequest
    ): Call<SignupResponse>

    @POST("signin")
    fun signinUser(
        @Body signinRequest: SigninRequest
    ): Call<SigninResponse>


}