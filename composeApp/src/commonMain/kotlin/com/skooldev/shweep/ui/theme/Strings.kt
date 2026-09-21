package com.skooldev.shweep.ui.theme

import com.skooldev.shweep.data.SHEEP_QUOTA_LIMIT
import com.skooldev.shweep.data.SHEEP_QUOTA_WINDOW_MILLIS

object Strings {
    // App Name
    const val APP_NAME = "Shweep"
    
    // Start Screen
    const val START_TAGLINE = "Use tech to go to sleep\nlike your ancestors"
    const val BUTTON_GO_TO_SLEEP = "Go to sleep"
    const val BUTTON_HISTORY = "Session history"
    
    // Counting Sheep Screen
    const val SWIPE_UP = "Swipe up"
    const val CD_COUNTING_BACKGROUND = "Night landscape background with mountains and moon"
    
    // History Dialog
    const val HISTORY_TITLE = "Session history"
    const val HISTORY_CLOSE = "✕"
    const val SHEEP_COUNT_FORMAT = "No of sheep: %d"
    
    // Content Descriptions
    const val CD_BACKGROUND_IMAGE = "Background image with sheep and landscape"
    
    // Toast Messages
    const val TOAST_SWIPED = "Swipped"
    
    // History
    const val NO_HISTORY = "No history"

    // Settings
    const val SETTINGS_TITLE = "Settings"
    const val SETTINGS_OPEN = "Open settings"
    const val SETTINGS_CLOSE = "Close settings"
    const val SHEEP_COLOR_TITLE = "Sheep color"
    const val SHEEP_COLOR_WHITE = "White sheep"
    const val SHEEP_COLOR_BLACK = "Black sheep"
    const val PRIVACY_POLICY = "Privacy Policy"
    const val TERMS_OF_SERVICE = "Terms of Service"
    const val LINK_OPENS_IN_BROWSER = "Opens in your browser"
    const val CD_GEAR = "Settings"
    const val SAVE = "Save"
    const val INVITE_FRIENDS = "Invite friends"
    const val SHARE_SHWEEP = "Share Shweep"

    // Sheep Color Dialog
    const val CHOOSE_SHEEP_TITLE = "Choose your sheep"
    const val CHOOSE_SHEEP_CONTINUE = "Continue"

    // Out of Sheep Dialog
    const val OUT_OF_SHEEP_TITLE = "No sheep left"
    const val OUT_OF_SHEEP_MESSAGE = "You've spent all 50 free sheep. Your flock returns in 24 hours, or you can make it unlimited forever."
    const val OUT_OF_SHEEP_TIMER_LABEL = "until your flock returns"
    const val WAIT_UNTIL_RESET = "Wait for my flock"

    // Unlimited Sheep Purchase
    const val UNLIMITED_SHEEP_TITLE = "Unlimited sheep"
    const val UNLIMITED_SHEEP_STATE_YES = "Yes"
    const val UNLIMITED_SHEEP_STATE_NO = "No"
    const val UNLIMITED_SHEEP_DESCRIPTION_DETAIL = "One-time purchase. Count as many sheep as you like, whenever you like. Restorable with the same store account."
    const val UNLIMITED_SHEEP_ACTIVE = "Active forever on this device"
    const val UNLIMITED_SHEEP_THANK_YOU = "Thank you for buying! Enjoy your winding-down flock - unlimited, forever."
    const val UNLIMITED_SHEEP_PURCHASE_TITLE = "Buy Unlimited Sheep"
    const val UNLIMITED_SHEEP_PURCHASE_LOADING = "Loading purchase..."
    const val UNLIMITED_SHEEP_PURCHASING = "Purchasing..."
    const val UNLIMITED_SHEEP_RESTORING = "Restoring..."
    const val UNLIMITED_SHEEP_UNAVAILABLE = "Purchase unavailable"
    const val RESTORE_PURCHASES = "Restore purchases"

    // Local test mode
    val LOCAL_TEST_MODE_MARKER: String
        get() = "Local test mode: $SHEEP_QUOTA_LIMIT sheep / ${SHEEP_QUOTA_WINDOW_MILLIS / 60_000} min"

    // History
    const val HISTORY_SUMMARY_TITLE = "Average wind-down time"
    const val HISTORY_ESTIMATE_NOTE = "Measured from time spent counting sheep"
    const val HISTORY_RECENT_NIGHTS = "Recent sessions"
    const val HISTORY_TOTAL_NIGHTS = "%d sessions"
    const val HISTORY_AVERAGE_SHEEP = "%d avg sheep"
    const val HISTORY_TIME_TO_SLEEP = "Wind-down time"
    const val HISTORY_SHEEP_COUNTED = "Sheep counted"
    const val HISTORY_STARTED_AT = "Started %s"
    const val HISTORY_DURATION_NOT_AVAILABLE = "Not available"
    const val HISTORY_EMPTY_TITLE = "No sessions recorded yet"
    const val HISTORY_EMPTY_MESSAGE = "Complete a sheep-counting session and your session history will appear here."
    const val HISTORY_SHEEP_COUNT = "%d sheep"
}
