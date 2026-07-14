package com.alexmod.audio_control

import android.content.res.Resources
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock
import java.lang.reflect.Field
import java.lang.reflect.Method

class AudioControlPlaybackCallbackRaceTest {

    @Test
    fun `onPlaybackStateChanged does not crash when mediaController has been cleared concurrently`() {
        val audioControl = AudioControl()
        val controller = mock<MediaControllerCompat>()

        val controllerField: Field = AudioControl::class.java.getDeclaredField("mediaController")
        controllerField.isAccessible = true
        controllerField.set(audioControl, controller)

        val getCallbackMethod: Method = AudioControl::class.java.getDeclaredMethod(
            "getMediaControllerCallback",
            Resources::class.java,
            Function1::class.java,
            Function0::class.java,
        )
        getCallbackMethod.isAccessible = true

        var stateChangedCalled = false
        val callback = getCallbackMethod.invoke(
            audioControl,
            mock<Resources>(),
            { _: MediaInfo -> stateChangedCalled = true },
            {},
        ) as MediaControllerCompat.Callback

        // Simulate onStop() nulling the controller from the main thread right
        // before this binder-thread callback fires.
        controllerField.set(audioControl, null)

        val playbackState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
            .build()

        callback.onPlaybackStateChanged(playbackState)

        assertFalse(stateChangedCalled)
    }
}
