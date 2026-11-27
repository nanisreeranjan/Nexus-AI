# ULTRON AI Companion 4.0 — implementation notes

This branch adds the first companion layer on top of the existing Nexus-AI 3.1 Android app. Existing browser, vision/detection, accessibility and local llama.cpp client code is retained.

## Selected profile
- Voice output: Android TextToSpeech using the device's selected natural voice.
- Personality: futuristic, calm, technically sharp companion tone.
- Privacy: local-first. The existing llama.cpp endpoint remains the only AI endpoint.
- Listening: an opt-in foreground microphone service listens for the spoken phrase "ULTRON" using Android's on-device SpeechRecognizer.

## Important scope and limitations
- This is a first wake-phrase prototype, not a dedicated low-power wake-word DSP/keyword-spotting engine. It may consume significant battery, pause or restart depending on the phone's speech service, and is not guaranteed to remain continuously active.
- The microphone service is started only after the user taps Enable wake word while the app is visible and grants microphone permission. Android restricts microphone foreground services started from the background. The ongoing notification is intentional and provides a visible stop path.
- Speech recognition uses Android's on-device recognizer only. If unavailable, ULTRON refuses to start wake-word listening; it does not silently switch to cloud recognition.
- Text-to-speech uses the Android-installed TTS engine/voice. Whether that voice works offline depends on the voice package installed on the phone.
- Hybrid cloud routing is not connected in this branch. The app remains local-only until a cloud provider/endpoint and a user-controlled opt-in configuration are deliberately added. No API keys are embedded in source or committed to GitHub.
- The existing local inference server at 127.0.0.1:8080 is unchanged. No model files, Termux files, or current server processes are modified by this branch.

## Build
Run the repository's GitHub Actions workflow on this branch and install the resulting debug APK only after the build succeeds. Test wake phrase mode with the app open, microphone permission granted, and the foreground notification visible.
