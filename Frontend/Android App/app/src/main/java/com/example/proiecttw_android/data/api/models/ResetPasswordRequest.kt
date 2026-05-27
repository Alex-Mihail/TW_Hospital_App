package com.example.proiecttw_android.data.models

data class ResetPasswordRequest(
    val identifier: String,
    val newPassword: String
)