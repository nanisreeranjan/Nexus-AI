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
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val history = mutableListOf<Pair<String, String>>()
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var send: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var busy = false
    private val pageColor = Color.rgb(18, 18, 20)
    private val panelColor = Color.rgb(34, 35, 38)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = pageColor
        window.navigationBarColor = pageColor
        buildUi()
        restoreHistory()
        if (history.isEmpty()) {
            addBubble("assistant", "Hi! I'm ULTRON. Ask me anything, or give me a phone command. Your conversation is processed by your local Qwen model.")
        } else {
            history.forEach { (role, text) -> addBubble(role, text) }
        }
        status.text = "LOCAL QWEN • PRIVATE MODE"
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(10))
            setBackgroundColor(pageColor)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(12))
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(TextView(this).apply {
            text = "ULTRON"
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        status = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.rgb(100, 205, 160))
        }
        brand.addView(status)
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(chip("+ New chat") { newChat() })
        root.addView(header)

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(12))
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(8))
        }
        tools.addView(chip("Browser") {
            startActivity(Intent(this@MainActivity, BrowserActivity::class.java).putExtra("url", "https://www.google.com"))
        })
        tools.addView(chip("Vision") {
            startActivity(Intent(this@MainActivity, DetectionActivity::class.java))
        })
        tools.addView(chip("Voice") { startOfflineVoice() })
        tools.addView(chip("Phone control") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        root.addView(tools)

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(5), dp(5), dp(5))
            background = rounded(panelColor, 24)
        }
        input = EditText(this).apply {
            hint = "Message ULTRON…"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            minLines = 1
            maxLines = 5
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = null
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { submit(); true } else false
        }
        send = TextView(this).apply {
            text = "↑"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(16, 163, 127), 22)
            isClickable = true
            isFocusable = true
            setOnClickListener { submit() }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(send, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(composer)
        setContentView(root)
    }

    private fun chip(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 11f
        setTextColor(Color.LTGRAY)
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(8), dp(9), dp(8))
        background = rounded(panelColor, 18)
        setOnClickListener { action() }
        val lp = LinearLayout.LayoutParams(-2, -2)
        lp.marginEnd = dp(5)
        layoutParams = lp
    }

    private fun addBubble(role: String, value: String) {
        val user = role == "user"
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (user) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }
        row.addView(TextView(this).apply {
            text = if (user) "YOU" else "ULTRON"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (user) Color.rgb(135, 175, 255) else Color.rgb(80, 205, 165))
            setPadding(dp(5), 0, dp(5), dp(4))
        })
        row.addView(TextView(this).apply {
            text = value
            textSize = 15.5f
            setTextColor(Color.rgb(235, 235, 238))
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(if (user) Color.rgb(45, 68, 103) else panelColor, 18)
            maxWidth = resources.displayMetrics.widthPixels * 85 / 100
            textIsSelectable = true
        })
        messages.addView(row, LinearLayout.LayoutParams(-1, -2))
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun submit() {
        if (busy) return
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        input.setText("")
        addBubble("user", prompt)

        val lower = prompt.lowercase()
        val isPhoneCommand = lower.matches(Regex("^(open|search|browse|sms|type)\\s+.+"))
        if (isPhoneCommand) {
            val result = try { AgentCommand.run(this, prompt) }
            catch (e: Exception) { "I couldn't complete that action: ${e.message ?: e.javaClass.simpleName}" }
            history.add("user" to prompt)
            history.add("assistant" to result)
            saveHistory()
            addBubble("assistant", result)
            return
        }

        busy = true
        send.isEnabled = false
        send.alpha = 0.5f
        status.text = "ULTRON IS THINKING…"
        val waiting = TextView(this).apply {
            text = "ULTRON is thinking…"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        messages.addView(waiting)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

        scope.launch {
            val answer = try {
                LocalAiClient.ask(history.toList(), prompt)
            } catch (e: Exception) {
                "I couldn't get a response from the local model: ${e.message ?: e.javaClass.simpleName}"
            }
            messages.removeView(waiting)
            history.add("user" to prompt)
            history.add("assistant" to answer)
            saveHistory()
            addBubble("assistant", answer)
            busy = false
            send.isEnabled = true
            send.alpha = 1f
            status.text = "LOCAL QWEN • PRIVATE MODE"
        }
    }

    private fun newChat() {
        if (busy) return
        history.clear()
        messages.removeAllViews()
        getSharedPreferences("ultron_chat", MODE_PRIVATE).edit().remove("history").apply()
        addBubble("assistant", "New conversation started. What would you like to talk about?")
    }

    private fun saveHistory() {
        val array = org.json.JSONArray()
        history.forEach { (role, value) ->
            array.put(org.json.JSONObject().put("role", role).put("text", value))
        }
        getSharedPreferences("ultron_chat", MODE_PRIVATE).edit().putString("history", array.toString()).apply()
    }

    private fun restoreHistory() {
        try {
            val raw = getSharedPreferences("ultron_chat", MODE_PRIVATE).getString("history", null) ?: return
            val array = org.json.JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                history.add(item.getString("role") to item.getString("text"))
            }
        } catch (_: Exception) {
            history.clear()
        }
    }

    private fun startOfflineVoice() {
        if (Build.VERSION.SDK_INT < 31) {
            addBubble("assistant", "On-device speech recognition requires Android 12 or newer.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 810)
            return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            addBubble("assistant", "No on-device speech recognition engine is available on this phone.")
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "LISTENING…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "PROCESSING VOICE…" }
            override fun onError(error: Int) {
                status.text = "LOCAL QWEN • PRIVATE MODE"
                addBubble("assistant", "Speech recognition error: $error")
            }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) { input.setText(spoken); submit() }
                status.text = "LOCAL QWEN • PRIVATE MODE"
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
        if (requestCode == 810 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startOfflineVoice()
        else if (requestCode == 810) addBubble("assistant", "Microphone permission was not granted.")
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        scope.cancel()
        super.onDestroy()
    }
}
