package com.college.sportsmeet.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.college.sportsmeet.models.UpdateTokenRequest
import com.college.sportsmeet.ui.MatchLobbyActivity
import com.college.sportsmeet.utils.TokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles receiving FCM push notifications and automatic token refresh events.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when a new FCM token is generated for the device.
     * We must sync this securely with our PHP backend so it can target this user.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Only update if the user is currently logged in (we have an access token)
        if (TokenManager.getAccessToken() != null) {
            serviceScope.launch {
                try {
                    ApiClient.apiService.updateFcmToken(UpdateTokenRequest(token))
                } catch (e: Exception) {
                    // Log failure. In a real app, you might want to schedule a WorkManager job to retry.
                }
            }
        }
    }

    /**
     * Called when a message is received while the app is in the foreground,
     * or if the message contains a 'data' payload regardless of app state.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            val title = it.title ?: "Sports Meet Update"
            val body = it.body ?: ""
            sendNotification(title, body)
        }
    }

    /**
     * Builds and displays a highly visible Material Design notification.
     */
    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MatchLobbyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // Use FLAG_IMMUTABLE as required by modern Android security standards
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "campus_sports_alerts"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.star_on) // Using the requested placeholder icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since Android Oreo (API 26), a notification channel is strictly required.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Match & Grudge Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for live matches and campus rivalry challenges."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Use a unique ID (e.g. system time) so multiple notifications stack instead of replacing each other
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}