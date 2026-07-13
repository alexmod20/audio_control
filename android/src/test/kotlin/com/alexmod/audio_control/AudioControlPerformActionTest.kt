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
