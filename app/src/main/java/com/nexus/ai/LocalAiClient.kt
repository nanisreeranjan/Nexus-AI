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
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 180000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val messages = JSONArray()
                messages.put(JSONObject().put("role", "system").put(
                    "content",
                    "You are ULTRON, a helpful conversational AI assistant running locally on an Android phone. " +
                    "Respond naturally to EVERY ordinary user message, including greetings like hi or hello, " +
                    "questions, explanations, writing, translation, brainstorming and casual conversation. " +
                    "Do not treat ordinary conversation as phone commands. " +
                    "Be friendly, direct and useful; give enough detail to answer the question without rambling. " +
                    "If you do not know something, say so instead of inventing facts. " +
                    "Never claim you performed an action unless the Android app confirms it. " +
                    "Never ask for passwords, OTPs or secrets. " +
                    "You do not have live internet access in this chat; the app's browser can be opened separately. " +
                    "You are a local model and must not claim to be ChatGPT or a cloud service."
                ))
                history.takeLast(10).forEach { (role, content) ->
                    if (role == "user" || role == "assistant") {
                        messages.put(JSONObject().put("role", role).put("content", content))
                    }
                }
                messages.put(JSONObject().put("role", "user").put("content", prompt))
                val body = JSONObject()
                    .put("model", "local-model")
                    .put("messages", messages)
                    .put("temperature", 0.7)
                    .put("max_tokens", 192)
                    .put("chat_template_kwargs", JSONObject().put("enable_thinking", false))
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    return@withContext "The local AI server returned HTTP $code. Check that llama-server is running at 127.0.0.1:8080."
                }
                val json = JSONObject(response)
                json.optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "The local model returned an empty response. Try again or check the model server."
            } catch (e: Exception) {
                "I can't reach your local AI model. Start llama-server in Termux at 127.0.0.1:8080. No cloud fallback was used. (${e.javaClass.simpleName})"
            } finally {
                connection?.disconnect()
            }
        }
}
