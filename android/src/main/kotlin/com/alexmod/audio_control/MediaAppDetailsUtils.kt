package com.alexmod.audio_control

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.content.res.Resources
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService

@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
object MediaAppDetailsUtils {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun getMediaAppsFromControllers(
        controllers: List<MediaController?>?,
        packageManager: PackageManager,
        resources: Resources
    ): List<MediaAppDetails> {
        val mediaApps = ArrayList<MediaAppDetails>()
        controllers?.let {
            for (controller in it) {
                val packageName = controller?.packageName
                val info: ApplicationInfo
                try {
                    info = packageManager.getApplicationInfo(packageName!!, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    // This should not happen. If we get a media session for a package, then the
                    // package must be installed on the device.
                    Log.e(ContentValues.TAG, "Unable to load package details", e)
                    continue
                }

                mediaApps.add(
                    infoToMediaAppDetails(info, packageManager, resources, controller.sessionToken)
                )
            }
        }
        return mediaApps
    }

    fun infoToMediaAppDetails(info: PackageItemInfo, pm: PackageManager, resources: Resources,
                                      token: MediaSession.Token?): MediaAppDetails {
        val packageName = info.packageName
        val appName = info.loadLabel(pm).toString()
        val appIcon = info.loadIcon(pm)
        val icon = BitmapUtils.convertDrawable(appIcon)
//        val icon = BitmapUtils.convertDrawableToBase64(appIcon)
        val appBanner = info.loadBanner(pm)
        val banner = appBanner?.let {
            BitmapUtils.convertDrawable(appBanner)
        }

        val sessionToken = if(token != null) {
            MediaSessionCompat.Token.fromToken(token)
        } else null
        val componentName = if(token == null) {
            ComponentName(info.packageName, info.name)
        } else null

        return MediaAppDetails(
            packageName = packageName,
            appName = appName,
            icon = icon,
            banner = banner,
            sessionToken = sessionToken,
            componentName = componentName
        )
    }
}