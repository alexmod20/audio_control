# Graph Report - audio_control  (2026-07-13)

## Corpus Check
- 42 files · ~12,905 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 321 nodes · 317 edges · 53 communities (24 shown, 29 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 9 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `251985b3`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Plugin Public API & Tests|Plugin Public API & Tests]]
- [[_COMMUNITY_Example Player Page UI|Example Player Page UI]]
- [[_COMMUNITY_Plugin Architecture & Dependencies|Plugin Architecture & Dependencies]]
- [[_COMMUNITY_Example Dashboard & Lifecycle|Example Dashboard & Lifecycle]]
- [[_COMMUNITY_Data Models & Platform Mocks|Data Models & Platform Mocks]]
- [[_COMMUNITY_Method Channel Platform Bridge|Method Channel Platform Bridge]]
- [[_COMMUNITY_Android AudioControl Methods|Android AudioControl Methods]]
- [[_COMMUNITY_AudioControl Session Control (Kotlin)|AudioControl Session Control (Kotlin)]]
- [[_COMMUNITY_AppCard Widget & Models|AppCard Widget & Models]]
- [[_COMMUNITY_Android Launcher Icons|Android Launcher Icons]]
- [[_COMMUNITY_Plugin Engine Registration (Kotlin)|Plugin Engine Registration (Kotlin)]]
- [[_COMMUNITY_Bitmap Conversion Utilities|Bitmap Conversion Utilities]]
- [[_COMMUNITY_Media App Discovery Utilities|Media App Discovery Utilities]]
- [[_COMMUNITY_MediaInfo Serialization|MediaInfo Serialization]]
- [[_COMMUNITY_Example App Entry Point|Example App Entry Point]]
- [[_COMMUNITY_Notification Access Check|Notification Access Check]]
- [[_COMMUNITY_MediaAppDetails Serialization|MediaAppDetails Serialization]]
- [[_COMMUNITY_Android MainActivity|Android MainActivity]]
- [[_COMMUNITY_BitmapMediaInfo Bridge|Bitmap/MediaInfo Bridge]]
- [[_COMMUNITY_toHashMap Serializers|toHashMap Serializers]]
- [[_COMMUNITY_Player Action Icons|Player Action Icons]]
- [[_COMMUNITY_Send Action Flow|Send Action Flow]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 47|Community 47]]
- [[_COMMUNITY_Community 48|Community 48]]
- [[_COMMUNITY_Community 49|Community 49]]
- [[_COMMUNITY_Community 50|Community 50]]
- [[_COMMUNITY_Community 51|Community 51]]
- [[_COMMUNITY_Community 52|Community 52]]

## God Nodes (most connected - your core abstractions)
1. `Android Native Hardening Implementation Plan` - 12 edges
2. `AudioControl` - 11 edges
3. `audio_control` - 11 edges
4. `audio_control_example` - 8 edges
5. `Task 8: Minor cleanups` - 7 edges
6. `dart:async` - 6 edges
7. `package:flutter/foundation.dart` - 6 edges
8. `Task 5: Ship the `NotificationListenerService` manifest declaration with the plugin` - 6 edges
9. `Task 7: Move `getMediaApps()` icon/banner conversion off the main thread` - 6 edges
10. `AudioControlPlugin.onMethodCall` - 6 edges

## Surprising Connections (you probably didn't know these)
- `audio_control` --uses--> `AudioControlPlugin`  [EXTRACTED]
  README.md → android/src/main/kotlin/com/alexmod/audio_control/AudioControlPlugin.kt
- `AudioControlPlugin` --implements_for--> `Android Platform`  [EXTRACTED]
  android/src/main/kotlin/com/alexmod/audio_control/AudioControlPlugin.kt → pubspec.yaml
- `AppCard` --shares_data_with--> `MediaAppDetails`  [INFERRED]
  example/lib/app_card.dart → lib/media_app_details.dart
