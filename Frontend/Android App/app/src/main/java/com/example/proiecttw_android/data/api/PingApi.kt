package com.example.proiecttw_android.data.api

import com.example.proiecttw_android.data.models.PingResponse
import retrofit2.http.GET

interface PingApi {
    @GET("ping")
    suspend fun ping(): PingResponse
}