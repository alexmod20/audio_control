package com.alexmod.audio_control

import android.content.ComponentName
import android.content.Context
import android.content.Context.MEDIA_SESSION_SERVICE
import android.content.Intent
import android.content.pm.LauncherApps.ShortcutQuery.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.session.MediaSessionManager
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.media.MediaBrowserServiceCompat

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AudioControl {
    private val TAG: String = AudioControl::class.java.simpleName
    private lateinit var listenerComponent: ComponentName
    private var mediaAppDetailsList = listOf<MediaAppDetails>()
    private var activeMediaAppDetailsList = listOf<MediaAppDetails>()
    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private lateinit var mCallback: MediaControllerCompat.Callback


//    fun onAppListUpdated(
//        mediaAppDetails: List<MediaAppDetails?>
//    ) {
//        if (mediaAppDetails.isEmpty()) {
//            return
//        }
//        //    mMediaSessionApps.setAppsList(mediaAppDetails)
//        Log.d("onAppListUpdated", mediaAppDetails.size.toString())
//    }

    private var mMediaSessionManager: MediaSessionManager? = null

    fun init(
        context: Context,
        onActiveSessionsChanged: ((List<HashMap<String, Any?>>) -> Unit)
    ): Boolean {
        if (!NotificationListener.isEnabled(context)) {
            context.startActivity(
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }

        mMediaSessionManager =
            context.getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager?
        listenerComponent = ComponentName(context, NotificationListener::class.java)

        val sessionsChangedListener =
            OnActiveSessionsChangedListener { list ->
                Log.d(TAG, "onActiveSessionsChanged: session is changed")
                activeMediaAppDetailsList = MediaAppDetailsUtils.getMediaAppsFromControllers(
                    list, context!!.packageManager, context.resources
                )
                onActiveSessionsChanged(activeMediaAppDetailsList.map { mediaAppDetails -> mediaAppDetails.toHasMap() })
            }
        mMediaSessionManager!!.addOnActiveSessionsChangedListener(
            sessionsChangedListener, listenerComponent
        )
        return true
    }

    fun isInit(context: Context): Boolean {
        return NotificationListener.isEnabled(context) && mMediaSessionManager != null
    }

    fun getMediaApps(context: Context): List<HashMap<String, Any?>> {
        val mediaApps = ArrayList<MediaAppDetails>()
        val mediaBrowserIntent = Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE)
        val packageManager = context.packageManager

        val services = context.packageManager.queryIntentServices(
            mediaBrowserIntent,
            PackageManager.GET_RESOLVED_FILTER
        )

        if (services.isNotEmpty()) {
            for (info in services) {
                mediaApps.add(
                    MediaAppDetailsUtils.infoToMediaAppDetails(
                        info.serviceInfo,
                        packageManager,
                        context.resources,
                        null
                    )
                )
            }
        }
        mediaAppDetailsList = mediaApps
        return mediaAppDetailsList.map { mediaAppDetails -> mediaAppDetails.toHasMap() }
    }

    fun getActiveSession(context: Context): List<HashMap<String, Any?>> {
        val list = mMediaSessionManager!!.getActiveSessions(listenerComponent)
        activeMediaAppDetailsList = MediaAppDetailsUtils.getMediaAppsFromControllers(
            list, context!!.packageManager, context.resources
        )
        return activeMediaAppDetailsList.map { mediaAppDetails -> mediaAppDetails.toHasMap() }
    }

    fun setupMediaController(
        context: Context,
        packageName: String,
        onStateChanged: (MediaInfo) -> Unit,
        onSessionDestroyed: () -> Unit,
    ): Boolean {
        val mMediaAppDetails = activeMediaAppDetailsList.find {
                mediaAppDetails -> mediaAppDetails.packageName == packageName }
        try {
            val token = mMediaAppDetails?.sessionToken
            if (token == null) {
//                token = mediaBrowser!!.sessionToken
                return false
            } else {
                mediaController = MediaControllerCompat(context, token)
                mediaController?.let {
                    val resources = context.packageManager.getResourcesForApplication(packageName)
                    mCallback = getMediaControllerCallback(resources, onStateChanged, onSessionDestroyed)
                    mediaController!!.registerCallback(mCallback)
//            mRatingUiHelper = ratingUiHelperFor(mediaController.ratingType)

                    // Force update on connect.
                    mCallback.onPlaybackStateChanged(mediaController!!.playbackState)
                    mCallback.onMetadataChanged(mediaController!!.metadata)
                }
            }
            return true
        } catch (remoteException: RemoteException) {
            Log.e(
                TAG,
                "Failed to create MediaController from session token",
                remoteException
            )
            return false
        }
    }

    private fun getMediaControllerCallback(
        resources: Resources,
        onStateChanged: (MediaInfo) -> Unit,
        onSessionDestroyed: () -> Unit,
    ): MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
                Log.d(TAG, "onPlaybackStateChanged: PlaybackState is changed")
                val mediaMetadata: MediaMetadataCompat = mediaController!!.metadata
                val actions = playbackState.actions;
                val customAction = playbackState.customActions
                val mediaInfo = MediaInfo(
                    title = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
                    artist = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST),
                    album = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM),
                    image = BitmapUtils.bitmapToByteArray(
                        mediaMetadata.getBitmap(
                            MediaMetadataCompat.METADATA_KEY_ALBUM_ART
                        )
                    ),
                    state = playbackState.state,
                    customAction = customAction.map{ ca -> MediaInfo.customActionToHashMap(ca, BitmapUtils.convertDrawable(
                        ResourcesCompat.getDrawable(
                            resources, ca.icon,  /* theme = */null
                        )!!
                        )) },
                )
                onStateChanged(mediaInfo)
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
//                onSessionDestroyed()
                Log.d(TAG, "onMetadataChanged: Metadata is changed")
            }

            override fun onSessionDestroyed() {
                onSessionDestroyed()
                Log.d(TAG, "MediaSession has been released")
            }
        }

    fun performAction(action: Int, seekTo: Int?) {
        mediaController?.let {
            it.transportControls?.apply {
                val actionId: Long = (1 shl action).toLong()
                when (actionId) {
                    PlaybackStateCompat.ACTION_STOP -> stop()
                    PlaybackStateCompat.ACTION_PLAY -> play()
                    PlaybackStateCompat.ACTION_PAUSE -> pause()
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT -> skipToNext()
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS -> skipToPrevious()
                    PlaybackStateCompat.ACTION_SEEK_TO -> {
                        seekTo?.let { seekTo ->
                            val position = it.playbackState.position
                            seekTo(position + 1000 * seekTo)
                        }
                    }
                }
            }
        }
    }

    fun performCustomAction(action: String) {
        mediaController?.let {
            it.transportControls.sendCustomAction(action, Bundle());
        }
    }

    fun onStart(context: Context) {
//            if (!NotificationListener.isEnabled(context!!)) {
//                mMediaSessionApps.setError(
//                    R.string.no_apps_found,
//                    R.string.no_apps_reason_missing_permission,
//                    R.string.action_notification_permissions_settings
//                ) { v ->
//                    startActivity(
//                        Intent(
//                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
//                        )
//                    )
//                }
//                return
//            }
        if (!NotificationListener.isEnabled(context!!)) {
            ContextCompat.startActivity(
                context!!,
                Intent(
                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                ),
                Bundle()
            )
            return
        }
        if (mMediaSessionManager == null) {
            return
        }
        val listenerComponent = ComponentName(context!!, NotificationListener::class.java)
//        mMediaSessionManager!!.addOnActiveSessionsChangedListener(
//            sessionsChangedListener, listenerComponent
//        )
        mMediaSessionManager!!.getActiveSessions(listenerComponent)
    }

    fun onStop() {
        if (mMediaSessionManager == null) {
            return
        }
//        mMediaSessionManager!!.removeOnActiveSessionsChangedListener(mSessionsChangedListener)

        mediaController?.let {
            it.unregisterCallback(mCallback)
            mediaController = null
        }

        mediaBrowser?.let {
            if (it.isConnected)
                it.disconnect()
        }
        mediaBrowser = null

        mediaAppDetailsList = listOf()
    }
}