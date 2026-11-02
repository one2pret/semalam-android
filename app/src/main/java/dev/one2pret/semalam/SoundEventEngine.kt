package dev.one2pret.semalam

import java.util.ArrayDeque
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * POC energy-based event segmenter. It detects changes in sound level, NOT snoring,
 * sleep talking, sleep stages, or individual speakers. No Android dependencies.
 */
class SoundEventEngine(
    private val sampleRate: Int = 16_000,
    private val maxClipSeconds: Int = 22,
    private val onEvent: (samples: ShortArray, offsetMs: Long, durationMs: Long) -> Unit,
) {
    private val calibrationLevels = ArrayList<Double>()
    private val preRoll = ArrayDeque<ShortArray>()
    private var currentEvent = ArrayList<ShortArray>()
    private var eventStartMs = 0L
    private var eventSamples = 0
    private var loudFrames = 0
    private var quietSamples = 0
    private var observedSamples = 0L
    private var thresholdDb: Double? = null

    val isCalibrated: Boolean get() = thresholdDb != null
    val threshold: Double? get() = thresholdDb

    fun accept(input: ShortArray, validSamples: Int = input.size) {
        if (validSamples <= 0) return
        val frame = input.copyOf(validSamples)
        val positionMs = observedSamples * 1000L / sampleRate
        observedSamples += validSamples
        val rms = sqrt(frame.fold(0.0) { sum, sample ->
            val normalized = sample.toDouble() / 32768.0
            sum + normalized * normalized
        } / validSamples)
        val db = 20.0 * log10(rms.coerceAtLeast(0.00001))

        if (thresholdDb == null) {
            calibrationLevels.add(db)
            if (observedSamples >= sampleRate * 12L) {
                val sorted = calibrationLevels.sorted()
                val background = sorted[sorted.size / 4]
                thresholdDb = (background + 14.0).coerceIn(-52.0, -28.0)
                calibrationLevels.clear()
            }
            rememberPreRoll(frame)
            return
        }

        val loud = db >= thresholdDb!!
        if (currentEvent.isEmpty()) {
            if (loud) {
                eventStartMs = (positionMs - preRoll.sumOf { it.size } * 1000L / sampleRate)
                    .coerceAtLeast(0L)
                currentEvent = ArrayList(preRoll)
                eventSamples = preRoll.sumOf { it.size }
                loudFrames = 0
                quietSamples = 0
                append(frame, loud)
            }
        } else {
            append(frame, loud)
            if (quietSamples >= sampleRate * 3 / 2 || eventSamples >= sampleRate * maxClipSeconds) {
                emitEvent()
            }
        }
        rememberPreRoll(frame)
    }

    fun finish() { emitEvent() }

    private fun append(frame: ShortArray, loud: Boolean) {
        currentEvent.add(frame)
        eventSamples += frame.size
        if (loud) {
            loudFrames++
            quietSamples = 0
        } else {
            quietSamples += frame.size
        }
    }

    private fun rememberPreRoll(frame: ShortArray) {
        preRoll.addLast(frame)
        var samples = preRoll.sumOf { it.size }
        while (samples > sampleRate && preRoll.isNotEmpty()) {
            samples -= preRoll.removeFirst().size
        }
    }

    private fun emitEvent() {
        if (currentEvent.isNotEmpty() && loudFrames >= 4 && eventSamples >= sampleRate / 2) {
            val output = ShortArray(eventSamples)
            var offset = 0
            currentEvent.forEach { segment ->
                segment.copyInto(output, offset)
                offset += segment.size
            }
            onEvent(output, eventStartMs, eventSamples * 1000L / sampleRate)
        }
        currentEvent.clear()
        eventSamples = 0
        loudFrames = 0
        quietSamples = 0
    }
}
