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
    val model: String,
    val format: String = "anthropic"
)

object ApiConfigStore {
    private const val PREFS = "eingent_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_FORMAT = "format"
    private const val KEY_COMPLETE = "setup_complete"

    fun save(ctx: Context, config: ApiConfig) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BASE_URL, config.baseUrl)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_MODEL, config.model)
            .putString(KEY_FORMAT, config.format)
            .putBoolean(KEY_COMPLETE, true)
            .apply()
    }

    fun load(ctx: Context): ApiConfig {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ApiConfig(
            baseUrl = p.getString(KEY_BASE_URL, "") ?: "",
            apiKey = p.getString(KEY_API_KEY, "") ?: "",
            model = p.getString(KEY_MODEL, "") ?: "",
            format = p.getString(KEY_FORMAT, "anthropic") ?: "anthropic"
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

    private fun endpoint(config: ApiConfig, path: String) =
        config.baseUrl.trimEnd('/') + path

    private fun buildRequest(config: ApiConfig, url: String, body: String): Request {
        val builder = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
        if (config.format == "openai") {
            builder.header("Authorization", "Bearer ${config.apiKey}")
        } else {
            builder.header("x-api-key", config.apiKey)
            builder.header("anthropic-version", "2023-06-01")
        }
        return builder.build()
    }

    private fun parseText(json: JSONObject, format: String): String =
        if (format == "openai")
            json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        else {
            val arr = json.getJSONArray("content")
            (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .first { it.optString("type") == "text" }
                .getString("text")
        }

    suspend fun testConnection(config: ApiConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = if (config.format == "openai") "/v1/chat/completions" else "/v1/messages"
                val body = JSONObject().apply {
                    put("model", config.model)
                    put("max_tokens", 5)
                    put("messages", JSONArray().put(
                        JSONObject().put("role", "user").put("content", "hi")
                    ))
                }.toString()
                val req = buildRequest(config, endpoint(config, path), body)
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    val err = runCatching {
                        val j = JSONObject(resp.body!!.string())
                        j.optJSONObject("error")?.optString("message")
                            ?: j.optString("message", "HTTP ${resp.code}")
                    }.getOrDefault("HTTP ${resp.code}")
                    error(err)
                }
            }
        }

    suspend fun chat(config: ApiConfig, history: List<Message>): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = if (config.format == "openai") "/v1/chat/completions" else "/v1/messages"
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
                val req = buildRequest(config, endpoint(config, path), body)
                val resp = client.newCall(req).execute()
                val json = JSONObject(resp.body!!.string())
                if (!resp.isSuccessful) {
                    val errMsg = runCatching {
                        json.optJSONObject("error")?.optString("message")
                            ?: json.optString("message", "HTTP ${resp.code}")
                    }.getOrDefault("HTTP ${resp.code}")
                    error(errMsg)
                }
                parseText(json, config.format)
            }
        }
}
