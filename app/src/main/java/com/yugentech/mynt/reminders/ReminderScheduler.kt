package com.yugentech.mynt.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yugentech.mynt.domain.model.NotificationConfig
import timber.log.Timber
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Keeps one daily alarm in step with the reminder settings. Inexact on purpose: a nudge
 * to log the day's spending doesn't need to fire on the second, and inexact alarms need
 * no special permission and are kinder to the battery.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "daily_reminders"
    private const val REQUEST_CODE = 4201
    internal const val EXTRA_PLAYFUL = "playful"

    fun apply(context: Context, config: NotificationConfig) {
        if (config.notificationsEnabled && config.dailyReminderEnabled) {
            schedule(context, config.reminderHour, config.reminderMinute, config.playfulRemindersEnabled)
        } else {
            cancel(context)
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A nudge to log the day's spending"
            }
        )
    }

    private fun schedule(context: Context, hour: Int, minute: Int, playful: Boolean) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = nextTrigger(hour, minute)

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent(context, playful)
            )
            Timber.d("Daily reminder scheduled for %02d:%02d", hour, minute)
        } catch (e: Exception) {
            Timber.e(e, "Error scheduling daily reminder alarm")
        }
    }

    private fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context, playful = false))
    }

    private fun pendingIntent(context: Context, playful: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_PLAYFUL, playful)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** The next time the clock reads [hour]:[minute]: later today, or tomorrow if that's passed. */
    internal fun nextTrigger(hour: Int, minute: Int, now: LocalDateTime = LocalDateTime.now()): Long {
        var next = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
