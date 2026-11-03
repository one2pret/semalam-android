package dev.one2pret.semalam

import kotlin.math.roundToInt

/**
 * Incident groups are VIRTUAL: each original clip and its JSON metadata stay untouched.
 * Peaks are sampled from existing WAV clips and indicate relative signal amplitude,
 * not clinical sleep severity or confirmation of snoring.
 */
internal data class NightIncident(
    val firstOffsetMs: Long,
    val lastOffsetMs: Long,
    val clipIds: List<String>,
    val maxSampledAmplitude: Float,
    val prominentScore: Int,
    val labeledClips: Int
)

internal data class NightHour(
    val startOffsetMs: Long,
    val endOffsetMs: Long,
    val eventCount: Int,
    val peak: Float
)

internal object NightReview {
    // Adjacent short clips are treated as one listening group without rewriting WAV.
    const val GROUP_GAP_MS = 30_000L
    const val MAX_GROUP_SPAN_MS = 120_000L
    const val BIN_MS = 15L * 60L * 1000L

    fun group(clips: List<SoundClip>, amplitude: Map<String, Float>): List<NightIncident> {
        val ordered = clips.sortedBy { it.offsetMs }
        if (ordered.isEmpty()) return emptyList()
        val groups = mutableListOf<List<SoundClip>>()
        var ongoing = mutableListOf<SoundClip>()
        var end = 0L
        var start = 0L
        ordered.forEach { clip ->
            val clipEnd = clip.offsetMs + clip.durationMs.coerceAtLeast(0)
            if (ongoing.isNotEmpty() && (clip.offsetMs - end > GROUP_GAP_MS ||
                        clipEnd - start > MAX_GROUP_SPAN_MS)) {
                groups.add(ongoing)
                ongoing = mutableListOf()
            }
            if (ongoing.isEmpty()) start = clip.offsetMs
            ongoing.add(clip)
            end = maxOf(end, clipEnd)
        }
        if (ongoing.isNotEmpty()) groups.add(ongoing)
        return groups.map { clipsInGroup ->
            val peak = clipsInGroup.maxOf { (amplitude[it.id] ?: 0f).coerceIn(0f, 1f) }
            val durationSec = clipsInGroup.sumOf { it.durationMs.coerceAtLeast(0L) } / 1000f
            // Relative, explainable score. 0 amplitude when sample isn't ready.
            val score = (peak * 65f + (durationSec / 60f).coerceIn(0f, 1f) * 20f +
                (clipsInGroup.size / 8f).coerceIn(0f, 1f) * 15f).roundToInt().coerceIn(0, 100)
            NightIncident(
                firstOffsetMs = clipsInGroup.first().offsetMs,
                lastOffsetMs = clipsInGroup.maxOf { it.offsetMs + it.durationMs.coerceAtLeast(0) },
                clipIds = clipsInGroup.map { it.id },
                maxSampledAmplitude = peak, prominentScore = score,
                labeledClips = clipsInGroup.count { it.label != "Belum dilabeli" && it.label != "Tidak yakin" }
            )
        }
    }

    fun bins(clips: List<SoundClip>, amplitude: Map<String, Float>, sessionDurationMs: Long): List<NightHour> {
        val count = ((sessionDurationMs.coerceAtLeast(1L) + BIN_MS - 1L) / BIN_MS)
            .coerceIn(1, 96).toInt()
        return (0 until count).map { index ->
            val start = index * BIN_MS
            val selected = clips.filter { it.offsetMs in start until (start + BIN_MS) }
            NightHour(
                startOffsetMs = start,
                endOffsetMs = minOf((index + 1L) * BIN_MS, sessionDurationMs.coerceAtLeast(1L)),
                eventCount = selected.size,
                peak = selected.maxOfOrNull { (amplitude[it.id] ?: 0f).coerceIn(0f, 1f) } ?: 0f
            )
        }
    }
}
