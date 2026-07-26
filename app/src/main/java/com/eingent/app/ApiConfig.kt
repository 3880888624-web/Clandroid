package com.eingent.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ApiConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String
)

object ApiConfigStore {
    private const val PREFS = "eingent_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_COMPLETE = "setup_complete"

    fun save(ctx: Context, config: ApiConfig) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BASE_URL, config.baseUrl)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_MODEL, config.model)
            .putBoolean(KEY_COMPLETE, true)
            .apply()
    }

    fun load(ctx: Context): ApiConfig {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ApiConfig(
            baseUrl = p.getString(KEY_BASE_URL, "") ?: "",
            apiKey = p.getString(KEY_API_KEY, "") ?: "",
            model = p.getString(KEY_MODEL, "") ?: ""
        )
    }

    fun isComplete(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_COMPLETE, false)
}

object ClaudeApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(config: ApiConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = config.baseUrl.trimEnd('/') + "/v1/messages"
                val body = JSONObject().apply {
                    put("model", config.model)
                    put("max_tokens", 5)
                    put("messages", JSONArray().put(
                        JSONObject().put("role", "user").put("content", "hi")
                    ))
                }.toString()
                val req = Request.Builder()
                    .url(url)
                    .header("x-api-key", config.apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    val err = runCatching {
                        JSONObject(resp.body!!.string()).getJSONObject("error").getString("message")
                    }.getOrDefault("HTTP ${resp.code}")
                    error(err)
                }
            }
        }

    suspend fun chat(config: ApiConfig, history: List<Message>): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = config.baseUrl.trimEnd('/') + "/v1/messages"
                val msgs = JSONArray().apply {
                    history.forEach { msg ->
                        put(JSONObject()
                            .put("role", if (msg.isUser) "user" else "assistant")
                            .put("content", msg.text))
                    }
                }
                val body = JSONObject().apply {
                    put("model", config.model)
                    put("max_tokens", 1024)
                    put("messages", msgs)
                }.toString()
                val req = Request.Builder()
                    .url(url)
                    .header("x-api-key", config.apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val resp = client.newCall(req).execute()
                val json = JSONObject(resp.body!!.string())
                if (!resp.isSuccessful) {
                    error(json.getJSONObject("error").getString("message"))
                }
                json.getJSONArray("content").getJSONObject(0).getString("text")
            }
        }
}
