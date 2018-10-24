package com.words.android.data.prefs

/**
 * Preferences that depend on a valid uid
 *
 * These have to be able to wait until AuthActivity has finished to be
 * get or set
 */
object UserPreferences {
    const val HAS_SEEN_RECENTS_BANNER = "has_seen_recents_banner"
    const val HAS_SEEN_TRENDING_BANNER = "has_seen_trending_banner"
    const val HAS_SEEN_FAVORITES_BANNER = "has_seen_favorites_banner"
}

