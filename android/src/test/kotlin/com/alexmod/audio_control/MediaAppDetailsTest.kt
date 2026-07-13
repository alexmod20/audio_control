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
