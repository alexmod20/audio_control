package com.alexmod.audio_control

import android.media.session.MediaSessionManager
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.lang.reflect.Field

class AudioControlSessionListenerLifecycleTest {

    @Test
    fun `onStop removes the active-sessions-changed listener that init registered`() {
        val audioControl = AudioControl()
        val sessionManager = mock<MediaSessionManager>()

        val managerField: Field = AudioControl::class.java.getDeclaredField("mMediaSessionManager")
        managerField.isAccessible = true
        managerField.set(audioControl, sessionManager)

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { }
        val listenerField: Field = AudioControl::class.java.getDeclaredField("sessionsChangedListener")
        listenerField.isAccessible = true
        listenerField.set(audioControl, listener)

        audioControl.onStop()

        verify(sessionManager).removeOnActiveSessionsChangedListener(listener)
    }
}
