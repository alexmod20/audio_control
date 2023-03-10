package com.alexmod.audio_control

import android.content.ComponentName
import android.support.v4.media.session.MediaSessionCompat

class MediaAppDetails(
    val packageName: String,
    val appName: String,
    val icon: ByteArray?,
    val banner: ByteArray?,
    val sessionToken: MediaSessionCompat.Token?
) {

    fun toHasMap(): HashMap<String, Any?> = hashMapOf(
        "packageName" to packageName,
        "appName" to appName,
        "icon" to icon,
        "banner" to banner
    )
}