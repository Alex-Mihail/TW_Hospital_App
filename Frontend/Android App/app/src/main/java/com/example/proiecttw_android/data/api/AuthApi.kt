package com.example.proiecttw_android.data.api

import com.example.proiecttw_android.data.models.LoginRequest
import com.example.proiecttw_android.data.models.LoginResponse
import com.example.proiecttw_android.data.models.PatientRegisterRequest
import com.example.proiecttw_android.data.models.ResetPasswordRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {

    @POST("patient/login")
    suspend fun loginPatient(@Body body: LoginRequest): Response<LoginResponse>

    @POST("doctor/login")
    suspend fun loginDoctor(@Body body: LoginRequest): Response<LoginResponse>

    // REGISTER
    @POST("patient/register")
    suspend fun registerPatient(@Body body: PatientRegisterRequest): Response<Any>

    @PUT("patient/reset-password")
    suspend fun resetPasswordPatient(@Body req: ResetPasswordRequest): Response<ResponseBody>

    @PUT("doctor/reset-password")
    suspend fun resetPasswordDoctor(@Body req: ResetPasswordRequest): Response<ResponseBody>
}
