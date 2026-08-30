package com.aile.takip.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aile.takip.MainActivity
import com.aile.takip.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_REMINDERS = "reminders_channel"
        const val CHANNEL_ID_TASKS = "tasks_channel"
        const val CHANNEL_ID_BILLS = "bills_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Hatırlatıcılar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aile hatırlatıcıları ve etkinlikleri"
                enableVibration(true)
            }

            val tasksChannel = NotificationChannel(
                CHANNEL_ID_TASKS,
                "Görevler",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Görev hatırlatmaları"
            }

            val billsChannel = NotificationChannel(
                CHANNEL_ID_BILLS,
                "Faturalar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fatura ödeme hatırlatmaları"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(remindersChannel, tasksChannel, billsChannel)
            )
        }
    }

    fun showReminderNotification(
        notificationId: Int,
        title: String,
        message: String,
        priority: String = "orta",
        alarmSound: String = "default",
        vibrate: Boolean = true
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "reminders")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationPriority = when (priority) {
            "yüksek" -> NotificationCompat.PRIORITY_HIGH
            "düşük" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val soundUri = getAlarmSoundUri(alarmSound)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(notificationPriority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)

        if (vibrate) {
            builder.setVibrate(getVibrationPattern(priority))
        }

        if (alarmSound == "urgent") {
            builder.setOngoing(true)  // Kapatılamayan bildirim
        }

        val notification = builder.build()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun getAlarmSoundUri(soundType: String): Uri {
        return when (soundType) {
            "alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            "notification" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "ringtone" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            "urgent" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    private fun getVibrationPattern(priority: String): LongArray {
        return when (priority) {
            "yüksek" -> longArrayOf(0, 300, 200, 300, 200, 300)
            "düşük" -> longArrayOf(0, 200)
            else -> longArrayOf(0, 500, 200, 500)
        }
    }

    fun showTaskNotification(
        notificationId: Int,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "tasks")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASKS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun showBillNotification(
        notificationId: Int,
        title: String,
        message: String,
        amount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "invoices")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BILLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("$message - ₺${String.format("%.2f", amount)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
