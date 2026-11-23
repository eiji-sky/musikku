package com.musikku.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Service kecil supaya musik tetap jalan saat aplikasi di background / layar mati. */
class PlaybackService : Service() {
    private val channelId = "musikku_play"

    private val listener: () -> Unit = {
        if (Player.current != null) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, build())
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Pemutar musik", NotificationManager.IMPORTANCE_LOW)
        )
        Player.listeners.add(listener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            Player.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val n = build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, n)
        }
        return START_NOT_STICKY
    }

    private fun build(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, PlaybackService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val s = Player.current
        return Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(s?.title ?: "Musikku")
            .setContentText(s?.artist ?: "")
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        Player.listeners.remove(listener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
