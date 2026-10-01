package com.skooldev.shweep

object FeatureFlags {
    /**
     * The app version whose first launch shows the one-time "what's changed" notice. Bump this only
     * when a release introduces user-visible changes that returning users should be told about.
     */
    const val UPDATE_NOTICE_VERSION = "2.0.0"
}

/**
 * Sentinel for the update-notice preference while DataStore has not emitted yet, so the notice
 * never flashes for a user who has already dismissed it.
 */
const val UPDATE_NOTICE_NOT_LOADED = "\u0000not-loaded"

/**
 * Decides whether the one-time "what's changed" notice should be shown.
 *
 * The notice is for *returning* users, so it is shown only when a previous notice version was
 * recorded and it differs from the current [FeatureFlags.UPDATE_NOTICE_VERSION]. The three
 * non-showing inputs are each deliberate:
 *
 *  - `null`: a fresh install has never run an earlier version, so there is no update to announce.
 *  - [UPDATE_NOTICE_NOT_LOADED]: DataStore has not emitted yet, so the value is not yet knowable.
 *  - [FeatureFlags.UPDATE_NOTICE_VERSION]: this release's notice was already dismissed.
 */
fun shouldShowUpdateNotice(seenUpdateNoticeVersion: String?): Boolean =
    seenUpdateNoticeVersion != null &&
        seenUpdateNoticeVersion != UPDATE_NOTICE_NOT_LOADED &&
        seenUpdateNoticeVersion != FeatureFlags.UPDATE_NOTICE_VERSION
