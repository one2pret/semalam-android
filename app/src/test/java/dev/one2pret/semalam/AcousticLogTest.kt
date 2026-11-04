package dev.one2pret.semalam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcousticLogTest {
    @Test fun bucketsAreFiveSecondsAndKeepPeak() {
        val acc = AcousticAccumulator()
        assertEquals(null, acc.accept(100, -55f))
        assertEquals(null, acc.accept(4999, -22f))
        val result = acc.accept(5000, -61f)!!
        assertEquals(0L, result.offsetMs)
        assertEquals(-38.5f, result.averageDbfs, .01f)
        assertEquals(-22f, result.peakDbfs, .01f)
        assertEquals(5000L, acc.finish()!!.offsetMs)
    }
    @Test fun normalizeIsBoundedAndQuietLowerThanLoud() {
        assertEquals(0f, AcousticLog.normalized(-95f), .001f)
        assertEquals(1f, AcousticLog.normalized(0f), .001f)
        assertTrue(AcousticLog.normalized(-65f) < AcousticLog.normalized(-23f))
    }
}
