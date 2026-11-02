package dev.one2pret.semalam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundEventEngineTest {
    @Test fun detectsLoudEventAfterCalibration() {
        var received = 0
        var duration = 0L
        val detector = SoundEventEngine(onEvent = { _, _, d -> received++; duration = d })
        repeat(190) { detector.accept(ShortArray(1024) { 40 }) }
        assertTrue(detector.isCalibrated)
        assertEquals(100, detector.calibrationPercent)
        assertTrue(detector.lastInputDb != null)
        repeat(25) { detector.accept(ShortArray(1024) { 9_000 }) }
        repeat(30) { detector.accept(ShortArray(1024) { 40 }) }
        detector.finish()
        assertEquals(1, received)
        assertTrue(duration > 1_000)
    }

    @Test fun ignoresQuietRoomAfterCalibration() {
        var received = 0
        val detector = SoundEventEngine(onEvent = { _, _, _ -> received++ })
        repeat(400) { detector.accept(ShortArray(1024) { 40 }) }
        detector.finish()
        assertEquals(0, received)
    }

    @Test fun calibrationProgressStaysBelowHundredBeforeTwelveSeconds() {
        val detector = SoundEventEngine(onEvent = { _, _, _ -> })
        repeat(90) { detector.accept(ShortArray(1024) { 40 }) }
        assertTrue(detector.calibrationPercent in 1..99)
        assertTrue(!detector.isCalibrated)
    }

    @Test fun rejectsSingleLoudTransient() {
        var received = 0
        val detector = SoundEventEngine(onEvent = { _, _, _ -> received++ })
        repeat(190) { detector.accept(ShortArray(1024) { 40 }) }
        detector.accept(ShortArray(1024) { 9_000 })
        repeat(40) { detector.accept(ShortArray(1024) { 40 }) }
        detector.finish()
        assertEquals(0, received)
    }
}
