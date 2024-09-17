package com.talkersdk.sample.messaging

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import network.talker.sdk.Talker
import network.talker.sdk.messaging.processTalkerFcm

// add this messaging service to your androidManifest.xml file.
// this service will be responsible for getting notifications from the server.
// kindly don't forget to handle permissions for notifications services and mentioned in the sample
// projects androidManifest file.

class TalkerFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
    }

    override fun handleIntent(intent: Intent?) {
        // pass the intent and talker sdk will handle playing and showing the notification audio message
        // just handle post notification permission and services permission and sdk will do the rest.
//        processTalkerFcm(intent, this)
        Talker.processTalkerFcm(intent, this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(
            "FCM",
            "FCM TOKEN : $token"
        )
    }
}