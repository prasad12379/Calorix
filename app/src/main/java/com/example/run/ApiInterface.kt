package com.example.run

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("chat")
    fun getFitnessResponse(
        @Query("prompt") prompt: String
    ): Call<MyData>
}