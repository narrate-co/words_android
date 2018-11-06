package com.words.android

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.words.android.data.prefs.Preferences
import com.words.android.di.AppInjector
import com.words.android.di.UserComponent
import com.words.android.ui.auth.Auth
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import javax.inject.Inject

class App: Application(), HasActivityInjector{

    companion object {
        const val REINJECT_USER_BROADCAST_ACTION = "reinject_user_broadcast_action"
    }

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var userComponentBuilder: UserComponent.Builder


    fun setUser(auth: Auth?) {
        userComponentBuilder
                .user(auth?.user)
                .firebaseUser(auth?.firebaseUser)
                .build()
                .inject(this)
        dispatchReinjectUserBroadcast()
    }

    fun clearUser() {
        setUser(null)
        AppInjector.init(this)
        dispatchReinjectUserBroadcast()
    }

    fun updateDefaultNightMode() {
        val nightMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(Preferences.NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    override fun onCreate() {
        updateDefaultNightMode()
        super.onCreate()
        AppInjector.init(this)
    }

    private fun dispatchReinjectUserBroadcast() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(REINJECT_USER_BROADCAST_ACTION))
    }

    override fun activityInjector(): AndroidInjector<Activity> = dispatchingAndroidInjector

}

