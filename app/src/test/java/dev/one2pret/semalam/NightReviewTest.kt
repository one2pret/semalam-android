package dev.one2pret.semalam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NightReviewTest {
    private fun clip(id: String, offset: Long, duration: Long = 3_000L): SoundClip =
        SoundClip(id, "clip_$id.wav", offset, duration)

    @Test fun groupsNearbyClipsWithoutChangingOriginals() {
        val a = clip("a", 0)
        val b = clip("b", 15_000)
        val c = clip("c", 66_000)
        val input = listOf(c, b, a)
        val groups = NightReview.group(input, mapOf("a" to .2f, "b" to .8f, "c" to .3f))
        assertEquals(2, groups.size)
        assertEquals(listOf("a", "b"), groups[0].clipIds)
        assertEquals(listOf("c"), groups[1].clipIds)
        assertEquals(.8f, groups[0].maxSampledAmplitude, .001f)
        assertEquals(listOf(c, b, a), input)
    }

    @Test fun twoMinutesCapsGroupLengthEvenWithManyContinuousEvents() {
        val events = (0..12).map { clip("id$it", it * 12_000L) }
        val groups = NightReview.group(events, emptyMap())
        assertTrue(groups.size >= 2)
        assertTrue(groups.all { it.lastOffsetMs - it.firstOffsetMs <= NightReview.MAX_GROUP_SPAN_MS })
    }

    @Test fun quarterHourBinsKeepQuietTimeVisible() {
        val bins = NightReview.bins(
            listOf(clip("a", 5_000), clip("b", 930_000)), mapOf("a" to .7f),
            sessionDurationMs = 45 * 60_000L
        )
        assertEquals(3, bins.size)
        assertEquals(1, bins[0].eventCount)
        assertEquals(1, bins[1].eventCount)
        assertEquals(0, bins[2].eventCount)
        assertEquals(0f, bins[2].peak, .001f)
    }

    @Test fun missingSampledAmplitudeCannotImplyLoudSound() {
        val g = NightReview.group(listOf(clip("a", 0L)), emptyMap()).single()
        assertEquals(0f, g.maxSampledAmplitude, .001f)
        assertTrue(g.prominentScore < 20)
    }
}
