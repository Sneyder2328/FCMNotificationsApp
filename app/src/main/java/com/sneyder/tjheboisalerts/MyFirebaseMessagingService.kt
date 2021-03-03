package com.sneyder.tjheboisalerts

import android.app.Notification
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sneyder.tjheboisalerts.MainActivity.Companion.CHANNEL_NAME


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FbMessagingService", "onMessageReceived()=${remoteMessage.data}")
        val title = remoteMessage.data["title"] ?: return
        displayNotification(title, CHANNEL_NAME)
    }

    private fun displayNotification(
        title: String,
        channel: String,
    ) {
        val notification = buildNotificationBig(
            context = this@MyFirebaseMessagingService,
            title = title,
            channel = channel
        )
        if (notification != null) {
            notificationManager().notify(System.currentTimeMillis().toInt(), notification)
        }
    }


    private fun buildNotificationBig(
        context: Context,
        title: String,
        channel: String
    ): Notification? = NotificationCompat.Builder(context, channel).apply {
        setSmallIcon(R.mipmap.ic_launcher)
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setContentTitle(title)
        setContentText(title)
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        setSound(alarmSound)
        val v = longArrayOf(200, 100)
        setVibrate(v)
        priority = NotificationCompat.PRIORITY_MAX
        setAutoCancel(true)
    }.build()

    override fun onNewToken(token: String) {
        Log.d("FbMessagingService", "onNewToken(token: String)=$token")
    }


}