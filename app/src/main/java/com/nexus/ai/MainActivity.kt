package com.nexus.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val CHAT_URL = "http://127.0.0.1:8080/v1/chat/completions"
private const val MODEL_NAME = "Qwen3-1.7B"

data class ChatMessage(val role: String, val content: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexusScreen() }
    }
}

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(180, TimeUnit.SECONDS)
    .callTimeout(200, TimeUnit.SECONDS)
    .build()

private suspend fun askNexus(history: List<ChatMessage>): String =
    withContext(Dispatchers.IO) {
        val messages = JSONArray()
        messages.put(
            JSONObject()
                .put("role", "system")
                .put("content", "You are NEXUS, a helpful and honest personal AI assistant. Answer clearly. If you do not know, say so.")
        )
        history.forEach { message ->
            messages.put(JSONObject().put("role", message.role).put("content", message.content))
        }

        val payload = JSONObject()
            .put("model", MODEL_NAME)
            .put("messages", messages)
            .put("temperature", 0.7)
            .put("max_tokens", 512)
            .put("stream", false)

        val request = Request.Builder()
            .url(CHAT_URL)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Local AI server returned HTTP ${response.code}: $body")
            }
            JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusScreen() {
    val messages = remember {
        mutableStateListOf(
            ChatMessage("assistant", "Hello! I'm NEXUS, connected to your local AI model.")
        )
    }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("NEXUS AI")
                            Text("Local Qwen3 · On your phone",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        Modifier
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(10.dp)
                    ) {
                        errorText?.let {
                            Text(it, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Message NEXUS…") },
                                enabled = !busy,
                                maxLines = 4
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = input.isNotBlank() && !busy,
                                onClick = {
                                    val prompt = input.trim()
                                    input = ""
                                    errorText = null
                                    messages.add(ChatMessage("user", prompt))
                                    busy = true
                                    scope.launch {
                                        try {
                                            val answer = askNexus(messages.toList())
                                            messages.add(ChatMessage("assistant", answer))
                                        } catch (e: Exception) {
                                            errorText = e.message ?: "Could not reach local model. Check Termux server."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            ) { Text("Send") }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (message.role == "user")
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.widthIn(max = 340.dp)
                        ) {
                            Text(message.content, Modifier.padding(12.dp))
                        }
                    }
                }
                if (busy) item {
                    Text("NEXUS is thinking…", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
