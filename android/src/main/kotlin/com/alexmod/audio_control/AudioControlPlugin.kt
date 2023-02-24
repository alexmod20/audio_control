package com.alexmod.audio_control

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** AudioControlPlugin */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AudioControlPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var mContext: Context
    private val audioControl = AudioControl()

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        mContext = flutterPluginBinding.applicationContext

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "audio_control")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> result.success("Android ${Build.VERSION.RELEASE}")
            "initialize" -> result.success(audioControl.init(
                mContext,
                {list -> channel.invokeMethod("sessionChanged", list); }
            ))
            "isInitialize" -> result.success(audioControl.isInit(mContext))
            "getActiveSession" -> result.success(audioControl.getActiveSession(mContext))
            "getMediaApps" -> result.success(audioControl.getMediaApps(mContext))
            "controlMediaApp" -> {
//                val args: ArrayList<Long> = call.arguments as ArrayList<Long>
//                val callbackHandle = args[0]
                result.success(audioControl.setupMediaController(
                    mContext,
                    call.arguments as String,
                    { mediaInfo -> channel.invokeMethod("stateChanged", mediaInfo.toHasMap()); },
                    { channel.invokeMethod("sessionDestroyed", null); }
                ));

//                result.success(
//                    audioControl.setupMedia(
//                        mContext,
//                        call.arguments as Int,
//                        { connectionSuccess -> connectionSuccess },
//                        { connectionError -> connectionError }
//                    )
//                )
            }
            "sendAction" -> {
                val args = call.arguments as HashMap<String, Int?>
                val action = args["action"]
                val seek = args["seek"]
                if(action == null) {
                    result.error("MISSING ARGUMENT", "Action argument missing", null)
                } else {
                    audioControl.performAction(action, seek)
                    result.success(true)
                }
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        audioControl.onStop()
    }
}
