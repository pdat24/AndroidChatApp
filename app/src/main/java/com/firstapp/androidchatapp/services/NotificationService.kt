package com.firstapp.androidchatapp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.ui.activities.FriendRequestsActivity
import com.firstapp.androidchatapp.utils.Constants.Companion.NOTIFICATION_CHANNEL_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.NOTIFICATION_CHANnEL_NAME
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL

class NotificationService : FirebaseMessagingService() {

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("token: $token")
    }

    private fun createNotificationChannel() =
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANnEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableVibration(true)
            }
        )

    @SuppressLint("MissingPermission")
    private fun pushNotification(title: String, content: String, largeIcon: Bitmap) {
        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, FriendRequestsActivity::class.java),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        val notification = NotificationCompat
            .Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.cirlce_app_icon)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setLargeIcon(largeIcon)
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(content)
            .build()
        notificationManager.notify(1, notification)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        println("Received message")
        message.notification?.let {
            val url = URL(it.imageUrl!!.toString())
            val tmp = url.openConnection() as HttpURLConnection
            val largeIcon = BitmapFactory.decodeStream(tmp.inputStream)
            pushNotification(
                it.title!!,
                it.body!!,
                largeIcon
            )
        }
    }
}