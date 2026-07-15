package com.alexmod.audio_control

import android.support.v4.media.session.PlaybackStateCompat
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaInfoTest {

    @Test
    fun `toHasMap serializes all fields with the channel-contract keys`() {
        val customAction = MediaInfo.customActionToHashMap(
            PlaybackStateCompat.CustomAction.Builder("skip_30", "Skip 30s", 1).build(),
            byteArrayOf(1, 2, 3),
        )
        val mediaInfo = MediaInfo(
            title = "Title",
            artist = "Artist",
            album = "Album",
            image = byteArrayOf(4, 5, 6),
            state = PlaybackStateCompat.STATE_PLAYING,
            customAction = listOf(customAction),
            mediaId = "media-id-123",
            mediaUri = "content://media/track/123",
        )

        val map = mediaInfo.toHasMap()

        assertEquals("Title", map["title"])
        assertEquals("Artist", map["artist"])
        assertEquals("Album", map["album"])
        assertEquals(byteArrayOf(4, 5, 6).toList(), (map["image"] as ByteArray).toList())
        assertEquals(PlaybackStateCompat.STATE_PLAYING, map["state"])
        assertEquals(listOf(customAction), map["customAction"])
        assertEquals("media-id-123", map["mediaId"])
        assertEquals("content://media/track/123", map["mediaUri"])
    }

    @Test
    fun `customActionToHashMap serializes name, icon and action`() {
        val action = PlaybackStateCompat.CustomAction.Builder("skip_30", "Skip 30s", 1).build()

        val map = MediaInfo.customActionToHashMap(action, byteArrayOf(9))

        assertEquals("Skip 30s", map["name"])
        assertEquals(byteArrayOf(9).toList(), (map["icon"] as ByteArray).toList())
        assertEquals(action.action, map["action"])
    }
}
