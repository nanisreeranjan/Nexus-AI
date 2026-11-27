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
    private lateinit var companionVoice: CompanionVoice
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        companionVoice = CompanionVoice(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 12)
        }
        val title = TextView(this).apply {
            text = "ULTRON AI COMPANION"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
        }
        status = TextView(this).apply {
            text = "LOCAL-FIRST • CLOUD ROUTE NOT CONFIGURED • VOICE COMPANION"
            textSize = 12f
        }
        transcript = TextView(this).apply {
            text = "ULTRON online. Local model: 127.0.0.1:8080.\n\nSay “ULTRON” after enabling wake word, or type a message."
            textSize = 16f
            setPadding(4, 14, 4, 14)
        }
        val scroll = ScrollView(this).apply { addView(transcript) }
        input = EditText(this).apply {
            hint = "Talk to ULTRON..."
            minLines = 2
            maxLines = 4
        }
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val send = Button(this).apply { text = "Send" }
        val browser = Button(this).apply {
            text = "Browser"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, BrowserActivity::class.java)
                    .putExtra("url", "https://www.google.com"))
            }
        }
        val vision = Button(this).apply {
            text = "Vision"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, DetectionActivity::class.java))
            }
        }
        val voice = Button(this).apply {
            text = "Speak"
            setOnClickListener { startOfflineVoice() }
        }
        val wake = Button(this).apply {
            text = if (WakeWordService.isRunning) "Stop wake word" else "Enable wake word"
            setOnClickListener {
                if (WakeWordService.isRunning) {
                    stopService(Intent(this@MainActivity, WakeWordService::class.java))
                    text = "Enable wake word"
                    status.text = "WAKE WORD OFF • LOCAL AI READY"
                } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startWakeWordService()
                    text = "Stop wake word"
                } else {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 811)
                }
            }
        }
        val accessibility = Button(this).apply {
            text = "Phone control settings"
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
        root.addView(wake)
        root.addView(accessibility)
        setContentView(root)

        if (intent?.getBooleanExtra(WakeWordService.EXTRA_WAKE_TRIGGER, false) == true) {
            startOfflineVoice()
        }
    }

    private fun startWakeWordService() {
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            append("ULTRON", "This Android device does not expose an on-device speech recognizer. Wake-word mode cannot start; no cloud recognizer will be used.")
            return
        }
        val serviceIntent = Intent(this, WakeWordService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
            status.text = "WAKE WORD ACTIVE • ON-DEVICE LISTENING"
            append("ULTRON", "Wake phrase listener started. Say “ULTRON”. Keep in mind this first version uses Android's on-device speech recognizer, not a dedicated low-power wake-word chip.")
        } catch (e: Exception) {
            append("ULTRON", "Couldn't start wake-word listening: ${e.javaClass.simpleName}")
        }
    }

    private fun startOfflineVoice() {
        if (Build.VERSION.SDK_INT < 31) {
            append("ULTRON", "Strict on-device speech recognition requires Android 12 or newer.")
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
                status.text = "LOCAL AI READY"
                append("ULTRON", "Offline voice recognition error: $error")
            }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    input.setText(spoken)
                    append("Voice", spoken)
                    submit()
                }
                status.text = "LOCAL AI READY"
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra(WakeWordService.EXTRA_WAKE_TRIGGER, false) == true) {
            startOfflineVoice()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 810 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startOfflineVoice()
        } else if (requestCode == 811 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startWakeWordService()
        } else if (requestCode == 810 || requestCode == 811) {
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

        status.text = "THINKING • LOCAL MODEL…"
        scope.launch {
            val answer = LocalAiClient.ask(history.toList(), prompt)
            history.add("user" to prompt)
            history.add("assistant" to answer)
            append("ULTRON", answer)
            status.text = if (WakeWordService.isRunning) "WAKE WORD ACTIVE • LOCAL AI READY" else "LOCAL AI READY"
        }
    }

    private fun append(who: String, message: String) {
        transcript.append("\n\n$who: $message")
        if (who == "ULTRON") companionVoice.speak(message)
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        companionVoice.shutdown()
        scope.cancel()
        super.onDestroy()
    }
}
