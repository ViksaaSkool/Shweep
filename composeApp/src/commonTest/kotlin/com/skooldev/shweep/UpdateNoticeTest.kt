package com.skooldev.shweep

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateNoticeTest {

    @Test
    fun freshInstallDoesNotShowNotice() {
        // No notice version has ever been recorded, so there is no update to announce.
        assertFalse(shouldShowUpdateNotice(null))
    }

    @Test
    fun notLoadedDoesNotShowNotice() {
        // The pre-load frame must not flash the dialog for a user who already dismissed it.
        assertFalse(shouldShowUpdateNotice(UPDATE_NOTICE_NOT_LOADED))
    }

    @Test
    fun alreadySeenCurrentVersionDoesNotShowNotice() {
        assertFalse(shouldShowUpdateNotice(FeatureFlags.UPDATE_NOTICE_VERSION))
    }

    @Test
    fun returningUserWhoSawAnOlderVersionIsShown() {
        assertTrue(shouldShowUpdateNotice("1.0.0"))
    }

    @Test
    fun noticeIsShownAgainAfterTheFlagIsBumped() {
        // Guards the flag-bump contract: a user who saw the current notice is re-notified once
        // UPDATE_NOTICE_VERSION moves past the value recorded on their device.
        assertFalse(shouldShowUpdateNotice(FeatureFlags.UPDATE_NOTICE_VERSION))
        assertTrue(shouldShowUpdateNotice("1.0.0"))
    }
}
