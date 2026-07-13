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
