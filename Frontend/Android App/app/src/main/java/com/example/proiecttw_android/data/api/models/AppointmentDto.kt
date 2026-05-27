package com.example.proiecttw_android.data.api

data class AppointmentDto(
    val id: Long? = null,
    val patient: PatientProfileDto? = null,
    val doctor: DoctorProfileDto? = null,
    val appointmentDatetime: String? = null, // ex: "2026-01-02T10:00:00"
    val description: String? = null,
    val status: String? = null
)

data class UpdateAppointmentStatusRequest(
    val status: String
)

data class CreateAppointmentRequest(
    val patientId: Long,
    val doctorId: Long,
    val appointmentDatetime: String, // "2026-01-02T10:00"
    val description: String? = null
)