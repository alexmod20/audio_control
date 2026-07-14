# Android Native Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the crash risks, one functional bug, one packaging/manifest gap, and the untested-code gap found in the native Android layer of the `audio_control` Flutter plugin (`android/src/main/kotlin/com/alexmod/audio_control/`), without changing its public Dart API.

**Architecture:** No architectural change. This plan hardens the existing federated-plugin structure (`AudioControlPlugin` → `AudioControl` → `MediaAppDetailsUtils`/`BitmapUtils`/`MediaInfo`) in place: it adds defensive checks around nullable Android framework calls, fixes a bit-mask dispatch gap, moves a manifest declaration from the example app into the plugin itself, tightens two permissions, offloads bitmap-heavy work off the platform thread, and adds the first native unit tests the module has ever had.

**Tech Stack:** Kotlin, AndroidX Media Compat (`androidx.media:media`), Gradle (AGP 9.0.1, Kotlin 2.3.20, JVM 17), JUnit4, Mockito (`mockito-core` + `mockito-kotlin`), Robolectric, Kotlin Coroutines (`kotlinx-coroutines-android` + `-test`).

## Global Constraints

- `minSdkVersion` is `24` — do not use APIs above API 24 without an SDK-version guard.
- `compileSdk 36`, Kotlin `2.3.20`, JVM target `17` — new dependencies must resolve against these.
- Do not change the Dart-facing method channel contract (method names, argument shapes, `PlayerActions`/`actionId` bit values in `lib/audio_control.dart`) — native fixes must stay behind the existing channel methods.
- Do not touch iOS — there is no iOS implementation and none should be added.
- Every task must leave `flutter analyze` and the existing Dart test suite (`flutter test`) green, plus any new/updated Kotlin unit tests green.
- No unrelated refactors — fix exactly the defect described in each task, matching existing code style (the file already mixes `!!`/`?.let` idioms; only replace the ones flagged, don't do a style sweep).

---

## Task 1: Android native unit test infrastructure

The module currently has **zero** native tests — everything is verified only through the Dart-level mocked-platform-channel tests. Every later task in this plan needs a place to put a real Kotlin test, so this task builds that first.

**Files:**
- Modify: `android/build.gradle:1-53`
- Create: `android/src/test/kotlin/com/alexmod/audio_control/SmokeTest.kt`

**Interfaces:**
- Produces: a `test` source set compiled against JUnit4 + Mockito + Robolectric, runnable via `testDebugUnitTest`. Later tasks add files under `android/src/test/kotlin/com/alexmod/audio_control/` and assume this Gradle config exists.

- [ ] **Step 1: Add test dependencies and Robolectric config to `android/build.gradle`**

Edit `android/build.gradle` — add `testOptions` inside the `android { }` block (after the existing `defaultConfig` block, i.e. after line 47) and add test dependencies to the `dependencies { }` block (after line 52):

```gradle
android {
    namespace 'com.alexmod.audio_control'
    compileSdk 36

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    sourceSets {
        main.java.srcDirs += 'src/main/kotlin'
    }

    defaultConfig {
        minSdkVersion 24
    }

    testOptions {
        unitTests.includeAndroidResources = true
        unitTests.returnDefaultValues = true
    }
}

dependencies {
    implementation 'androidx.media:media:1.8.0'
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"

    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.14.2'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:5.4.0'
    testImplementation 'org.robolectric:robolectric:4.14.1'
    testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"
}
```

(The `kotlinx-coroutines-android` main dependency is added now because Task 7 needs it — adding it here keeps the Gradle-file edits in one place instead of touching this file twice.)

- [ ] **Step 2: Write a smoke test to prove the harness works**

Create `android/src/test/kotlin/com/alexmod/audio_control/SmokeTest.kt`:

```kotlin
package com.alexmod.audio_control

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun `test harness runs`() {
        assertEquals(4, 2 + 2)
    }
}
```

- [ ] **Step 3: Run the test to verify the harness works**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.SmokeTest"`

Expected: `BUILD SUCCESSFUL`, 1 test passed. (If the module name isn't `:audio_control`, run `./gradlew projects` first to find the Flutter-plugin-loader-generated project name and use that instead — it's derived from the plugin's package name in `pubspec.yaml`, `audio_control`.)

- [ ] **Step 4: Commit**

```bash
git add android/build.gradle android/src/test/kotlin/com/alexmod/audio_control/SmokeTest.kt
git commit -m "test: add Android native unit test infrastructure"
```

---

## Task 2: Fix `performAction` — `FAST_FORWARD`/`REWIND` silently no-op

`lib/audio_control.dart:18-27` defines `actionId` for all 8 `PlayerActions`, including `FAST_FORWARD` (bit 6) and `REWIND` (bit 3), which correctly correspond to `PlaybackStateCompat.ACTION_FAST_FORWARD` (64) and `ACTION_REWIND` (8). But `AudioControl.performAction` (`android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:173-192`) only handles `ACTION_STOP/PLAY/PAUSE/SKIP_TO_NEXT/SKIP_TO_PREVIOUS/SEEK_TO` in its `when` — calling `sendAction(PlayerActions.FAST_FORWARD)` or `sendAction(PlayerActions.REWIND)` from Dart silently does nothing.

**Files:**
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:173-192`
- Test: `android/src/test/kotlin/com/alexmod/audio_control/AudioControlPerformActionTest.kt`

**Interfaces:**
- Consumes: `AudioControl.performAction(action: Int, seekTo: Int?)` (existing signature, unchanged) and `MediaControllerCompat` / `MediaControllerCompat.TransportControls` from `androidx.media` (existing dependency from Task 1).

- [ ] **Step 1: Write the failing tests**

Create `android/src/test/kotlin/com/alexmod/audio_control/AudioControlPerformActionTest.kt`:

```kotlin
package com.alexmod.audio_control

import android.support.v4.media.session.MediaControllerCompat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.lang.reflect.Field

class AudioControlPerformActionTest {

    private fun audioControlWithMockController(): Pair<AudioControl, MediaControllerCompat.TransportControls> {
        val audioControl = AudioControl()
        val controller = mock<MediaControllerCompat>()
        val transportControls = mock<MediaControllerCompat.TransportControls>()
        org.mockito.kotlin.whenever(controller.transportControls).thenReturn(transportControls)

        val field: Field = AudioControl::class.java.getDeclaredField("mediaController")
        field.isAccessible = true
        field.set(audioControl, controller)

        return audioControl to transportControls
    }

    @Test
    fun `fast forward action calls fastForward on transport controls`() {
        val (audioControl, transportControls) = audioControlWithMockController()

        audioControl.performAction(action = 6, seekTo = null) // PlayerActions.FAST_FORWARD

        verify(transportControls).fastForward()
    }

    @Test
    fun `rewind action calls rewind on transport controls`() {
        val (audioControl, transportControls) = audioControlWithMockController()

        audioControl.performAction(action = 3, seekTo = null) // PlayerActions.REWIND

        verify(transportControls).rewind()
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.AudioControlPerformActionTest"`

Expected: FAIL — both tests fail with `Wanted but not invoked: transportControls.fastForward()/rewind()` because the current `when` block has no matching branch for those action ids and does nothing.

- [ ] **Step 3: Add the missing branches**

Edit `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt`, in `performAction` (replace lines 173-192):

```kotlin
    fun performAction(action: Int, seekTo: Int?) {
        mediaController?.let {
            it.transportControls?.apply {
                val actionId: Long = (1 shl action).toLong()
                when (actionId) {
                    PlaybackStateCompat.ACTION_STOP -> stop()
                    PlaybackStateCompat.ACTION_PLAY -> play()
                    PlaybackStateCompat.ACTION_PAUSE -> pause()
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT -> skipToNext()
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS -> skipToPrevious()
                    PlaybackStateCompat.ACTION_FAST_FORWARD -> fastForward()
                    PlaybackStateCompat.ACTION_REWIND -> rewind()
                    PlaybackStateCompat.ACTION_SEEK_TO -> {
                        seekTo?.let { seekTo ->
                            val position = it.playbackState.position
                            seekTo(position + 1000 * seekTo)
                        }
                    }
                }
            }
        }
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.AudioControlPerformActionTest"`

Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt android/src/test/kotlin/com/alexmod/audio_control/AudioControlPerformActionTest.kt
git commit -m "fix: wire up FAST_FORWARD and REWIND in AudioControl.performAction"
```

---

## Task 3: Guard `getActiveSession` against calls before `initialize()`

`AudioControl.getActiveSession` (`android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:92-98`) does `mMediaSessionManager!!.getActiveSessions(...)`. If Dart calls `getActiveSession()` before `initialize()` succeeded (e.g. permission not yet granted, or the host app calls it out of order), `mMediaSessionManager` is `null` and this throws `KotlinNullPointerException`, crashing the host app instead of returning an empty list the way `AudioControl.isInit` already treats "not ready" as a normal state.

**Files:**
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:92-98`
- Test: `android/src/test/kotlin/com/alexmod/audio_control/AudioControlGetActiveSessionTest.kt`

**Interfaces:**
- Consumes: `AudioControl.getActiveSession(context: Context): List<HashMap<String, Any?>>` (existing signature, unchanged).
- Produces: same signature now returns `emptyList()` instead of throwing when `mMediaSessionManager == null`.

- [ ] **Step 1: Write the failing test**

Create `android/src/test/kotlin/com/alexmod/audio_control/AudioControlGetActiveSessionTest.kt`:

```kotlin
package com.alexmod.audio_control

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class AudioControlGetActiveSessionTest {

    @Test
    fun `getActiveSession before initialize returns empty list instead of throwing`() {
        val audioControl = AudioControl()
        val context = mock<Context>()

        val result = audioControl.getActiveSession(context)

        assertTrue(result.isEmpty())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.AudioControlGetActiveSessionTest"`

Expected: FAIL with `KotlinNullPointerException` (or `NullPointerException`) thrown from `getActiveSession`, not the assertion.

- [ ] **Step 3: Add the guard**

Edit `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt`, replace `getActiveSession` (lines 92-98):

```kotlin
    fun getActiveSession(context: Context): List<HashMap<String, Any?>> {
        val sessionManager = mMediaSessionManager ?: return emptyList()
        val list = sessionManager.getActiveSessions(listenerComponent)
        activeMediaAppDetailsList = MediaAppDetailsUtils.getMediaAppsFromControllers(
            list, context.packageManager
        )
        return activeMediaAppDetailsList.map { mediaAppDetails -> mediaAppDetails.toHasMap() }
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.AudioControlGetActiveSessionTest"`

Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt android/src/test/kotlin/com/alexmod/audio_control/AudioControlGetActiveSessionTest.kt
git commit -m "fix: return empty list from getActiveSession instead of crashing when uninitialized"
```

---

## Task 4: Harden `setupMediaController` against resource-lookup failures

Two crash paths in `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt`:

1. `setupMediaController` (lines 100-132) calls `context.packageManager.getResourcesForApplication(packageName)` (line 115) inside a `try` that only catches `RemoteException` (line 124) — `PackageManager.NameNotFoundException` (thrown if the target app was uninstalled between session-list refresh and this call) is uncaught and crashes the host app.
2. `getMediaControllerCallback`'s `onPlaybackStateChanged` (lines 140-161) resolves each custom action's icon with `ResourcesCompat.getDrawable(resources, ca.icon, null)!!` (lines 155-157) — if the target app's resource id can't be resolved (stale id, resource stripped, wrong package resources), `getDrawable` returns `null` and the `!!` crashes the host app inside a system-driven callback.

**Files:**
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:100-171`
- Test: `android/src/test/kotlin/com/alexmod/audio_control/AudioControlSetupMediaControllerTest.kt`

**Interfaces:**
- Consumes: `PackageManager.NameNotFoundException` (`android.content.pm.PackageManager`), `ResourcesCompat.getDrawable` (`androidx.core.content.res.ResourcesCompat`, existing dependency).
- Produces: `setupMediaController` returns `false` (instead of throwing) when resources can't be resolved for the target package; `onPlaybackStateChanged` emits a `MediaInfo` whose `customAction` entry for an unresolvable icon carries an empty `ByteArray` instead of crashing.

- [ ] **Step 1: Write the failing test for the `NameNotFoundException` path**

Create `android/src/test/kotlin/com/alexmod/audio_control/AudioControlSetupMediaControllerTest.kt`:

```kotlin
package com.alexmod.audio_control

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.media.session.MediaSessionCompat
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.reflect.Field

class AudioControlSetupMediaControllerTest {

    @Test
    fun `setupMediaController returns false when target app resources cannot be resolved`() {
        val audioControl = AudioControl()
        val packageManager = mock<PackageManager>()
        whenever(packageManager.getResourcesForApplication("com.example.uninstalled"))
            .thenThrow(PackageManager.NameNotFoundException("com.example.uninstalled"))
        val context = mock<Context>()
        whenever(context.packageManager).thenReturn(packageManager)

        val sessionToken = mock<MediaSessionCompat.Token>()
        val details = MediaAppDetails(
            packageName = "com.example.uninstalled",
            appName = "Uninstalled App",
            icon = null,
            banner = null,
            sessionToken = sessionToken,
        )
        val field: Field = AudioControl::class.java.getDeclaredField("activeMediaAppDetailsList")
        field.isAccessible = true
        field.set(audioControl, listOf(details))

        val result = audioControl.setupMediaController(
            context = context,
            packageName = "com.example.uninstalled",
            onStateChanged = {},
            onSessionDestroyed = {},
        )

        assertFalse(result)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.AudioControlSetupMediaControllerTest"`

Expected: FAIL — the test throws `PackageManager.NameNotFoundException` out of `setupMediaController` instead of returning `false`, because the current `catch` block only matches `RemoteException`.

- [ ] **Step 3: Catch the missing exception and guard the icon lookup**

Edit `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt`, replace `setupMediaController` (lines 100-132):

```kotlin
    fun setupMediaController(
        context: Context,
        packageName: String,
        onStateChanged: (MediaInfo) -> Unit,
        onSessionDestroyed: () -> Unit,
    ): Boolean {
        val mMediaAppDetails = activeMediaAppDetailsList.find {
                mediaAppDetails -> mediaAppDetails.packageName == packageName }
        val token = mMediaAppDetails?.sessionToken ?: return false
        return try {
            mediaController = MediaControllerCompat(context, token)
            mediaController?.let {
                val resources = context.packageManager.getResourcesForApplication(packageName)
                mCallback = getMediaControllerCallback(resources, onStateChanged, onSessionDestroyed)
                mediaController!!.registerCallback(mCallback)

                mCallback.onPlaybackStateChanged(mediaController!!.playbackState)
                mCallback.onMetadataChanged(mediaController!!.metadata)
            }
            true
        } catch (remoteException: RemoteException) {
            Log.e(
                TAG,
                "Failed to create MediaController from session token",
                remoteException
            )
            false
        } catch (notFoundException: PackageManager.NameNotFoundException) {
            Log.e(
                TAG,
                "Failed to resolve resources for $packageName",
                notFoundException
            )
            false
        }
    }
```

Then, in the same file, replace the custom-action icon resolution inside `getMediaControllerCallback`'s `onPlaybackStateChanged` (lines 154-158):

```kotlin
                    customAction = customAction.map { ca ->
                        val drawable = ResourcesCompat.getDrawable(resources, ca.icon, null)
                        val icon = if (drawable != null) BitmapUtils.convertDrawable(drawable) else ByteArray(0)
                        MediaInfo.customActionToHashMap(ca, icon)
                    },
```

Add the `PackageManager` import at the top of the file (next to the existing `android.content.pm.PackageManager` import — check first, it may already be imported transitively; if `PackageManager` is unresolved, add `import android.content.pm.PackageManager` under the existing `import android.content.*`-style imports at the top of `AudioControl.kt`).

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.AudioControlSetupMediaControllerTest"`

Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 5: Manually verify the icon-fallback path on device**

This path (unresolvable custom-action icon) needs a real `MediaControllerCompat`/`PlaybackStateCompat` from a live session, which isn't practical to unit test without a much heavier Robolectric shadow setup — verify it manually instead:
Run: `cd example && flutter run` (or use the `/run` skill), open a media app with custom actions (e.g. a podcast app with a "skip 30s" custom action) from the example app's player page, and confirm the app no longer crashes even if you temporarily hardcode `ca.icon` to an invalid id (e.g. `0`) to force the fallback branch, then revert that temporary change.

- [ ] **Step 6: Commit**

```bash
git add android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt android/src/test/kotlin/com/alexmod/audio_control/AudioControlSetupMediaControllerTest.kt
git commit -m "fix: don't crash on missing package resources or unresolvable custom-action icons"
```

---

## Task 5: Ship the `NotificationListenerService` manifest declaration with the plugin

The plugin only works if the host app's manifest declares `<service android:name="com.alexmod.audio_control.NotificationListener" ...>` with the `BIND_NOTIFICATION_LISTENER_SERVICE` permission and the `NotificationListenerService` intent-filter. Today that declaration exists **only** in `example/android/app/src/main/AndroidManifest.xml:27-34` — the plugin's own `android/src/main/AndroidManifest.xml:1-4` has no `<service>` entry, and the 6-line `README.md` says nothing about it. Any real consumer of this plugin (installed from `pubspec.yaml`, not working from this repo) will find `NotificationListener.isEnabled()` always false and `initialize()` looping back to the settings screen, with no clue why.

The fix: declare the `<service>` in the plugin's own manifest so Gradle's manifest merger adds it to every consuming app automatically, and document the one remaining manual step (granting the permission is a runtime user action, already handled by `AudioControl.init()`'s settings-intent).

**Files:**
- Modify: `android/src/main/AndroidManifest.xml:1-4`
- Modify: `example/android/app/src/main/AndroidManifest.xml:27-34`
- Modify: `README.md`

**Interfaces:**
- Produces: any app depending on `audio_control` gets the `NotificationListener` service registered automatically via manifest merge — no code interface change.

- [ ] **Step 1: Add the service declaration to the plugin manifest**

Edit `android/src/main/AndroidManifest.xml`, replace the full file:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />

    <application>
        <service android:name="com.alexmod.audio_control.NotificationListener"
            android:label="Media Notifications Listener"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

(`INSTALL_SHORTCUT` is dropped here — see Task 6, which removes it for its own reason; if Task 6 hasn't landed yet when you do this step, leave the existing `<uses-permission android:name="android.permission.INSTALL_SHORTCUT" />` line in place and let Task 6 remove it.)

- [ ] **Step 2: Remove the now-duplicate declaration from the example app**

Edit `example/android/app/src/main/AndroidManifest.xml`, remove lines 27-34 (the `<service>` block), leaving:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
   <application
        android:label="audio_control_example"
        android:name="${applicationName}"
        android:icon="@mipmap/ic_launcher">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@style/LaunchTheme"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:hardwareAccelerated="true"
            android:windowSoftInputMode="adjustResize">
            <meta-data
              android:name="io.flutter.embedding.android.NormalTheme"
              android:resource="@style/NormalTheme"
              />
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        <meta-data
            android:name="flutterEmbedding"
            android:value="2" />
    </application>
</manifest>
```

- [ ] **Step 3: Verify the merged manifest still contains the service**

Run:
```bash
cd example/android && ./gradlew :app:processDebugMainManifest
grep -A5 "com.alexmod.audio_control.NotificationListener" app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml
```

Expected: the `<service>` block for `com.alexmod.audio_control.NotificationListener` appears in the merged manifest output (the exact intermediate path may differ slightly by AGP version — if the `grep` finds nothing, run `find app/build -iname "AndroidManifest.xml" | xargs grep -l NotificationListener` to locate the merged file for this AGP version).

- [ ] **Step 4: Document the requirement in the README**

Edit `README.md`, add after the existing content:

```markdown
## Android setup

This plugin registers a `NotificationListenerService` automatically via manifest
merging — no manifest changes are required in the consuming app.

The user must still grant **Notification access** to your app manually
(Android Settings → Apps → Special app access → Notification access), because
this permission cannot be requested with a runtime permission dialog. Call
`AudioControl.instance.initialize()`; if the permission isn't granted yet, it
opens the system settings screen for the user and returns `false` — call it
again after the user grants access.
```

- [ ] **Step 5: Commit**

```bash
git add android/src/main/AndroidManifest.xml example/android/app/src/main/AndroidManifest.xml README.md
git commit -m "fix: ship NotificationListenerService manifest declaration with the plugin, not just the example app"
```

---

## Task 6: Tighten permissions — scoped `<queries>` instead of `QUERY_ALL_PACKAGES`, drop unused `INSTALL_SHORTCUT`

`android/src/main/AndroidManifest.xml` declares `QUERY_ALL_PACKAGES`, a Play Store "sensitive permission" requiring an explicit declaration form and justification, and `INSTALL_SHORTCUT`, which is unused anywhere in the codebase (confirmed: no `ShortcutManager`/`INSTALL_SHORTCUT`-related code exists) and is a no-op since API 26. `AudioControl.kt` also has a leftover wildcard import, `android.content.pm.LauncherApps.ShortcutQuery.*` (line 7), that isn't used either. The plugin only needs visibility into apps that expose `MediaBrowserServiceCompat.SERVICE_INTERFACE` (used in `AudioControl.getMediaApps`, `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:69`), which Android's package-visibility system (API 30+) supports via a scoped `<queries>` element — no broad permission needed.

**Files:**
- Modify: `android/src/main/AndroidManifest.xml`
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:1-21`

**Interfaces:**
- No code interface change — `getMediaApps()` keeps its existing signature and behavior for apps that declare a `MediaBrowserService`.

- [ ] **Step 1: Replace `QUERY_ALL_PACKAGES` with a scoped `<queries>` element and drop `INSTALL_SHORTCUT`**

Edit `android/src/main/AndroidManifest.xml` (this assumes Task 5 already landed and added the `<application>`/`<service>` block; if not, apply this on top of the original two-`uses-permission` file):

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <queries>
        <intent>
            <action android:name="android.media.browse.MediaBrowserService" />
        </intent>
    </queries>

    <application>
        <service android:name="com.alexmod.audio_control.NotificationListener"
            android:label="Media Notifications Listener"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

- [ ] **Step 2: Remove the dead wildcard import**

Edit `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt`, remove line 7:

```kotlin
import android.content.pm.LauncherApps.ShortcutQuery.*
```

- [ ] **Step 3: Verify the module still compiles**

Run: `cd example/android && ./gradlew :audio_control:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL` (proves the removed import wasn't actually used for anything).

- [ ] **Step 4: Manually verify `getMediaApps()` still finds media apps**

Package-visibility filtering is only enforced against the declared `<queries>` scope on real devices/Play-installed builds (API 30+); confirm on a device or emulator running API 30+:
Run: `cd example && flutter run`, open the dashboard page, and confirm the list of media apps (e.g. a music player installed on the test device) still appears — this exercises `AudioControlPlugin.onMethodCall("getMediaApps")` → `AudioControl.getMediaApps` → `context.packageManager.queryIntentServices(...)`.

- [ ] **Step 5: Commit**

```bash
git add android/src/main/AndroidManifest.xml android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt
git commit -m "fix: replace QUERY_ALL_PACKAGES with scoped <queries>, drop unused INSTALL_SHORTCUT and dead import"
```

---

## Task 7: Move `getMediaApps()` icon/banner conversion off the main thread

`AudioControl.getMediaApps` (`android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:67-90`) iterates every installed app exposing a `MediaBrowserService` and, for each one, decodes its icon (and banner, if present) into an `ARGB_8888` `Bitmap` and PNG-encodes it (`BitmapUtils.convertDrawable`, called from `MediaAppDetailsUtils.infoToMediaAppDetails`). This runs synchronously on whatever thread `AudioControlPlugin.onMethodCall` runs on — the Flutter platform (main UI) thread, since `MethodChannel.setMethodCallHandler` dispatches there by default. On a device with several media apps installed, this can visibly jank or ANR the host app.

**Files:**
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt:67-90`
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/AudioControlPlugin.kt:27-67`
- Test: `android/src/test/kotlin/com/alexmod/audio_control/AudioControlGetMediaAppsTest.kt`

**Interfaces:**
- Consumes: `kotlinx.coroutines.Dispatchers`, `withContext` (added in Task 1's `build.gradle` edit).
- Produces: `AudioControl.getMediaAppsAsync(context: Context, dispatcher: CoroutineDispatcher = Dispatchers.Default, callback: (List<HashMap<String, Any?>>) -> Unit)` — replaces the old synchronous `getMediaApps` return-value API. `AudioControlPlugin.onMethodCall`'s `"getMediaApps"` branch now calls this and completes `result` from the callback instead of returning synchronously.

- [ ] **Step 1: Write the failing test**

Create `android/src/test/kotlin/com/alexmod/audio_control/AudioControlGetMediaAppsTest.kt`:

```kotlin
package com.alexmod.audio_control

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AudioControlGetMediaAppsTest {

    @Test
    fun `getMediaAppsAsync completes with an empty list when no media apps are installed`() = runTest {
        val audioControl = AudioControl()
        val packageManager = mock<PackageManager>()
        whenever(
            packageManager.queryIntentServices(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.eq(PackageManager.GET_RESOLVED_FILTER),
            )
        ).thenReturn(emptyList<ResolveInfo>())
        val context = mock<Context>()
        whenever(context.packageManager).thenReturn(packageManager)

        var callbackResult: List<HashMap<String, Any?>>? = null
        audioControl.getMediaAppsAsync(context, StandardTestDispatcher(testScheduler)) { result ->
            callbackResult = result
        }
        testScheduler.advanceUntilIdle()

        assertTrue(callbackResult != null && callbackResult!!.isEmpty())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.AudioControlGetMediaAppsTest"`

Expected: FAIL to compile — `getMediaAppsAsync` doesn't exist yet.

- [ ] **Step 3: Replace `getMediaApps` with an async version**

Edit `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt`, replace `getMediaApps` (lines 67-90):

```kotlin
    fun getMediaAppsAsync(
        context: Context,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        onResult: (List<HashMap<String, Any?>>) -> Unit,
    ) {
        val mediaBrowserIntent = Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE)
        val packageManager = context.packageManager
        val services = packageManager.queryIntentServices(
            mediaBrowserIntent,
            PackageManager.GET_RESOLVED_FILTER
        )

        CoroutineScope(dispatcher).launch {
            val mediaApps = ArrayList<MediaAppDetails>()
            for (info in services) {
                mediaApps.add(
                    MediaAppDetailsUtils.infoToMediaAppDetails(
                        info.serviceInfo,
                        packageManager,
                        null
                    )
                )
            }
            mediaAppDetailsList = mediaApps
            val result = mediaAppDetailsList.map { mediaAppDetails -> mediaAppDetails.toHasMap() }
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
```

Add the coroutine imports at the top of `AudioControl.kt` (near the other `android.*`/`androidx.*` imports):

```kotlin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
```

- [ ] **Step 4: Update the plugin to use the async `Result`**

Edit `android/src/main/kotlin/com/alexmod/audio_control/AudioControlPlugin.kt`, replace the `"getMediaApps"` branch (line 36):

```kotlin
            "getMediaApps" -> audioControl.getMediaAppsAsync(mContext) { mediaApps ->
                result.success(mediaApps)
            }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.AudioControlGetMediaAppsTest"`

Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 6: Manually verify no behavior regression on device**

Run: `cd example && flutter run`, open the dashboard page, confirm the media app list still populates (same manual check as Task 6 Step 4) and the UI doesn't freeze while it loads.

- [ ] **Step 7: Commit**

```bash
git add android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt android/src/main/kotlin/com/alexmod/audio_control/AudioControlPlugin.kt android/src/test/kotlin/com/alexmod/audio_control/AudioControlGetMediaAppsTest.kt
git commit -m "perf: run getMediaApps icon/banner conversion off the main thread"
```

---

## Task 8: Minor cleanups

Three small, independent, low-risk fixes bundled into one task since none needs its own test cycle beyond a compile check and are too small to be worth separate reviewer gates.

**Files:**
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/AudioControl.kt` (multiple small spots)
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/BitmapUtils.kt:32`
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/AudioControlPlugin.kt:14`
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/MediaAppDetailsUtils.kt:15,17`
- Modify: `android/src/main/kotlin/com/alexmod/audio_control/NotificationListener.kt:9`

**Interfaces:** none — purely internal/cosmetic, no signature changes.

- [ ] **Step 1: Use the `Settings` constant instead of the hardcoded action string**

Edit `AudioControl.kt`, in `init()` (around line 40), replace:

```kotlin
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
```

with:

```kotlin
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
```

and add the import near the top of `AudioControl.kt`:

```kotlin
import android.provider.Settings
```

- [ ] **Step 2: Drop the misleading PNG "quality" argument**

Edit `BitmapUtils.kt` line 32, replace:

```kotlin
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
```

with:

```kotlin
            // PNG is lossless; the quality argument below is ignored by the platform but required by the API.
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
```

- [ ] **Step 3: Remove dead `@RequiresApi(LOLLIPOP)` annotations now that `minSdk` is 24**

`minSdkVersion 24` (Android 7.0) is already above `Build.VERSION_CODES.LOLLIPOP` (21), so these annotations can never trigger a lint warning and are dead weight. Remove:
- `@RequiresApi(Build.VERSION_CODES.LOLLIPOP)` on `class AudioControl` in `AudioControl.kt` (line 22) and its now-unused `import androidx.annotation.RequiresApi` (line 18) if nothing else in the file uses `RequiresApi`.
- `@RequiresApi(Build.VERSION_CODES.LOLLIPOP)` on `class AudioControlPlugin` in `AudioControlPlugin.kt` (line 14) and its `import androidx.annotation.RequiresApi` (line 7) if unused elsewhere in that file.
- `@TargetApi(Build.VERSION_CODES.LOLLIPOP)` on `getMediaAppsFromControllers` in `MediaAppDetailsUtils.kt` (line 17) — keep the class-level `@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)` (line 15) as-is since `KITKAT_WATCH` (20) is also below `minSdk 24`, so it's equally dead, but leaving one file's cleanup scope smaller reduces risk — remove it too if the diff review agrees it's safe.
- `@TargetApi(VERSION_CODES.LOLLIPOP)` on `NotificationListener` in `NotificationListener.kt` (line 9).

- [ ] **Step 4: Verify the module still compiles**

Run: `cd example/android && ./gradlew :audio_control:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run the full native test suite to confirm no regression**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`, all tests from Tasks 1-7 still passing.

- [ ] **Step 6: Commit**

```bash
git add android/src/main/kotlin/com/alexmod/audio_control/
git commit -m "chore: drop dead API-level annotations, use Settings constant, fix misleading PNG quality arg"
```

---

## Task 9: Regression tests for the serialization layer (`MediaInfo`, `MediaAppDetails`, `BitmapUtils`)

These three files carry the entire native→Dart data contract (`toHasMap()` on both models, `customActionToHashMap`, and the drawable→`ByteArray` conversion the other two depend on) and have zero test coverage today. Add tests now, after the fixes above, so future changes to the channel payload shape have a regression net.

**Files:**
- Test: `android/src/test/kotlin/com/alexmod/audio_control/MediaInfoTest.kt`
- Test: `android/src/test/kotlin/com/alexmod/audio_control/MediaAppDetailsTest.kt`
- Test: `android/src/test/kotlin/com/alexmod/audio_control/BitmapUtilsTest.kt`

**Interfaces:**
- Consumes: `MediaInfo.toHasMap()`, `MediaInfo.customActionToHashMap()` (`android/src/main/kotlin/com/alexmod/audio_control/MediaInfo.kt`), `MediaAppDetails.toHasMap()` (`android/src/main/kotlin/com/alexmod/audio_control/MediaAppDetails.kt`), `BitmapUtils.bitmapToByteArray()`/`convertDrawable()` (`android/src/main/kotlin/com/alexmod/audio_control/BitmapUtils.kt`) — all existing, unchanged signatures.

- [ ] **Step 1: Write `MediaInfoTest`**

Create `android/src/test/kotlin/com/alexmod/audio_control/MediaInfoTest.kt`:

```kotlin
package com.alexmod.audio_control

import android.support.v4.media.session.PlaybackStateCompat
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaInfoTest {

    @Test
    fun `toHasMap serializes all fields with the channel-contract keys`() {
        val customAction = MediaInfo.customActionToHashMap(
            PlaybackStateCompat.CustomAction.Builder("skip_30", "Skip 30s", 0).build(),
            byteArrayOf(1, 2, 3),
        )
        val mediaInfo = MediaInfo(
            title = "Title",
            artist = "Artist",
            album = "Album",
            image = byteArrayOf(4, 5, 6),
            state = PlaybackStateCompat.STATE_PLAYING,
            customAction = listOf(customAction),
        )

        val map = mediaInfo.toHasMap()

        assertEquals("Title", map["title"])
        assertEquals("Artist", map["artist"])
        assertEquals("Album", map["album"])
        assertEquals(byteArrayOf(4, 5, 6).toList(), (map["image"] as ByteArray).toList())
        assertEquals(PlaybackStateCompat.STATE_PLAYING, map["state"])
        assertEquals(listOf(customAction), map["customAction"])
    }

    @Test
    fun `customActionToHashMap serializes name, icon and action`() {
        val action = PlaybackStateCompat.CustomAction.Builder("skip_30", "Skip 30s", 0).build()

        val map = MediaInfo.customActionToHashMap(action, byteArrayOf(9))

        assertEquals("skip_30", map["name"])
        assertEquals(byteArrayOf(9).toList(), (map["icon"] as ByteArray).toList())
        assertEquals(action.action, map["action"])
    }
}
```

- [ ] **Step 2: Write `MediaAppDetailsTest`**

Create `android/src/test/kotlin/com/alexmod/audio_control/MediaAppDetailsTest.kt`:

```kotlin
package com.alexmod.audio_control

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MediaAppDetailsTest {

    @Test
    fun `toHasMap serializes packageName, appName, icon and banner but not the session token`() {
        val details = MediaAppDetails(
            packageName = "com.example.app",
            appName = "Example App",
            icon = byteArrayOf(1),
            banner = byteArrayOf(2),
            sessionToken = null,
        )

        val map = details.toHasMap()

        assertEquals("com.example.app", map["packageName"])
        assertEquals("Example App", map["appName"])
        assertEquals(byteArrayOf(1).toList(), (map["icon"] as ByteArray).toList())
        assertEquals(byteArrayOf(2).toList(), (map["banner"] as ByteArray).toList())
        assertFalse(map.containsKey("sessionToken"))
    }
}
```

- [ ] **Step 3: Write `BitmapUtilsTest` (needs Robolectric for real `Bitmap`/`Canvas`/`Drawable` behavior)**

Create `android/src/test/kotlin/com/alexmod/audio_control/BitmapUtilsTest.kt`:

```kotlin
package com.alexmod.audio_control

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BitmapUtilsTest {

    @Test
    fun `bitmapToByteArray produces a non-empty PNG-encoded array`() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

        val bytes = BitmapUtils.bitmapToByteArray(bitmap)

        assertTrue(bytes.isNotEmpty())
        // PNG magic number: 0x89 'P' 'N' 'G'
        assertTrue(bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte())
    }

    @Test
    fun `convertDrawable handles a BitmapDrawable by reusing its bitmap`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val drawable = BitmapDrawable(context.resources, bitmap)

        val bytes = BitmapUtils.convertDrawable(drawable)

        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun `convertDrawable rasterizes a non-bitmap drawable`() {
        val drawable = ColorDrawable(android.graphics.Color.RED)
        drawable.setBounds(0, 0, 4, 4)

        val bytes = BitmapUtils.convertDrawable(drawable)

        assertTrue(bytes.isNotEmpty())
    }
}
```

If `androidx.test:core` isn't already resolvable, add it to Task 1's `build.gradle` test dependencies: `testImplementation 'androidx.test:core:1.6.1'`.

- [ ] **Step 4: Run all three test files**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest --tests "com.alexmod.audio_control.MediaInfoTest" --tests "com.alexmod.audio_control.MediaAppDetailsTest" --tests "com.alexmod.audio_control.BitmapUtilsTest"`

Expected: `BUILD SUCCESSFUL`, all 5 tests (2 + 1 + 3, minus any you trimmed) passed. These should pass immediately since they test existing, already-correct behavior — this task is pure regression-net-building, not a bug fix.

- [ ] **Step 5: Run the entire native suite one last time**

Run: `cd example/android && ./gradlew :audio_control:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`, every test added across Tasks 1-9 passes together.

- [ ] **Step 6: Commit**

```bash
git add android/src/test/kotlin/com/alexmod/audio_control/MediaInfoTest.kt android/src/test/kotlin/com/alexmod/audio_control/MediaAppDetailsTest.kt android/src/test/kotlin/com/alexmod/audio_control/BitmapUtilsTest.kt android/build.gradle
git commit -m "test: add regression tests for MediaInfo, MediaAppDetails and BitmapUtils serialization"
```

---

## Final verification (run once, after all tasks land)

- [ ] Run `cd example/android && ./gradlew :audio_control:testDebugUnitTest` — all native tests pass.
- [ ] Run `flutter analyze` from the repo root — no new warnings.
- [ ] Run `flutter test` from the repo root — existing Dart suite still green.
- [ ] Run `cd example && flutter test` — example app's widget test still green.
- [ ] Run `cd example && flutter run` on a real device/emulator, exercise: permission grant flow, media app list, controlling an active session (play/pause/skip/seek), a custom action, and disconnecting/uninstalling the controlled app mid-session (to hit `onSessionDestroyed`).
