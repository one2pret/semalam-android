package dev.one2pret.semalam

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.Calendar

internal object BedtimeSchedule {
    private const val PREF = "bedtime"
    private const val ENABLED = "enabled"
    private const val HOUR = "hour"
    private const val MINUTE = "minute"
    const val ACTION_BEDTIME = "dev.one2pret.semalam.BEDTIME"
    const val ACTION_SNOOZE = "dev.one2pret.semalam.SNOOZE"

    fun isEnabled(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(ENABLED, false)
    fun hour(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(HOUR, 22)
    fun minute(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(MINUTE, 30)

    fun update(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED, enabled)
            .putInt(HOUR, hour)
            .putInt(MINUTE, minute)
            .apply()
        reschedule(context)
    }

    fun reschedule(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            context, 201, Intent(context, BedtimeReminderReceiver::class.java).setAction(ACTION_BEDTIME),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        manager.cancel(intent)
        if (!isEnabled(context)) return
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour(context))
            set(Calendar.MINUTE, minute(context))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
        }
        // Inexact and battery-friendly: may arrive later, especially in Doze mode.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, intent)
    }

    fun snooze(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            context, 202, Intent(context, BedtimeReminderReceiver::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 15 * 60_000L, intent)
    }
}

class BedtimeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!BedtimeSchedule.isEnabled(context)) return
        if (intent?.action == BedtimeSchedule.ACTION_BEDTIME) BedtimeSchedule.reschedule(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel("semalam_bedtime", "Pengingat jam tidur", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 203, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snooze = PendingIntent.getBroadcast(
            context, 204, Intent(context, BedtimeReminderReceiver::class.java).setAction("dev.one2pret.semalam.SNOOZE_CLICK"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (intent?.action == "dev.one2pret.semalam.SNOOZE_CLICK") {
            BedtimeSchedule.snooze(context)
            manager.cancel(202)
            return
        }
        NotificationManagerCompat.from(context).notify(
            202,
            NotificationCompat.Builder(context, "semalam_bedtime")
                .setSmallIcon(R.drawable.ic_semalam)
                .setContentTitle("Sudah waktunya istirahat?")
                .setContentText("Buka Semalam untuk mulai sesi suara tidur.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .addAction(0, "Tunda 15 menit", snooze)
                .build()
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            BedtimeSchedule.reschedule(context)
        }
    }
}
