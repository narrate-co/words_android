package com.wordsdict.android.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.lifecycle.LiveData

class UserPreferenceRepository(
        private val applicationContext: Context,
        userId: String? = null
) {


    private val sharedPrefs: SharedPreferences by lazy {
        if (userId == null) {
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        } else {
            applicationContext.getSharedPreferences(userId, Context.MODE_PRIVATE)
        }
    }

    fun resetAll() {
        hasSeenRecentsBanner = false
        hasSeenFavoritesBanner = false
        hasSeenTrendingBanner = false
        portraitToLandscapeOrientationChangeCount = 0L
        landscapeToPortraitOrientationChangeCount = 0L
    }


    var hasSeenRecentsBanner: Boolean by PreferenceDelegate(
            sharedPrefs,
            UserPreferences.HAS_SEEN_RECENTS_BANNER,
            false
    )
    val hasSeenRecentsBannerLive: LiveData<Boolean> =
        PreferenceLiveData(sharedPrefs, UserPreferences.HAS_SEEN_RECENTS_BANNER, false)


    var hasSeenFavoritesBanner: Boolean by PreferenceDelegate(
            sharedPrefs,
            UserPreferences.HAS_SEEN_FAVORITES_BANNER,
            false
    )
    val hasSeenFavoritesBannerLive: LiveData<Boolean> =
        PreferenceLiveData(sharedPrefs, UserPreferences.HAS_SEEN_FAVORITES_BANNER, false)


    var hasSeenTrendingBanner: Boolean by PreferenceDelegate(
            sharedPrefs,
            UserPreferences.HAS_SEEN_TRENDING_BANNER,
            false
    )
    val hasSeenTrendingBannerLive: LiveData<Boolean> =
        PreferenceLiveData(sharedPrefs, UserPreferences.HAS_SEEN_TRENDING_BANNER, false)

    var portraitToLandscapeOrientationChangeCount: Long by PreferenceDelegate(
            sharedPrefs,
            UserPreferences.PORTRAIT_TO_LANDSCAPE_ORIENTATION_CHANGE_COUNT,
            0L
    )
    
    var landscapeToPortraitOrientationChangeCount: Long by PreferenceDelegate(
            sharedPrefs,
            UserPreferences.LANDSCAPE_TO_PORTRAIT_ORIENTATION_CHANGE_COUNT,
            0L
    )
    
}

