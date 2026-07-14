# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`audio_control` is a Flutter plugin (Android-only) that gives an app access to other apps' media
playback sessions on the device — enumerate media apps, read now-playing metadata/artwork, and send
transport controls (play/pause/skip/seek/custom actions) to whichever app currently holds an active
`MediaSession`. It works by registering an `AudioControlPlugin` `NotificationListenerService`, which is
the only way Android grants a third-party app visibility into other apps' media sessions.

## Commands

Run these from the repo root unless noted.

```bash
flutter pub get                     # fetch plugin dependencies
flutter test                        # run the Dart unit tests in test/
flutter test test/audio_control_test.dart   # run a single test file
flutter analyze                     # lint per analysis_options.yaml (flutter_lints)
```

Example app (in `example/`, depends on the plugin via a local path dependency):

```bash
cd example
flutter pub get
flutter run                         # launches the example on a connected Android device/emulator
flutter test                        # example app's own widget test
```

Android native unit tests (JUnit/Mockito/Robolectric, under `android/src/test/kotlin/`) run through the
plugin module's own Gradle wrapper, not through `flutter test`:

```bash
cd android
./gradlew testDebugUnitTest         # runs the Kotlin unit test suite (AudioControl, MediaInfo, BitmapUtils, etc.)
```

There is no iOS implementation — `pubspec.yaml` only declares an `android` platform entry, and the
example app only has an `android/` directory. Don't add iOS-specific code paths without first
confirming that's in scope.

## Architecture

Three layers, standard federated-plugin shape:

1. **Dart public API** (`lib/audio_control.dart`) — `AudioControl` singleton exposing
   `initialize()`, `getMediaApps()`, `getActiveSession()`, `controlMediaApp(packageName)`,
   `getMediaInfo()`, `sendAction(PlayerActions, {seek})`, `sendCustomAction(action)`, plus three
   listener setters (`setSessionChangedListener`, `setStateChangeListener`,
   `setSessionDestroyedListener`) that take `StreamController`s supplied by the caller.

2. **Platform interface / method channel** (`lib/audio_control_platform_interface.dart`,
   `lib/audio_control_method_channel.dart`) — the usual `plugin_platform_interface` split. All calls
   go over a single `MethodChannel('audio_control')`. Native → Dart pushes arrive as method calls
   the channel listens for: `stateChanged`, `sessionChanged`, `sessionDestroyed` — these are routed
   to the corresponding `StreamController` rather than returned from a `Future`.

3. **Android native** (`android/src/main/kotlin/com/alexmod/audio_control/`):
   - `AudioControlPlugin.kt` — `FlutterPlugin` + `MethodCallHandler`, the method-channel entry point;
     dispatches to a single `AudioControl` instance.
   - `AudioControl.kt` — core logic. Holds the `MediaSessionManager`, the list of currently active
     media apps, and the live `MediaControllerCompat` for whichever app was selected via
     `controlMediaApp`. `init()` requires the notification listener permission to already be granted
     (see below) — if not, it fires the system settings screen intent and returns `false` rather than
     throwing. Native method names don't always match the Dart-facing ones:
     `controlMediaApp` dispatches to `AudioControl.setupMediaController`, and `getMediaApps` dispatches
     to the async, callback-based `AudioControl.getMediaAppsAsync` (not a synchronous call) — see the
     `when` block in `AudioControlPlugin.kt` for the full method-name mapping.
   - `NotificationListener.kt` — an empty `NotificationListenerService` subclass. Its only job is to
     exist as a registered component so `MediaSessionManager` will grant this app the active-sessions
     list; `NotificationListener.isEnabled()` checks whether the user has granted the permission in
     system settings.
   - `MediaAppDetailsUtils.kt` / `MediaAppDetails.kt` — resolve installed media-browser-service apps
     and active `MediaController`s into `MediaAppDetails` (package name, display name, icon, banner,
     session token).
   - `MediaInfo.kt` / `BitmapUtils.kt` — map `MediaMetadataCompat`/`PlaybackStateCompat` into the
     serializable map sent back over the channel (title/artist/album/art bytes/playback state/custom
     actions); `BitmapUtils` handles bitmap ↔ byte array conversion for artwork and custom-action
     icons.

### Key constraint: the notification listener permission

Nearly everything upstream of `getActiveSession`/`getMediaApps`/`controlMediaApp` depends on the user
having granted "notification access" to the host app (Android Settings → Notification access), because
that's what lets `NotificationListenerService`'s component be passed to
`MediaSessionManager.addOnActiveSessionsChangedListener`. `AudioControl.init()` checks
`NotificationListener.isEnabled(context)` first and bails out (launching the settings screen) if it
isn't granted — callers must call `initialize()` and check its return value before relying on session
data being available.

### State flow for "now playing" data

`controlMediaApp(packageName)` looks up the matching active session's token, constructs a
`MediaControllerCompat`, and registers a callback (`AudioControl.getMediaControllerCallback`) that
converts `onPlaybackStateChanged`/metadata into a `MediaInfo` map and pushes it back to Dart via
`channel.invokeMethod("stateChanged", ...)`. Only one `MediaController` is tracked at a time — calling
`controlMediaApp` again for a different package replaces it. `onSessionDestroyed` on the controller
callback triggers the `sessionDestroyed` channel event, which Dart surfaces through
`setSessionDestroyedListener`.

`PlayerActions` (Dart) and the numeric action ids in `audio_control.dart`'s `actionId` map correspond to
bit positions consumed by `AudioControl.performAction` on the Kotlin side (`1 shl action` against
`PlaybackStateCompat.ACTION_*` constants) — the two enums/maps must stay in sync if either side
changes.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- ALWAYS read graphify-out/GRAPH_REPORT.md before reading any source files, running grep/glob searches, or answering codebase questions. The graph is your primary map of the codebase.
- IF graphify-out/wiki/index.md EXISTS, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
