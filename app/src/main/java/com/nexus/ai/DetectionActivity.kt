package com.nexus.ai

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.*
import android.view.ViewGroup
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class DetectionActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var output: TextView
    private val imageRequest = 701
    private val videoRequest = 702

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
        }
        val title = TextView(this).apply {
            text = "ULTRON VISION"
            textSize = 24f
        }
        val image = Button(this).apply {
            text = "Analyze image"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), imageRequest)
            }
        }
        val video = Button(this).apply {
            text = "Analyze video"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), videoRequest)
            }
        }
        output = TextView(this).apply {
            text = "Choose a photo or video. Text and face analysis run locally on this phone."
            textSize = 16f
        }
        val scroll = ScrollView(this).apply { addView(output) }
        root.addView(title)
        root.addView(image)
        root.addView(video)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    @Deprecated("Use Activity Result APIs in a future UI refactor")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        output.text = "Analyzing locally…"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    if (requestCode == imageRequest) analyzeBitmap(loadBitmap(uri))
                    else if (requestCode == videoRequest) analyzeVideo(uri)
                    else "Unsupported selection."
                } catch (e: Exception) {
                    "Analysis failed: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            output.text = result
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        return contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(BitmapFactory.decodeStream(stream)) { "Could not decode image." }
        }
    }

    private fun analyzeBitmap(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val textClient = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        val faceClient = FaceDetection.getClient(faceOptions)
        try {
            val text = Tasks.await(textClient.process(image), 45, TimeUnit.SECONDS)
            val faces = Tasks.await(faceClient.process(image), 45, TimeUnit.SECONDS)
            val extracted = text.text.ifBlank { "No readable text found." }
            return "IMAGE ANALYSIS (ON DEVICE)\\n\\nTEXT:\\n$extracted\\n\\nFACES DETECTED: ${faces.size}\\n" +
                "Note: face detection counts visible faces. It does not identify people or determine who they are."
        } finally {
            textClient.close()
            faceClient.close()
        }
    }

    private fun analyzeVideo(uri: Uri): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        if (durationMs <= 0L) {
            retriever.release()
            return "Could not read video duration."
        }
        val sampleCount = minOf(8, maxOf(1, (durationMs / 3000L).toInt() + 1))
        val summaries = mutableListOf<String>()
        try {
            for (i in 0 until sampleCount) {
                val timeMs = if (sampleCount == 1) 0L else (durationMs * i / sampleCount)
                val frame = retriever.getFrameAtTime(timeMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: continue
                summaries += "At ${timeMs / 1000}s:\\n${analyzeBitmap(frame)}"
                frame.recycle()
            }
        } finally {
            retriever.release()
        }
        return "VIDEO FRAME ANALYSIS (ON DEVICE)\\nSampled $sampleCount frames from ${durationMs / 1000}s video.\\n\\n" +
            summaries.joinToString("\\n\\n")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
