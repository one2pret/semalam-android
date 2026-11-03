package dev.one2pret.semalam

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.max

/**
 * Preview of the current WAV clip. Reads small, evenly spaced PCM windows
 * without loading whole-night audio or holding the entire clip in memory.
 * WAV emitted by WavFiles: PCM16 mono, 16 kHz, standard 44-byte header.
 */
internal object WavWaveform {
    fun readPeaks(file: File, bars: Int = 72): List<Float> {
        if (bars !in 1..256 || !file.isFile) return emptyList()
        return try {
            RandomAccessFile(file, "r").use { wav ->
                if (wav.length() < 46L) return emptyList()
                val header = ByteArray(44)
                wav.readFully(header)
                if (String(header, 0, 4, Charsets.US_ASCII) != "RIFF" ||
                    String(header, 8, 4, Charsets.US_ASCII) != "WAVE" ||
                    String(header, 36, 4, Charsets.US_ASCII) != "data"
                ) return emptyList()
                val samples = (wav.length() - 44L) / 2L
                if (samples <= 0) return emptyList()
                (0 until bars).map { index ->
                    val blockStart = (samples * index / bars).coerceAtMost(samples - 1L)
                    val blockEnd = (samples * (index + 1L) / bars).coerceAtMost(samples)
                    val span = (blockEnd - blockStart).coerceAtLeast(1L)
                    val sampleCount = minOf(96L, span).toInt()
                    var peak = 0
                    for (j in 0 until sampleCount) {
                        val sampleIndex = blockStart + span * j / sampleCount
                        wav.seek(44L + sampleIndex * 2L)
                        val low = wav.readUnsignedByte()
                        val high = wav.readUnsignedByte()
                        val signed = ((high shl 8) or low).toShort().toInt()
                        peak = max(peak, abs(signed))
                    }
                    (peak.toFloat() / 32768f).coerceIn(0f, 1f)
                }
            }
        } catch (_: Exception) { emptyList() }
    }
}
