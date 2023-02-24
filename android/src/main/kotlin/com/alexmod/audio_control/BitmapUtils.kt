package com.alexmod.audio_control

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream

class BitmapUtils {
    /**
     * Converts a [Drawable] to an appropriately sized [Bitmap].
     *
     * @param resources Resources for the current [android.content.Context].
     * @param drawable  The [Drawable] to convert to a Bitmap.
     * @param downScale Will downscale the Bitmap to `R.dimen.app_icon_size` dp.
     * @return A Bitmap, no larger than `R.dimen.app_icon_size` dp if desired.
     */
    companion object {
        fun convertDrawable(
            drawable: Drawable
        ): ByteArray {
            val bitmap: Bitmap
            if (drawable is BitmapDrawable) {
                bitmap = drawable.bitmap
            } else {
                bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
            return bitmapToByteArray(bitmap)
        }

        fun convertDrawableToBase64(
            drawable: Drawable
        ): String {
            val bytes = convertDrawable(drawable);
            return Base64.encodeToString(bytes,Base64.DEFAULT);
        }

        fun bitmapToByteArray(bitmap: Bitmap) : ByteArray{
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            return stream.toByteArray()
        }

        /**
         * Creates a Material Design compliant [androidx.appcompat.widget.Toolbar] icon
         * from a given full sized icon.
         *
         * @param resources Resources for the current [android.content.Context].
         * @param icon      The bitmap to convert.
         * @return A scaled Bitmap of the appropriate size and in-built padding.
         */
        fun createToolbarIcon(
            resources: Resources,
            icon: Bitmap
        ): Bitmap? {
            val padding = 8 //resources.getDimensionPixelSize(R.dimen.margin_small)
            val iconSize = 24 //resources.getDimensionPixelSize(R.dimen.toolbar_icon_size)
            val sizeWithPadding = iconSize + 2 * padding

            // Create a Bitmap backed Canvas to be the toolbar icon.
            val toolbarIcon =
                Bitmap.createBitmap(sizeWithPadding, sizeWithPadding, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(toolbarIcon)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            // Resize the app icon to Material Design size.
            val scaledIcon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false)
            canvas.drawBitmap(scaledIcon, padding.toFloat(), padding.toFloat(), null)
            return toolbarIcon
        }
    }
}