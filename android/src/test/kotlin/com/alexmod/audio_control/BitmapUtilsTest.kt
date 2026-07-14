package com.alexmod.audio_control

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
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
        val drawable = ShapeDrawable(OvalShape())
        drawable.intrinsicWidth = 4
        drawable.intrinsicHeight = 4
        drawable.setBounds(0, 0, 4, 4)

        val bytes = BitmapUtils.convertDrawable(drawable)

        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun `convertDrawable caps oversized non-bitmap drawables to the maximum dimension`() {
        val drawable = ShapeDrawable(OvalShape())
        drawable.intrinsicWidth = 4096
        drawable.intrinsicHeight = 4096
        drawable.setBounds(0, 0, 4096, 4096)

        val bytes = BitmapUtils.convertDrawable(drawable)
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        assertTrue(decoded.width <= 512)
        assertTrue(decoded.height <= 512)
    }
}
