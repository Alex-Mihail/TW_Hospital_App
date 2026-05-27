package com.example.proiecttw_android.ui.theme

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChatApiClient {
    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun ask(
        baseUrl: String,
        message: String,
        role: String,
        userId: Long?,
        uiContext: Any?
    ): String = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/') + "/api/chat"

        val payload = mapOf(
            "message" to message,
            "role" to role,
            "userId" to userId,
            "uiContext" to uiContext
        )

        val body = gson.toJson(payload).toRequestBody(JSON)

        val req = Request.Builder()
            .url(url)
            .post(body)
            .build()

        http.newCall(req).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw RuntimeException("HTTP ${res.code}: $raw")
            }
            val parsed = gson.fromJson(raw, Map::class.java)
            (parsed["answer"] as? String) ?: "(Fără răspuns)"
        }
    }
}