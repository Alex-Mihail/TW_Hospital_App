package com.example.proiecttw_android.ui

data class UserUi(
    val id: Long,
    val role: String,
    val username: String,
    val firstName: String,
    val lastName: String
) {
    val displayName: String get() = "$firstName $lastName"
}
