package com.skooldev.shweep.purchase

import kotlin.test.Test
import kotlin.test.assertEquals

class RevenueCatConfigTest {

    @Test
    fun androidDebugUsesTestKey() {
        assertEquals(RevenueCatConfig.DEBUG_SDK_KEY, RevenueCatConfig.androidSdkKey(isDebug = true))
    }

    @Test
    fun androidReleaseUsesProductionKey() {
        assertEquals(RevenueCatConfig.ANDROID_SDK_KEY, RevenueCatConfig.androidSdkKey(isDebug = false))
    }

    @Test
    fun iosDebugUsesTestKey() {
        assertEquals(RevenueCatConfig.DEBUG_SDK_KEY, RevenueCatConfig.iosSdkKey(isDebug = true))
    }

    @Test
    fun iosReleaseUsesProductionKey() {
        assertEquals(RevenueCatConfig.IOS_SDK_KEY, RevenueCatConfig.iosSdkKey(isDebug = false))
    }
}