- `audio_control_test` --references--> `MethodChannelAudioControl`  [EXTRACTED]
  test/audio_control_test.dart → lib/audio_control_method_channel.dart
- `audio_control_method_channel_test` --references--> `MethodChannelAudioControl`  [EXTRACTED]
  test/audio_control_method_channel_test.dart → lib/audio_control_method_channel.dart

## Hyperedges (group relationships)
- **Media App Discovery and Representation Pattern** — audiocontrol, mediaappdetailsutils, bitmaputils, mediaappdetails [INFERRED 0.85]
- **Flutter Plugin Bridge Pattern** — audiocontrolplugin, audiocontrol, mediainfo, mediaappdetails [INFERRED 0.95]
- **Media Session Control Flow** — audiocontrol_setupmediacontroller, audiocontrol_performaction, audiocontrol_performcustomaction, mediainfo [EXTRACTED 1.00]
- **Platform Interface Pattern** — audio_control_platform_interface_audiocontrolplatform, audio_control_method_channel_methodchannelaudiocontrol, audio_control_audiocontrol [EXTRACTED 1.00]
- **Media Data Models** — media_app_details_mediaappdetails, media_info_mediainfo, audio_control_method_channel_methodchannelaudiocontrol [EXTRACTED 1.00]
- **UI Navigation Flow** — main_myapp, dashboard_page_dashboardpage, player_page_playerpage [EXTRACTED 1.00]
- **Audio Control Plugin Stack** — audio_control, audiocontrolplugin, android_platform, flutter_framework [EXTRACTED 1.00]

## Communities (53 total, 29 thin omitted)

