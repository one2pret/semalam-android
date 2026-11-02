package dev.one2pret.semalam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AutoStopSettingsTest {
    private val zone = TimeZone.getTimeZone("Asia/Jakarta")
    private fun time(day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(zone).apply {
            set(2026, Calendar.NOVEMBER, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun midnightToSixAmSameDay() {
        assertEquals(time(3,6,0), AutoStopSettings.nextClockTime(time(3,0,0),6,0,zone))
    }
    @Test fun stopAtSixNextMorning() {
        assertEquals(time(3,6,0), AutoStopSettings.nextClockTime(time(2,23,0),6,0,zone))
    }
    @Test fun passedClockTimeMeansTomorrow() {
        assertEquals(time(4,6,0), AutoStopSettings.nextClockTime(time(3,6,1),6,0,zone))
    }
    @Test fun equalClockTimeMeansTomorrow() {
        assertEquals(time(4,6,0), AutoStopSettings.nextClockTime(time(3,6,0),6,0,zone))
    }
    @Test fun labelsAreNotAutoClassified() {
        val values = listOf("Belum dilabeli", "Mirip berbicara", "Mirip dengkuran",
            "Suara sekitar", "Suara keras dari luar", "Batuk / suara tubuh", "Tidak yakin")
        val s = SoundDashboard.summarize(values)
        assertEquals(7,s.total)
        assertEquals(2,s.unreviewed)
        assertEquals(1,s.sleepTalking)
        assertEquals(1,s.snoring)
        assertEquals(2,s.environment)
        assertEquals(1,s.bodySound)
        assertEquals(0,s.other)
        assertTrue(SoundDashboard.matchesFilter("Suara keras dari luar","Lingkungan"))
    }
}
