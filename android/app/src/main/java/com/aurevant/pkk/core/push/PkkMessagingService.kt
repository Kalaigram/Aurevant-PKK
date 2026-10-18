package com.aurevant.pkk.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PkkMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushTokenSync: PushTokenSync

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch { pushTokenSync.sync() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Aurevant PKK"
        val body = message.notification?.body ?: message.data["body"]
        if (message.notification == null && message.data.isEmpty()) return

        PushNotifications.show(this, title, body, message.data["link"])
    }
}
