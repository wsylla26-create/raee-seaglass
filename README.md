# RAEE SeaGlass

A see-through teal glass web browser you talk to, made by [RAEE Industries](https://raee-industries.me).
Android app `me.raeeindustries.seaglass` (Android 8.0+).

## What's in it
- Voice: tap the mic and say where to go; SeaGlass talks back (English, French, Spanish, Italian, Tagalog)
- Slim glass top bar (address, mic, menu) that hides while you read, with a pull-down glass menu
- Start page that fades through three shades of teal
- Tabs, private tabs with fingerprint lock, bookmarks, history, downloads, find in page, reader mode, translate
- Ad blocker, Google Safe Browsing scam-site warnings, dark websites, text size
- Floating bubble browser over other apps
- Local weather, Google Play Billing (one-time unlock `seaglass_unlock`, subscription `seaglass_plus`)

## Building
GitHub builds the app for you (`.github/workflows/seaglass.yml`):
- Every push to `main` makes a test APK: download `RAEE-SeaGlass-test-apk` from the Actions run.
- Actions > SeaGlass > Run workflow > tick **release** makes the signed `.aab` for Google Play.
  It needs the upload-key secrets listed at the top of the workflow file.

The version number goes up automatically with every GitHub build.

## Code map (`app/src/main/java/me/raeeindustries/seaglass`)
| File | What it does |
|---|---|
| `MainActivity.java` | The browser: tabs, slim bar, pull-down menu, sheets, voice commands |
| `MenuIcon.java` | Line icons for the pull-down menu |
| `Glass.java` | Teal Glass / Midnight Glass colours and glass shapes |
| `VoiceEngine.java`, `CommandRouter.java` | Listening, talking back, and spoken commands |
| `FloatingService.java` | The floating bubble over other apps |
| `Billing.java`, `AiClient.java` | Google Play purchases and AI answers |
| `AdBlocker.java`, `WebSupport.java`, `PageTools.java` | Ad blocking, WebView setup, reader/page tools |
| `Library.java`, `Downloads.java`, `Weather.java`, `PrivateLock.java` | Bookmarks/history, downloads, weather, private-tab lock |
| `assets/home.html` | The start page |
