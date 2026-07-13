# audio-control-plugin
A Flutter plugin to control the audio being played in Android device.

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

