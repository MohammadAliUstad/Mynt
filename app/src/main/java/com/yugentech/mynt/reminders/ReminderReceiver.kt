package com.yugentech.mynt.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yugentech.mynt.MainActivity
import com.yugentech.mynt.R
import com.yugentech.mynt.user.datastore.UserDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val standardMessages = listOf(
    "Logged today's spending yet? It takes a few seconds by voice.",
    "Quick check-in: anything you spent today?",
    "Keep your streak going: add today's entries."
)

private val playfulMessages = listOf(
    "Your wallet called. It wants to know where the money went. 💸",
    "Coffee, cab, snacks? Spill the beans (and the amounts). ☕",
    "Future you says thanks for logging today. 🙌",
    "Tap the orb, say it out loud, done. Easy streak points. 🔥",
    "Money check! Ten seconds now saves a headache later. 🧾"
)

/** Shows the daily nudge; also re-arms the alarm after a reboot or an app update. */
class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val userDataStore: UserDataStore by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> rearm(context)
            else -> show(context, intent.getBooleanExtra(ReminderScheduler.EXTRA_PLAYFUL, false))
        }
    }

    private fun rearm(context: Context) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderScheduler.apply(context, userDataStore.settingsConfiguration.first().notificationConfig)
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, playful: Boolean) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ReminderScheduler.ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = (if (playful) playfulMessages else standardMessages).random()
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mynt")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post; nothing to do.
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 4202
    }
}
