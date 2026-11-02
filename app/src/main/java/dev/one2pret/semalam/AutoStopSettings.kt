package dev.one2pret.semalam

import android.content.Context
import java.util.Calendar
import java.util.TimeZone

/** Stop deadline applies only to a session already started by the user. */
internal object AutoStopSettings {
    private const val PREFS = "semalam_autostop"
    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false)
    fun hour(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("hour", 6)
    fun minute(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("minute", 0)
    fun save(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        require(hour in 0..23 && minute in 0..59)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", enabled).putInt("hour", hour).putInt("minute", minute).apply()
    }
    fun deadline(context: Context, startMillis: Long): Long? =
        if (enabled(context)) nextClockTime(startMillis, hour(context), minute(context)) else null

    fun nextClockTime(
        startMillis: Long, hour: Int, minute: Int,
        zone: TimeZone = TimeZone.getDefault()
    ): Long {
        require(hour in 0..23 && minute in 0..59)
        val calendar = Calendar.getInstance(zone).apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= startMillis) calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}
