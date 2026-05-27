package com.example.proiecttw_android.data.api

import retrofit2.Response
import retrofit2.http.GET

interface SpecializationApi {
    @GET("/api/specializations")
    suspend fun getAll(): Response<List<SpecializationDto>>
}
