# NEXUS AI — Android Kotlin starter

Native Android chat UI (Kotlin + Jetpack Compose) connecting to a local `llama.cpp` OpenAI-compatible server.

## Architecture
- Android app: Kotlin, Jetpack Compose, OkHttp
- Local model server: `llama-server` in Termux
- Model: Qwen3 1.7B GGUF Q4_K_M (starter choice)
- Endpoint: `http://127.0.0.1:8080/v1/chat/completions`

The app does not contain the model or inference engine. Keep `llama-server` running in Termux while using the app.

## Run the model in Termux
Assuming your existing build and model paths:
```bash
cd ~/nexus/llama.cpp
./build/bin/llama-server -m ~/nexus/models/qwen3-1.7b-q4.gguf --host 127.0.0.1 --port 8080 -c 2048 -t 4
```
Test in another Termux session:
```bash
curl http://127.0.0.1:8080/health
```

## Build APK on GitHub
1. Create a private GitHub repository named `NEXUS-AI`.
2. Upload all files from this project, preserving folder structure.
3. Open **Actions** → **Build NEXUS Android APK** → **Run workflow**.
4. When the run succeeds, open it and download the `nexus-debug-apk` artifact.
5. Extract the APK on your phone and install it. Android may ask permission to install unknown apps for the app you use to open it.

## Build locally in Android Studio
Use JDK 17 and Android SDK 35. Open the project root and build the `app` debug variant.

## Important limitations
- This is a starter app: messages are held in memory and disappear when the app process is closed.
- No model training is performed by the Android app.
- The local server must remain running in Termux. Android battery management may stop background processes; exempt Termux from battery optimization if needed.
- The server binds only to 127.0.0.1; do not change it to a public interface without authentication and network security.
