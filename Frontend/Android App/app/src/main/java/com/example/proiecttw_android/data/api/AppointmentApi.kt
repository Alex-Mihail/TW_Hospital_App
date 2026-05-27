package com.example.proiecttw_android.data.api

import retrofit2.Response
import retrofit2.http.*

interface AppointmentApi {

    @GET("/api/appointments/patient/{patientId}")
    suspend fun byPatient(@Path("patientId") patientId: Long): Response<List<AppointmentDto>>

    @GET("/api/appointments/doctor/{doctorId}")
    suspend fun byDoctor(@Path("doctorId") doctorId: Long): Response<List<AppointmentDto>>

    @PUT("/api/appointments/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: Long,
        @Body body: UpdateAppointmentStatusRequest
    ): Response<AppointmentDto>

    @PUT("/api/appointments/{id}/cancel")
    suspend fun cancel(@Path("id") id: Long): Response<AppointmentDto>

    @DELETE("/api/appointments/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>

    // availability
    @GET("/api/appointments/availability")
    suspend fun availability(
        @Query("doctorId") doctorId: Long,
        @Query("date") date: String
    ): Response<List<AppointmentDto>>

    // âœ… Create appointment
    @POST("/api/appointments")
    suspend fun create(@Body body: CreateAppointmentRequest): Response<AppointmentDto>
}
