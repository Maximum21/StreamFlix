package com.asadraza.streamflix.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.asadraza.streamflix.MainActivity
import com.asadraza.streamflix.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * Notifications that arrive while the app is in the background are shown by the system. This
 * service only draws the ones that arrive while it is in the foreground.
 */
class StreamFlixMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        ensureChannel(this)

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val built = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(message.messageId.hashCode(), built)
        } catch (denied: SecurityException) {
            // POST_NOTIFICATIONS was revoked after the user opted in
            Timber.w(denied, "Notification dropped: permission not granted")
        }
    }

    // Delivery uses topic subscriptions, so the token itself is not stored anywhere
    override fun onNewToken(token: String) = Unit

    companion object {
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                context.getString(R.string.notification_channel_id),
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }
}
