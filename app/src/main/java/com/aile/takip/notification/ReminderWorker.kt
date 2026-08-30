package com.aile.takip.notification

import android.content.Context
import androidx.work.*
import com.aile.takip.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "reminder_check"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                15, TimeUnit.MINUTES  // Her 15 dakikada bir kontrol et
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelPeriodicCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.get(applicationContext)
                val notificationHelper = NotificationHelper(applicationContext)
                val now = System.currentTimeMillis()

                // Check active reminders that are due
                val dueReminders = db.reminderDao().getDueReminders(now)
                for (reminder in dueReminders) {
                    notificationHelper.showReminderNotification(
                        notificationId = reminder.id.hashCode(),
                        title = reminder.title,
                        message = reminder.description.ifEmpty { "Hatırlatma zamanı geldi!" },
                        priority = reminder.priority,
                        alarmSound = reminder.alarmSound,
                        vibrate = reminder.vibrate
                    )

                    // Handle repeat reminders
                    if (reminder.repeatType != "once") {
                        val nextTime = calculateNextFireTime(reminder)
                        if (nextTime > 0) {
                            db.reminderDao().upsert(
                                reminder.copy(
                                    lastFiredAt = now,
                                    nextFireAt = nextTime,
                                    reminderTime = nextTime
                                )
                            )
                        } else {
                            // Repeat ended, mark as completed
                            db.reminderDao().upsert(reminder.copy(isCompleted = true))
                        }
                    } else {
                        // One-time reminder, mark as completed
                        db.reminderDao().upsert(reminder.copy(isCompleted = true))
                    }
                }

                // Check snoozed reminders that are now due
                val snoozedReminders = db.reminderDao().getSnoozedReminders(now)
                for (reminder in snoozedReminders) {
                    // Clear snooze and show notification
                    db.reminderDao().upsert(reminder.copy(isSnoozed = false, snoozeUntil = 0L))
                    notificationHelper.showReminderNotification(
                        notificationId = reminder.id.hashCode(),
                        title = reminder.title,
                        message = reminder.description.ifEmpty { "Ertelenen hatırlatma zamanı!" },
                        priority = reminder.priority,
                        alarmSound = reminder.alarmSound,
                        vibrate = reminder.vibrate
                    )
                }

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private fun calculateNextFireTime(reminder: com.aile.takip.data.model.Reminder): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = reminder.reminderTime
        }

        when (reminder.repeatType) {
            "daily" -> {
                calendar.add(Calendar.DAY_OF_MONTH, reminder.repeatInterval)
            }
            "weekly" -> {
                if (reminder.repeatDays.isNotEmpty()) {
                    // Custom days: find next matching day
                    val days = reminder.repeatDays.split(",").map { it.toInt() }
                    val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                    val nextDay = days.firstOrNull { it > currentDay } ?: days.min()
                    val daysToAdd = if (nextDay > currentDay) {
                        nextDay - currentDay
                    } else {
                        7 - currentDay + nextDay
                    }
                    calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
                } else {
                    calendar.add(Calendar.WEEK_OF_YEAR, reminder.repeatInterval)
                }
            }
            "monthly" -> {
                calendar.add(Calendar.MONTH, reminder.repeatInterval)
            }
            "custom" -> {
                if (reminder.repeatDays.isNotEmpty()) {
                    val days = reminder.repeatDays.split(",").map { it.toInt() }
                    val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                    val nextDay = days.firstOrNull { it > currentDay } ?: days.min()
                    val daysToAdd = if (nextDay > currentDay) {
                        nextDay - currentDay
                    } else {
                        7 - currentDay + nextDay
                    }
                    calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
                } else {
                    calendar.add(Calendar.DAY_OF_MONTH, reminder.repeatInterval)
                }
            }
            else -> return 0  // No repeat
        }

        // Check if we've passed the end date
        if (reminder.repeatEndDate > 0 && calendar.timeInMillis > reminder.repeatEndDate) {
            return 0
        }

        return calendar.timeInMillis
    }
}
