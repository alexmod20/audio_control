package com.alexmod.audio_control

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AudioControlGetMediaAppsTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getMediaAppsAsync completes with an empty list when no media apps are installed`() = runTest {
        val audioControl = AudioControl()
        val packageManager = mock<PackageManager>()
        whenever(
            packageManager.queryIntentServices(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.eq(PackageManager.GET_RESOLVED_FILTER),
            )
        ).thenReturn(emptyList<ResolveInfo>())
        val context = mock<Context>()
        whenever(context.packageManager).thenReturn(packageManager)

        var callbackResult: List<HashMap<String, Any?>>? = null
        audioControl.getMediaAppsAsync(context, StandardTestDispatcher(testScheduler)) { result ->
            callbackResult = result
        }
        testScheduler.advanceUntilIdle()

        assertTrue(callbackResult != null && callbackResult!!.isEmpty())
    }

    @Test
    fun `getMediaAppsAsync completes with an empty list when queryIntentServices throws`() = runTest {
        val audioControl = AudioControl()
        val packageManager = mock<PackageManager>()
        whenever(
            packageManager.queryIntentServices(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.eq(PackageManager.GET_RESOLVED_FILTER),
            )
        ).thenThrow(RuntimeException("Test exception"))
        val context = mock<Context>()
        whenever(context.packageManager).thenReturn(packageManager)

        var callbackResult: List<HashMap<String, Any?>>? = null
        audioControl.getMediaAppsAsync(context, StandardTestDispatcher(testScheduler)) { result ->
            callbackResult = result
        }
        testScheduler.advanceUntilIdle()

        assertTrue(callbackResult != null && callbackResult!!.isEmpty())
    }
}
