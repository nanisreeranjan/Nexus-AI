package com.nexus.ai

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.os.Build
import android.widget.*
import android.graphics.Typeface
import android.view.ViewGroup
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val history = mutableListOf<Pair<String, String>>()
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 12)
        }
        val title = TextView(this).apply {
            text = "ULTRON AI 3.1"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
        }
        status = TextView(this).apply {
            text = "PRIVATE MODE • LOCAL AI • INTERNET BROWSER"
            textSize = 12f
        }
        transcript = TextView(this).apply {
            text = "ULTRON is ready. Your prompts go only to the local model at 127.0.0.1.\\n\\nTry: open WhatsApp, search Android news, or sms +911234567890 Hello."
            textSize = 16f
            setPadding(4, 14, 4, 14)
        }
        val scroll = ScrollView(this).apply {
            addView(transcript)
        }
        input = EditText(this).apply {
            hint = "Tell ULTRON what to do..."
            minLines = 2
            maxLines = 4
        }
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val send = Button(this).apply { text = "Run" }
        val browser = Button(this).apply {
            text = "Browser"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, BrowserActivity::class.java)
                    .putExtra("url", "https://www.google.com"))
            }
        }
        val vision = Button(this).apply {
            text = "Image / Video detection"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, DetectionActivity::class.java))
            }
        }
        val voice = Button(this).apply {
            text = "Offline voice"
            setOnClickListener { startOfflineVoice() }
        }
        val accessibility = Button(this).apply {
            text = "Enable phone control"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        buttons.addView(send, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(browser, LinearLayout.LayoutParams(0, -2, 1f))
        send.setOnClickListener { submit() }
        root.addView(title)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(input)
        root.addView(buttons)
        root.addView(vision)
        root.addView(voice)
        root.addView(accessibility)
        setContentView(root)
    }

    private fun startOfflineVoice() {
        if (Build.VERSION.SDK_INT < 31) {
            append("ULTRON", "Strict on-device speech recognition requires Android 12 or newer. This phone may need a compatible offline speech engine.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 810)
            return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            append("ULTRON", "No on-device speech recognition engine is available. I will not fall back to a cloud recognizer.")
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "LISTENING LOCALLY…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "PROCESSING ON DEVICE…" }
            override fun onError(error: Int) {
                status.text = "PRIVATE MODE • LOCAL AI • INTERNET BROWSER"
                append("ULTRON", "Offline voice recognition error: $error")
            }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    input.setText(spoken)
                    append("Voice", spoken)
                }
                status.text = "PRIVATE MODE • LOCAL AI • INTERNET BROWSER"
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 810 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startOfflineVoice()
        } else if (requestCode == 810) {
            append("ULTRON", "Microphone permission was not granted.")
        }
    }

    private fun submit() {
        val prompt = input.text.toString().trim()
        if (prompt.isBlank()) return
        input.setText("")
        append("You", prompt)

        val lower = prompt.lowercase()
        val localCommand = lower.startsWith("open ") ||
            lower.startsWith("search ") || lower.startsWith("browse ") ||
            lower.startsWith("sms ") || lower.startsWith("type ")

        if (localCommand) {
            val result = try {
                AgentCommand.run(this, prompt)
            } catch (e: Exception) {
                "Couldn't complete that action: ${e.message ?: e.javaClass.simpleName}"
            }
            append("ULTRON", result)
            return
        }

        status.text = "CONTACTING LOCAL MODEL ONLY…"
        scope.launch {
            val answer = LocalAiClient.ask(history.toList(), prompt)
            history.add("user" to prompt)
            history.add("assistant" to answer)
            append("ULTRON", answer)
            status.text = "PRIVATE MODE • LOCAL AI • INTERNET BROWSER"
        }
    }

    private fun append(who: String, message: String) {
        transcript.append("\n\n$who: $message")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
