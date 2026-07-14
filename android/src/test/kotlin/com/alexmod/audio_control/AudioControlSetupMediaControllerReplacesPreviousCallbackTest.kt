package com.alexmod.audio_control

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.lang.reflect.Field

class AudioControlSetupMediaControllerReplacesPreviousCallbackTest {

    @Test
    fun `setupMediaController unregisters the previous controller's callback before replacing it`() {
        val audioControl = AudioControl()
        val previousController = mock<MediaControllerCompat>()
        val previousCallback = mock<MediaControllerCompat.Callback>()

        val controllerField: Field = AudioControl::class.java.getDeclaredField("mediaController")
        controllerField.isAccessible = true
        controllerField.set(audioControl, previousController)

        val callbackField: Field = AudioControl::class.java.getDeclaredField("mCallback")
        callbackField.isAccessible = true
        callbackField.set(audioControl, previousCallback)

        val packageManager = mock<PackageManager>()
        whenever(packageManager.getResourcesForApplication("com.example.next"))
            .thenThrow(PackageManager.NameNotFoundException("com.example.next"))
        val context = mock<Context>()
        whenever(context.packageManager).thenReturn(packageManager)

        val sessionToken = mock<MediaSessionCompat.Token>()
        val details = MediaAppDetails(
            packageName = "com.example.next",
            appName = "Next App",
            icon = null,
            banner = null,
            sessionToken = sessionToken,
        )
        val activeListField: Field = AudioControl::class.java.getDeclaredField("activeMediaAppDetailsList")
        activeListField.isAccessible = true
        activeListField.set(audioControl, listOf(details))

        audioControl.setupMediaController(
            context = context,
            packageName = "com.example.next",
            onStateChanged = {},
            onSessionDestroyed = {},
        )

        verify(previousController).unregisterCallback(previousCallback)
    }
}
