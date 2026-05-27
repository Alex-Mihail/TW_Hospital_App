package com.example.proiecttw_android.data.models

data class PatientRegisterRequest(
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String? = null,
    val password: String,
    val phone: String,
    val dateOfBirth: String
)