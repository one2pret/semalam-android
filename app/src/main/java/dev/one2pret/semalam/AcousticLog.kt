package dev.one2pret.semalam

import java.io.File
import java.io.FileOutputStream
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import kotlin.math.roundToInt

/** One measurement every 5 seconds; never stores ambient PCM audio. */
internal data class AcousticPoint(
    val offsetMs: Long,
    val averageDbfs: Float,
    val peakDbfs: Float
)

internal object AcousticLog {
    const val INTERVAL_MS = 5_000L
    private const val MAX_POINTS = 8_640

    fun appendBatch(file: File, points: List<AcousticPoint>) {
        if (points.isEmpty()) return
        file.parentFile?.mkdirs()
        FileOutputStream(file, true).use { stream ->
            BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { out ->
                for (point in points) {
                    out.append(point.offsetMs.toString()).append(',')
                        .append(point.averageDbfs.toString()).append(',')
                        .append(point.peakDbfs.toString()).append('\n')
                }
                out.flush()
                stream.fd.sync()
            }
        }
    }

    fun read(file: File): List<AcousticPoint> {
        if (!file.isFile || file.length() > 600_000L) return emptyList()
        return try {
            file.useLines(Charsets.UTF_8) { lines ->
                lines.take(MAX_POINTS + 2).mapNotNull { line ->
                    val parts = line.split(',')
                    if (parts.size != 3) null
                    else {
                        val ms = parts[0].toLongOrNull()
                        val average = parts[1].toFloatOrNull()
                        val peak = parts[2].toFloatOrNull()
                        if (ms == null || average == null || peak == null ||
                            ms < 0 || !average.isFinite() || !peak.isFinite() ||
                            average !in -120f..0f || peak !in -120f..0f
                        ) null else AcousticPoint(ms, average, peak)
                    }
                }.toList()
            }
        } catch (_: Exception) { emptyList() }
    }

    /** Relative microphone level 0..1, NOT environmental dB SPL. */
    fun normalized(dbfs: Float): Float = ((dbfs.coerceIn(-80f, -15f) + 80f) / 65f).coerceIn(0f, 1f)
}

/** On each audio read: aggregate levels, emit a compact bucket every 5s. */
internal class AcousticAccumulator(private val intervalMs: Long = AcousticLog.INTERVAL_MS) {
    private var bucketStart = 0L
    private var total = 0.0
    private var count = 0
    private var peak = -120f

    fun accept(offsetMs: Long, dbfs: Float): AcousticPoint? {
        if (!dbfs.isFinite()) return null
        val bucket = (offsetMs.coerceAtLeast(0L) / intervalMs) * intervalMs
        if (count == 0) bucketStart = bucket
        if (bucket > bucketStart) {
            val result = emit()
            bucketStart = bucket
            total = dbfs.toDouble()
            count = 1
            peak = dbfs
            return result
        }
        total += dbfs.toDouble()
        count++
        peak = maxOf(peak, dbfs)
        return null
    }

    fun finish(): AcousticPoint? = if (count == 0) null else emit()

    private fun emit(): AcousticPoint {
        val point = AcousticPoint(bucketStart, (total / count).toFloat().coerceIn(-120f, 0f), peak.coerceIn(-120f, 0f))
        count = 0
        total = 0.0
        peak = -120f
        return point
    }
}