### Community 0 - "Plugin Public API & Tests"
Cohesion: 0.05
Nodes (35): Android Native Hardening Implementation Plan, code:gradle (android {), code:kotlin (package com.alexmod.audio_control), code:kotlin (customAction = customAction.map { ca ->), code:bash (git add android/src/main/kotlin/com/alexmod/audio_control/Au), code:xml (<manifest xmlns:android="http://schemas.android.com/apk/res/), code:kotlin (package com.alexmod.audio_control), code:kotlin (import android.content.pm.LauncherApps.ShortcutQuery.*) (+27 more)

### Community 1 - "Example Player Page UI"
Cohesion: 0.09
Nodes (19): package:audio_control/audio_control.dart, package:audio_control/audio_control_method_channel.dart, package:audio_control_example/dashboard_page.dart, package:audio_control_example/main.dart, package:flutter/material.dart, package:flutter/services.dart, package:flutter_test/flutter_test.dart, AppCard (+11 more)

### Community 2 - "Plugin Architecture & Dependencies"
Cohesion: 0.09
Nodes (21): package:wakelock_plus/wakelock_plus.dart, package:wakelock/wakelock.dart, build, _buildAlbumImage, _buildCustomActionButtons, _buildPlayerButtons, Center, CircularProgressIndicator (+13 more)

### Community 3 - "Example Dashboard & Lifecycle"
Cohesion: 0.1
Nodes (18): package:audio_control_example/app_card.dart, package:audio_control_example/player_page.dart, package:external_app_launcher/external_app_launcher.dart, package:flutter/foundation.dart, build, Center, Container, DashboardPage (+10 more)

### Community 4 - "Data Models & Platform Mocks"
Cohesion: 0.11
Nodes (18): audio_control_method_channel.dart, package:audio_control/audio_control_platform_interface.dart, package:audio_control/media_app_details.dart, package:audio_control/media_info.dart, package:plugin_platform_interface/plugin_platform_interface.dart, AudioControlPlatform, dispose, instance (+10 more)

### Community 5 - "Method Channel Platform Bridge"
Cohesion: 0.14
Nodes (20): Android Audio Playback Control, Android Platform, audio_control, audio_control_example, Audio Control Plugin, AudioControl, AudioControlPlugin, cupertino_icons (+12 more)

### Community 6 - "Android AudioControl Methods"
Cohesion: 0.16
Nodes (16): AppCard, AudioControl, MethodChannelAudioControl, MethodChannelAudioControl._methodCallHandler, audio_control_method_channel_test, AudioControlPlatform, audio_control_test, MockAudioControlPlatform (+8 more)

### Community 8 - "AppCard Widget & Models"
Cohesion: 0.23
Nodes (12): AudioControl.getActiveSession, AudioControl.getMediaApps, AudioControl.init, AudioControl.performAction, AudioControl.performCustomAction, AudioControl.setupMediaController, AudioControlPlugin.onMethodCall, BitmapUtils.bitmapToByteArray (+4 more)

### Community 9 - "Android Launcher Icons"
Cohesion: 0.24
Nodes (9): audio_control_platform_interface.dart, dart:async, media_app_details.dart, media_info.dart, AudioControl, setSessionChangedListener, setSessionDestroyedListener, setStateChangeListener (+1 more)

### Community 10 - "Plugin Engine Registration (Kotlin)"
Cohesion: 0.2
Nodes (8): Architecture, code:bash (flutter pub get                     # fetch plugin dependenc), code:bash (cd example), Commands, graphify, Key constraint: the notification listener permission, State flow for "now playing" data, What this is

### Community 11 - "Bitmap Conversion Utilities"
Cohesion: 0.48
Nodes (7): Audio Control Android Launcher Icon, Flutter Framework Logo, Launcher Icon HDPI (72x72), Launcher Icon MDPI (48x48), Launcher Icon XHDPI (96x96), Launcher Icon XXHDPI (144x144), Launcher Icon XXXHDPI (192x192)

### Community 13 - "MediaInfo Serialization"
Cohesion: 0.33
Nodes (6): code:xml (<manifest xmlns:android="http://schemas.android.com/apk/res/), code:xml (<manifest xmlns:android="http://schemas.android.com/apk/res/), code:bash (cd example/android && ./gradlew :app:processDebugMainManifes), code:markdown (## Android setup), code:bash (git add android/src/main/AndroidManifest.xml example/android), Task 5: Ship the `NotificationListenerService` manifest declaration with the plugin

### Community 17 - "Android MainActivity"
Cohesion: 0.4
Nodes (5): code:kotlin (package com.alexmod.audio_control), code:kotlin (package com.alexmod.audio_control), code:kotlin (package com.alexmod.audio_control), code:bash (git add android/src/test/kotlin/com/alexmod/audio_control/Me), Task 9: Regression tests for the serialization layer (`MediaInfo`, `MediaAppDetails`, `BitmapUtils`)

### Community 19 - "toHashMap Serializers"
Cohesion: 0.67
Nodes (3): bitmapToByteArray(), BitmapUtils, convertDrawable()

### Community 22 - "Community 22"
Cohesion: 0.5
Nodes (3): Android setup, audio-control-plugin, Example

### Community 23 - "Community 23"
Cohesion: 0.5
Nodes (4): DashboardPage, main, MyApp, widget_test

## Knowledge Gaps
- **153 isolated node(s):** `main`, `MockAudioControlPlatform`, `dispose`, `setSessionDestroyedListener`, `setStateChangeListener` (+148 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **29 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `dart:async` connect `Android Launcher Icons` to `Plugin Architecture & Dependencies`, `Example Dashboard & Lifecycle`, `Data Models & Platform Mocks`?**
  _High betweenness centrality (0.025) - this node is a cross-community bridge._
- **Why does `Android Native Hardening Implementation Plan` connect `Plugin Public API & Tests` to `Android MainActivity`, `MediaInfo Serialization`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **Why does `package:flutter/material.dart` connect `Example Player Page UI` to `Plugin Architecture & Dependencies`, `Example Dashboard & Lifecycle`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `audio_control_example` (e.g. with `wakelock` and `external_app_launcher`) actually correct?**
  _`audio_control_example` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `main`, `MockAudioControlPlatform`, `dispose` to the rest of the system?**
  _153 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Plugin Public API & Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._
- **Should `Example Player Page UI` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._