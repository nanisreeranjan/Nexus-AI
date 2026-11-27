# ULTRON AI 3.0 — Private Android Agent

A phone-first Kotlin starter for a local-first Android assistant.

## Privacy design
- AI inference is sent only to `http://127.0.0.1:8080` (llama.cpp on the same phone).
- The app has no Gemini/OpenAI/cloud-AI integration and no analytics SDK.
- Public web browsing happens in the app's WebView. Websites you visit can still receive ordinary browser requests and anything you submit to them.
- Personal contacts/messages/files are not included in AI prompts by this starter.
- SMS opens Android's message composer; the user presses Send. This avoids silent message transmission.
- Accessibility is optional and only used for user-requested UI text entry.

## Added ULTRON 3.1 capabilities
- On-device OCR for text in selected images.
- On-device face detection/counting in selected images.
- Video analysis by sampling up to 8 frames and running local OCR + face detection on each frame.
- Strict on-device speech recognition on Android 12+ only when an on-device recognizer is available. No cloud fallback is attempted.
- These are detection capabilities, not identity recognition. The app does not identify a person by name.
- Full-body person/object detection is not yet included; it requires a vetted TensorFlow Lite model bundled with the app.

## What this build does
1. Chat with a local OpenAI-compatible llama.cpp server.
2. Deterministic local commands: `open WhatsApp`, `open YouTube`, `search ...`, `browse ...`, `sms +91... message`, `type ...`.
3. Open a built-in WebView browser for public web searches and websites.
4. Optional Accessibility Service to type text into the currently focused app after the user enables it.
5. No cloud AI fallback. If local inference is unavailable, it says so instead of uploading prompts elsewhere.

## Important limitations
This is a functional starter, not unrestricted phone control. Android sandboxing, app-specific UI changes, permissions, background restrictions and third-party app policies still apply. Accessibility cannot guarantee reliable automation in every app. The starter does not silently send WhatsApp/SMS, make payments, delete data, bypass app protections, or control every screen. SMS is handed to the system composer for user review.

## Requirements
- Android 8.0+ (API 26)
- JDK 17 for building
- Android SDK 35
- Internet permission is used only for browser content and loopback local model requests.

## Run local AI on the same phone
Install Termux from a trusted source, install/build llama.cpp and download a GGUF model yourself. Start the server in Termux, using the actual path of your model:

```bash
./llama-server -m /path/to/your-model.gguf --host 127.0.0.1 --port 8080 -c 2048 -t 4
```

Keep Termux and the server running. ULTRON calls:
`http://127.0.0.1:8080/v1/chat/completions`

If you already have llama.cpp running, keep its current command and port. The model file is not included in this ZIP.

## Build with GitHub Actions using only your phone
1. Extract this ZIP on your Android phone.
2. Upload the contents (not the outer folder) to the root of your GitHub repository. The root must contain `settings.gradle.kts`, `build.gradle.kts`, and `app/`.
3. Commit to the `main` branch.
4. Open repository → Actions → `Build ULTRON AI 3.0 APK` → Run workflow.
5. Wait for the green run. Open its Artifacts section and download `ULTRON-AI-3-debug-apk`.
6. Extract the downloaded artifact and install `app-debug.apk`. Allow installation from your browser/file manager if Android asks.

If the build fails, open the failed step and inspect the first actual error. Do not randomly change SDK or JVM versions.

## Add new options by user commands
This release keeps the executable command set explicit and local. A safe extension mechanism should work as follows:
1. User says `add command ...`.
2. ULTRON creates a proposed Kotlin plugin source file and a manifest describing its permissions.
3. ULTRON shows the complete source and explains permissions/data access.
4. User reviews and approves the change.
5. The project is rebuilt through GitHub Actions and the user installs the new signed APK.

The running APK must not silently rewrite and execute arbitrary code. Android app signing and sandbox rules mean genuine self-upgrades require a rebuild/reinstall or a separately designed, signed plugin system. Never grant a generated plugin unrestricted access.

## Enable phone automation
1. Install ULTRON.
2. Open ULTRON → Enable accessibility.
3. Android Settings opens Accessibility → Downloaded apps/Installed apps → ULTRON Agent → On.
4. Return to ULTRON.
Accessibility is powerful. Enable it only if you understand and accept that Android lets the service inspect screen content to perform requested actions. This starter uses it only for explicit `type ...` commands.

## Commands
- `open WhatsApp`
- `open YouTube`
- `open Settings`
- `search latest Android news`
- `browse https://example.com`
- `sms +911234567890 Hello, I will call you`
- `type Hello from ULTRON`

For `type`, first open the target app and focus its text field. The service inserts text but does not press Send.

## Security notes
- Do not put personal API keys in this app or GitHub. This build needs no cloud API key.
- WebView browsing is not anonymous. Websites can see normal network metadata such as IP address and browser requests.
- Other apps (WhatsApp, Gmail, etc.) communicate with their own providers when you use them. ULTRON cannot prevent that.
- Keep Android, Termux and model files updated. Do not enable Accessibility for apps you do not trust.
