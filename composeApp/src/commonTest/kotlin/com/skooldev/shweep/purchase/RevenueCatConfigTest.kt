package com.skooldev.shweep.purchase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

    /**
     * A release build that ships the Test Store key is rejected during App
     * Review, and a leftover placeholder silently disables purchases. Nothing
     * at runtime prevents either, because key selection depends only on the
     * build configuration, so it is pinned here instead.
     */
    @Test
    fun releaseKeysAreNeverTheTestStoreKey() {
        assertNotEquals(RevenueCatConfig.DEBUG_SDK_KEY, RevenueCatConfig.ANDROID_SDK_KEY)
        assertNotEquals(RevenueCatConfig.DEBUG_SDK_KEY, RevenueCatConfig.IOS_SDK_KEY)
    }

    @Test
    fun productionKeysAreNotPlaceholders() {
        for (key in listOf(RevenueCatConfig.ANDROID_SDK_KEY, RevenueCatConfig.IOS_SDK_KEY)) {
            assertTrue(
                PLACEHOLDER_MARKERS.none { key.contains(it, ignoreCase = true) },
                "SDK key still looks like a placeholder: $key"
            )
        }
    }

    @Test
    fun productionKeysMatchTheirPlatformPrefix() {
        assertTrue(
            RevenueCatConfig.ANDROID_SDK_KEY.startsWith("goog_"),
            "Android key must be a Google Play key"
        )
        assertTrue(
            RevenueCatConfig.IOS_SDK_KEY.startsWith("appl_"),
            "iOS key must be an App Store key"
        )
    }

    private companion object {
        val PLACEHOLDER_MARKERS = listOf("REPLACE", "TODO", "XXXX", "YOUR_KEY", "CHANGEME")
    }
}
