# Screen Lock for Kids

A system-wide screen lock overlay app for Android that lets parents lock the screen while their child watches videos on YouTube, WhatsApp, or any other app.

## Features

- 🎯 **Floating lock button** — Stays on screen over any app
- 🔒 **One-tap lock** — Instantly blocks all touch input
- 🔓 **One-tap unlock** — Tap the floating button again to restore touch
- 🔄 **Draggable** — Move the floating button anywhere on screen
- 🛡️ **Lightweight** — No internet required, no data collection

## How It Works

1. Open any video app (YouTube, Netflix, etc.)
2. Tap the floating lock button → Screen locks
3. Your child can watch without interruption
4. Tap the floating button again to unlock

## Tech Stack

- Kotlin
- Jetpack Compose
- Hilt (DI)
- WindowManager (system overlay)
- Foreground Service

## Building

```bash
# Build debug APK
./gradlew assembleDebug

# Build release AAB for Google Play
./gradlew bundleRelease
```

## Privacy

This app does not collect any personal data. The overlay permission is only used to display the floating lock button over other apps.

## License

MIT
