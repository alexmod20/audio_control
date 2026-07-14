package com.alexmod.audio_control

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

class BitmapUtils {
    companion object {
        private const val MAX_DIMENSION = 512

        fun convertDrawable(
            drawable: Drawable
        ): ByteArray {
            if (drawable is BitmapDrawable) {
                return bitmapToByteArray(drawable.bitmap)
            }

            val width = drawable.intrinsicWidth.coerceIn(1, MAX_DIMENSION)
            val height = drawable.intrinsicHeight.coerceIn(1, MAX_DIMENSION)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)

            val bytes = bitmapToByteArray(bitmap)
            bitmap.recycle()
            return bytes
        }

        fun bitmapToByteArray(bitmap: Bitmap) : ByteArray{
            val stream = ByteArrayOutputStream()
            // PNG is lossless; the quality argument below is ignored by the platform but required by the API.
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }
    }
}
