package com.aurevant.pkk

import android.app.Application
import com.aurevant.pkk.core.push.PushNotifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AurevantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PushNotifications.ensureChannel(this)
    }
}
