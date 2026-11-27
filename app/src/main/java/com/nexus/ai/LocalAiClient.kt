package com.nexus.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalAiClient {
    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"

    suspend fun ask(history: List<Pair<String, String>>, prompt: String): String =
        withContext(Dispatchers.IO) {
            try {
                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 3500
                    readTimeout = 90000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val messages = JSONArray()
                messages.put(JSONObject().put("role", "system").put(
                    "content",
                    "You are ULTRON, a private on-device Android assistant. You run locally. " +
                    "Never claim you performed an action unless the Android app confirms it. " +
                    "Do not ask the user to share passwords, OTPs, or secrets. " +
                    "Do not claim internet access unless the user uses the built-in browser command. " +
                    "The app executes explicit local commands separately. Give concise, practical answers."
                ))
                history.takeLast(8).forEach { (role, content) ->
                    messages.put(JSONObject().put("role", role).put("content", content))
                }
                messages.put(JSONObject().put("role", "user").put("content", prompt))
                val body = JSONObject()
                    .put("model", "local-model")
                    .put("messages", messages)
                    .put("temperature", 0.6)
                    .put("max_tokens", 512)
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(response)
                json.optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Local model returned no response. Check the llama.cpp server."
            } catch (e: Exception) {
                "Local AI is unavailable. Start llama-server in Termux at 127.0.0.1:8080. No cloud fallback was used. Details: ${e.javaClass.simpleName}"
            }
        }
}
