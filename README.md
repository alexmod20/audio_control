# audio_control

A Flutter plugin (Android-only) that gives your app access to other apps' media playback
sessions on the device: enumerate installed media apps, read now-playing metadata and
artwork, and send transport controls (play/pause/skip/seek/custom actions) to whichever
app currently holds an active `MediaSession`.

It works by registering an `AudioControlPlugin` `NotificationListenerService`, which is the
only way Android grants a third-party app visibility into other apps' media sessions.

## Example
https://github.com/user-attachments/assets/e6f53f68-3fa4-47a0-a574-571f7e395baa

## Android setup

This plugin registers a `NotificationListenerService` automatically via manifest
merging — no manifest changes are required in the consuming app.

The user must still grant **Notification access** to your app manually
(Android Settings → Apps → Special app access → Notification access), because
this permission cannot be requested with a runtime permission dialog. Call
`AudioControl.instance.initialize()`; if the permission isn't granted yet, it
opens the system settings screen for the user and returns `false` — call it
again after the user grants access.

There is no iOS implementation: `pubspec.yaml` only declares an `android` platform entry.

## Usage

### 1. Initialize

```dart
final granted = await AudioControl.instance.initialize();
if (!granted!) {
  // Notification access isn't granted yet — the system settings screen was
  // opened for the user. Call initialize() again once they grant access
  // (e.g. from an app lifecycle resume callback).
  return;
}
```

### 2. Discover media apps and active sessions

```dart
// All installed apps that expose a media-browser service.
final mediaApps = await AudioControl.instance.getMediaApps();

// Apps that currently have an active MediaSession (i.e. something playing
// or recently played).
final activeSessions = await AudioControl.instance.getActiveSession();
```

Both calls resolve to a `List<MediaAppDetails>` (package name, display name, icon, banner).

Subscribe to `setSessionChangedListener` to be notified whenever the set of active
sessions changes, instead of polling `getActiveSession()`:

```dart
final sessionChangedController = StreamController<List<MediaAppDetails>>();
AudioControl.instance.setSessionChangedListener(sessionChangedController);
sessionChangedController.stream.listen((activeApps) { ... });
```

### 3. Take control of one app's session

```dart
final ok = await AudioControl.instance.controlMediaApp(packageName);
```

Only one app can be controlled at a time — calling `controlMediaApp` again for a
different package replaces the previously tracked session.

### 4. Listen for now-playing updates

```dart
final stateChangeController = StreamController<MediaInfo>();
AudioControl.instance.setStateChangeListener(stateChangeController);

stateChangeController.stream.listen((MediaInfo info) {
  print('${info.title} — ${info.artist} (${info.state})');
});
```

`setStateChangeListener` replays the last known `MediaInfo` to a new subscriber
immediately, so you don't need to also call `getMediaInfo()` on first load.

Use `setSessionDestroyedListener` to react when the controlled app's session goes
away (e.g. the user closed the app):

```dart
final sessionDestroyedController = StreamController();
AudioControl.instance.setSessionDestroyedListener(sessionDestroyedController);
sessionDestroyedController.stream.listen((_) {
  // The MediaController is no longer valid — stop showing player UI.
});
```

### 5. Send transport controls

```dart
await AudioControl.instance.sendAction(PlayerActions.PLAY);
await AudioControl.instance.sendAction(PlayerActions.PAUSE);
await AudioControl.instance.sendAction(PlayerActions.SKIP_TO_NEXT);
await AudioControl.instance.sendAction(PlayerActions.SEEK_TO, seek: 10); // seconds

// App-specific custom actions surfaced via MediaInfo.customAction.
await AudioControl.instance.sendCustomAction(action);
```

## API reference

### `AudioControl` (`lib/audio_control.dart`)

Singleton accessed via `AudioControl.instance`.

