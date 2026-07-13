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
