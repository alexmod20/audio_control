package com.alexmod.audio_control

import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi

class MediaInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    val image: ByteArray?,
    val state: Int,
    val customAction: List<HashMap<String, Any?>>?
) {
    fun toHasMap(): HashMap<String, Any?> = hashMapOf(
        "title" to title,
        "artist" to artist,
        "album" to album,
        "image" to image,
        "state" to state,
        "customAction" to customAction
    )

    companion object {
        fun customActionToHashMap(customAction: PlaybackStateCompat.CustomAction, icon: ByteArray) : HashMap<String, Any?> {
            return hashMapOf(
                "name" to customAction.name,
                "icon" to icon,
                "action" to customAction.action,
            )
        }
    }
}