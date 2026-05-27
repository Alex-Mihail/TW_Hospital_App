package com.example.proiecttw_android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

data class StoredUser(
    val id: Long,
    val role: String,      // "patient" / "doctor"
    val username: String,
    val firstName: String,
    val lastName: String
)

class SessionStore(private val context: Context) {
    private val KEY_ID = longPreferencesKey("id")
    private val KEY_ROLE = stringPreferencesKey("role")
    private val KEY_USERNAME = stringPreferencesKey("username")
    private val KEY_FIRST = stringPreferencesKey("firstName")
    private val KEY_LAST = stringPreferencesKey("lastName")

    val userFlow: Flow<StoredUser?> = context.dataStore.data.map { p ->
        val id = p[KEY_ID] ?: return@map null
        val role = p[KEY_ROLE] ?: return@map null
        val username = p[KEY_USERNAME] ?: return@map null
        val first = p[KEY_FIRST] ?: return@map null
        val last = p[KEY_LAST] ?: return@map null
        StoredUser(id, role, username, first, last)
    }

    suspend fun saveUser(u: StoredUser) {
        context.dataStore.edit { p ->
            p[KEY_ID] = u.id
            p[KEY_ROLE] = u.role
            p[KEY_USERNAME] = u.username
            p[KEY_FIRST] = u.firstName
            p[KEY_LAST] = u.lastName
        }
    }

    suspend fun updateName(firstName: String, lastName: String) {
        context.dataStore.edit { p ->
            p[stringPreferencesKey("firstName")] = firstName
            p[stringPreferencesKey("lastName")] = lastName
        }
    }

    suspend fun logout() {
        context.dataStore.edit { it.clear() }
    }
}
