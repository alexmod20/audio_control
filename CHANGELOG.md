## 0.0.1

* Android-only plugin exposing other apps' media sessions: enumerate media apps, read now-playing metadata/artwork, and send transport controls to the active `MediaSession`.
* Ship the `NotificationListenerService` manifest declaration with the plugin itself, instead of requiring host apps to declare it.
* Wire up `FAST_FORWARD` and `REWIND` transport actions.
* Return an empty list from `getActiveSession`/`getMediaApps` instead of crashing when the notification-listener permission hasn't been granted yet.
* Avoid crashing on missing package resources or unresolvable custom-action icons.
* Replace the `QUERY_ALL_PACKAGES` permission with a scoped `<queries>` manifest entry.
* Run icon/banner bitmap conversion off the main thread in `getMediaApps`.
* Add Android native (JUnit/Mockito/Robolectric) and Dart unit test coverage.