| Member | Description |
| --- | --- |
| `initialize()` | Requests/verifies the notification listener permission and starts the session listener. Returns `false` (after opening system settings) if the permission isn't granted yet. |
| `isInit` | Last known result of `initialize()`. |
| `getMediaApps()` | All installed apps exposing a media-browser service. |
| `getActiveSession()` | Apps with a currently active `MediaSession`. |
| `controlMediaApp(packageName)` | Starts tracking the given app's session; its updates are delivered via the state-change listener. |
| `getMediaInfo()` | One-shot fetch of the current `MediaInfo` for the tracked session. |
| `sendAction(PlayerActions action, {int? seek})` | Sends a transport control action; `seek` is required (in seconds) for `SEEK_TO`. |
| `sendCustomAction(String action)` | Invokes one of the app-specific custom actions listed in `MediaInfo.customAction`. |
| `setStateChangeListener(StreamController<MediaInfo>)` | Streams `MediaInfo` updates for the tracked session. |
| `setSessionChangedListener(StreamController<List<MediaAppDetails>>)` | Streams updates to the list of active sessions. |
| `setSessionDestroyedListener(StreamController)` | Fires once when the tracked session is destroyed. |

### `PlayerActions` (`lib/audio_control.dart`)

`STOP`, `PAUSE`, `PLAY`, `FAST_FORWARD`, `REWIND`, `SKIP_TO_PREVIOUS`, `SKIP_TO_NEXT`, `SEEK_TO`.

### `MediaInfo` (`lib/media_info.dart`)

Now-playing snapshot delivered by `getMediaInfo()` and the state-change listener.

| Field | Type | Notes |
| --- | --- | --- |
| `title` | `String?` | Track title. |
| `artist` | `String?` | Track artist. |
| `album` | `String?` | Album name. |
| `image` | `Uint8List?` | Album art, decoded as raw bitmap bytes. |
| `state` | `PlaybackState` | Current playback state. |
| `customAction` | `List<Map<String, dynamic>>` | App-specific actions, each with `name`, `icon` (bytes) and `action` (pass to `sendCustomAction`). |
| `mediaId` | `String?` | The playing app's own stable identifier for the track (from `MediaMetadataCompat.METADATA_KEY_MEDIA_ID`), when the app populates it. Useful for detecting a genuine track change vs. a repeated state update for the same track — do not rely on it being present for every app. |
| `mediaUri` | `String?` | Content URI for the track (from `MediaMetadataCompat.METADATA_KEY_MEDIA_URI`), when the app populates it. Best-effort fallback alongside `mediaId`. |

`mediaId`/`mediaUri` are both best-effort: Android doesn't require media apps to populate
either field, so some apps will report `null` for one or both.

### `PlaybackState` (`lib/media_info.dart`)

`NONE`, `STOPPED`, `PAUSED`, `PLAYING`, `FAST_FORWARDING`, `REWINDING`, `BUFFERING`,
`ERROR`, `CONNECTING`, `SKIPPING_TO_PREVIOUS`, `SKIPPING_TO_NEXT`,
`SKIPPING_TO_QUEUE_ITEM`, `POSITION_UNKNOWN`.

### `MediaAppDetails` (`lib/media_app_details.dart`)

| Field | Type | Notes |
| --- | --- | --- |
| `packageName` | `String` | Android package name. |
| `appName` | `String` | Display name. |
| `icon` | `Uint8List?` | App icon. |
| `banner` | `Uint8List?` | App banner (Android TV/media-browser banner), when available. |

## Development

```bash
flutter pub get                     # fetch plugin dependencies
flutter test                        # run the Dart unit tests in test/
flutter analyze                     # lint per analysis_options.yaml (flutter_lints)
```

Android native unit tests run through the plugin module's own Gradle wrapper via the
example app:

```bash
cd example/android && ./gradlew :audio_control:testDebugUnitTest
```

Run the example app on a connected Android device/emulator:

```bash
cd example
flutter pub get
flutter run
```
