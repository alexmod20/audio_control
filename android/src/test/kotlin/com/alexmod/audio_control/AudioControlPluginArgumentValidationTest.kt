package com.alexmod.audio_control

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AudioControlPluginArgumentValidationTest {

    private fun attachedPlugin(): AudioControlPlugin {
        val plugin = AudioControlPlugin()
        val binding = mock<FlutterPlugin.FlutterPluginBinding>()
        whenever(binding.binaryMessenger).thenReturn(mock<BinaryMessenger>())
        whenever(binding.applicationContext).thenReturn(RuntimeEnvironment.getApplication())
        plugin.onAttachedToEngine(binding)
        return plugin
    }

    @Test
    fun `controlMediaApp with a non-String argument reports an error instead of throwing`() {
        val plugin = attachedPlugin()
        val call = MethodCall("controlMediaApp", 42)
        val result = mock<MethodChannel.Result>()

        plugin.onMethodCall(call, result)

        verify(result).error(eq("INVALID_ARGUMENT"), any(), eq(null))
    }

    @Test
    fun `sendAction with a non-Map argument reports an error instead of throwing`() {
        val plugin = attachedPlugin()
        val call = MethodCall("sendAction", "not-a-map")
        val result = mock<MethodChannel.Result>()

        plugin.onMethodCall(call, result)

        verify(result).error(eq("INVALID_ARGUMENT"), any(), eq(null))
    }
}
