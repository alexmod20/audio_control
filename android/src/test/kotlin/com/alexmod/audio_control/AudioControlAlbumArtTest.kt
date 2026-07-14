package com.alexmod.audio_control

import android.content.res.Resources
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.reflect.Method

class AudioControlAlbumArtTest {

    @Test
    fun `onPlaybackStateChanged does not crash when track has no embedded album art bitmap`() {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Track title")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Artist")
            .build()
        val playbackState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
            .build()

        val controller = mock<MediaControllerCompat>()
        whenever(controller.metadata).thenReturn(metadata)

        val audioControl = AudioControl()
        val controllerField = AudioControl::class.java.getDeclaredField("mediaController")
        controllerField.isAccessible = true
        controllerField.set(audioControl, controller)

        val getCallbackMethod: Method = AudioControl::class.java.getDeclaredMethod(
            "getMediaControllerCallback",
            Resources::class.java,
            Function1::class.java,
            Function0::class.java,
        )
        getCallbackMethod.isAccessible = true

        var receivedImage: ByteArray? = ByteArray(0)
        val onStateChanged: (MediaInfo) -> Unit = { mediaInfo -> receivedImage = mediaInfo.image }
        val onSessionDestroyed: () -> Unit = {}
        val callback = getCallbackMethod.invoke(
            audioControl,
            mock<Resources>(),
            onStateChanged,
            onSessionDestroyed,
        ) as MediaControllerCompat.Callback

        callback.onPlaybackStateChanged(playbackState)

        assertNull(receivedImage)
    }
}
