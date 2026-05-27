package com.example.proiecttw_android.data.api

// GET patient
data class PatientProfileDto(
    val id: Long? = null,
    val role: String? = null,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val dateOfBirth: String? = null
)

// GET doctor account
data class DoctorProfileDto(
    val id: Long? = null,
    val role: String? = null,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val specialization: SpecializationDto? = null
)

// PUT patient
data class PatientUpdateRequest(
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phone: String? = null,
    val dateOfBirth: String? = null
)

// PUT doctor/admin
data class DoctorAdminUpdateRequest(
    val firstName: String,
    val lastName: String,
    val email: String? = null
)

data class SpecializationDto(
    val id: Long? = null,
    val name: String? = null,
    val title: String? = null
)

