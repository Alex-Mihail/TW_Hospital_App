package com.example.proiecttw_android.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {

    @GET("patient/{id}")
    suspend fun getPatient(@Path("id") id: Long): Response<PatientProfileDto>

    @GET("doctor/{id}/account")
    suspend fun getDoctorAccount(@Path("id") id: Long): Response<DoctorProfileDto>

    @PUT("patient/{id}")
    suspend fun updatePatient(@Path("id") id: Long, @Body body: PatientUpdateRequest): Response<Unit>

    @PUT("doctor/{id}/account")
    suspend fun updateDoctorAccount(@Path("id") id: Long, @Body body: DoctorAdminUpdateRequest): Response<Unit>

    @DELETE("patient/{id}")
    suspend fun deletePatient(@Path("id") id: Long): Response<Unit>
}
