package com.alexmod.audio_control

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION_CODES
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat

@TargetApi(VERSION_CODES.LOLLIPOP)
class NotificationListener : NotificationListenerService() {
    companion object {
        fun isEnabled(context: Context): Boolean {
            return NotificationManagerCompat
                .getEnabledListenerPackages(context)
                .contains(context.packageName)
        }
    }
}