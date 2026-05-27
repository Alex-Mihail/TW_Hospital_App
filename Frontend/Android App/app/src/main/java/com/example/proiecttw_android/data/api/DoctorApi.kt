package com.example.proiecttw_android.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DoctorApi {
    @GET("/api/doctor/by-specialization")
    suspend fun bySpecialization(
        @Query("specialization") specialization: String
    ): Response<List<DoctorProfileDto>>

    @GET("/api/doctor/{id}")
    suspend fun getDoctorById(
        @retrofit2.http.Path("id") id: Long
    ): retrofit2.Response<DoctorProfileDto>
}
