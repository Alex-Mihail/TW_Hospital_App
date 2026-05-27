package com.example.proiecttw_android.data.models

data class LoginResponse(
    val id: Long? = null,
    val role: String? = null,      // "PATIENT", "DOCTOR"
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val message: String? = null
)
