package dev.one2pret.semalam

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal data class SoundClip(
    val id: String,
    val fileName: String,
    val offsetMs: Long,
    val durationMs: Long,
    val label: String = "Belum dilabeli",
)

internal data class SleepSession(
    val id: String,
    val startedMs: Long,
    val finishedMs: Long?,
    val lastHeartbeatMs: Long,
    val status: String,
    val message: String,
    val clips: List<SoundClip>,
)

internal data class StorageSummary(val bytes: Long, val sessions: Int, val clips: Int)

internal class SessionStore(context: Context) {
    private val base = File(context.filesDir, "sleep_sessions").apply { mkdirs() }

    @Synchronized
    fun startSession(): SleepSession {
        val now = System.currentTimeMillis()
        val session = SleepSession(UUID.randomUUID().toString(), now, null, now, "recording", "", emptyList())
        sessionDir(session.id).mkdirs()
        write(session)
        return session
    }

    @Synchronized
    fun get(id: String): SleepSession? = read(File(sessionDir(id), "session.json"))

    @Synchronized
    fun list(): List<SleepSession> = base.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { read(File(it, "session.json")) }
        ?.sortedByDescending { it.startedMs } ?: emptyList()

    @Synchronized
    fun heartbeat(id: String) {
        val old = get(id) ?: return
        if (old.status == "recording") write(old.copy(lastHeartbeatMs = System.currentTimeMillis()))
    }

    @Synchronized
    fun finish(id: String, status: String, message: String = "") {
        val old = get(id) ?: return
        write(old.copy(finishedMs = System.currentTimeMillis(), lastHeartbeatMs = System.currentTimeMillis(), status = status, message = message))
    }

    @Synchronized
    fun markStaleSessions() {
        val now = System.currentTimeMillis()
        if (RecorderStatus.isRecording) return
        list().filter { it.status == "recording" && now - it.lastHeartbeatMs > 120_000L }
            .forEach { old -> write(old.copy(status = "interrupted", finishedMs = old.lastHeartbeatMs, message = "Sesi terputus; aplikasi atau sistem menghentikan perekaman.")) }
    }

    @Synchronized
    fun addClip(id: String, samples: ShortArray, offsetMs: Long, durationMs: Long): Boolean {
        val old = get(id) ?: return false
        if (old.clips.size >= 240) return false // Safety cap for POC storage.
        val fileName = "clip_${UUID.randomUUID()}.wav"
        val destination = File(sessionDir(id), fileName)
        if (usableSpaceBytes() < 100L * 1024L * 1024L) return false
        WavFiles.write(destination, samples)
        write(old.copy(clips = old.clips + SoundClip(UUID.randomUUID().toString(), fileName, offsetMs, durationMs)))
        return true
    }

    @Synchronized
    fun updateLabel(sessionId: String, clipId: String, label: String) {
        val safeLabels = setOf("Belum dilabeli", "Mirip dengkuran", "Mirip berbicara", "Suara sekitar", "Lainnya", "Tidak yakin")
        if (label !in safeLabels) return
        val old = get(sessionId) ?: return
        if (old.status == "recording") return
        write(old.copy(clips = old.clips.map { if (it.id == clipId) it.copy(label = label) else it }))
    }

    @Synchronized
    fun removeClip(sessionId: String, clipId: String): Boolean {
        val old = get(sessionId) ?: return false
        // No edits to an active session: the recorder and UI own different store instances.
        if (old.status == "recording" || RecorderStatus.isRecording && old.status == "recording") return false
        val clip = old.clips.firstOrNull { it.id == clipId } ?: return false
        val audioFile = File(sessionDir(sessionId), clip.fileName)
        if (!audioFile.exists() || !audioFile.delete()) return false
        return try {
            write(old.copy(clips = old.clips.filterNot { it.id == clipId }))
            true
        } catch (_: Exception) {
            false // UI reports failure rather than falsely claiming the file was deleted.
        }
    }

    @Synchronized
    fun removeSession(id: String): Boolean {
        val old = get(id) ?: return false
        if (old.status == "recording") return false
        val folder = sessionDir(id)
        return folder.deleteRecursively() && !folder.exists()
    }

    fun storageSummary(): StorageSummary {
        val sessions = list()
        // Include metadata and any residual WAV/temp files when reporting used storage.
        val bytes = base.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return StorageSummary(bytes, sessions.size, sessions.sumOf { it.clips.size })
    }

    fun clipFile(sessionId: String, clip: SoundClip): File = File(sessionDir(sessionId), clip.fileName)

    private fun usableSpaceBytes(): Long = base.usableSpace
    private fun sessionDir(id: String) = File(base, id)

    private fun read(file: File): SleepSession? = try {
        if (!file.isFile) null else {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        val clipsArray = json.optJSONArray("clips") ?: JSONArray()
        val clips = (0 until clipsArray.length()).map { i ->
            val j = clipsArray.getJSONObject(i)
            SoundClip(j.getString("id"), j.getString("fileName"), j.getLong("offsetMs"), j.getLong("durationMs"), j.optString("label", "Belum dilabeli"))
        }
        SleepSession(
            json.getString("id"), json.getLong("startedMs"),
            if (json.isNull("finishedMs")) null else json.getLong("finishedMs"),
            json.getLong("lastHeartbeatMs"), json.getString("status"), json.optString("message", ""), clips,
        )
        }
    } catch (_: Exception) { null }

    private fun write(s: SleepSession) {
        val json = JSONObject().apply {
            put("id", s.id)
            put("startedMs", s.startedMs)
            put("finishedMs", s.finishedMs ?: JSONObject.NULL)
            put("lastHeartbeatMs", s.lastHeartbeatMs)
            put("status", s.status)
            put("message", s.message)
            put("clips", JSONArray().also { array ->
                s.clips.forEach { clip ->
                    array.put(JSONObject().apply {
                        put("id", clip.id)
                        put("fileName", clip.fileName)
                        put("offsetMs", clip.offsetMs)
                        put("durationMs", clip.durationMs)
                        put("label", clip.label)
                    })
                }
            })
        }
        val folder = sessionDir(s.id).apply { mkdirs() }
        val temp = File(folder, "session.json.tmp")
        FileOutputStream(temp).use { out ->
            out.write(json.toString().toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        val actual = File(folder, "session.json")
        if (!temp.renameTo(actual)) {
            // Fallback for unusual filesystem rename behavior.
            temp.copyTo(actual, overwrite = true)
            temp.delete()
        }
    }
}

internal object WavFiles {
    private const val SAMPLE_RATE = 16_000
    fun write(destination: File, samples: ShortArray) {
        val temp = File(destination.parentFile, destination.name + ".tmp")
        FileOutputStream(temp).use { fileOut ->
            val out = BufferedOutputStream(fileOut)
            val payloadBytes = samples.size * 2
            fun writeLE(value: Int, bytes: Int) {
                repeat(bytes) { index -> out.write((value ushr (index * 8)) and 0xff) }
            }
            out.write("RIFF".toByteArray(Charsets.US_ASCII))
            writeLE(payloadBytes + 36, 4)
            out.write("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            writeLE(16, 4)
            writeLE(1, 2) // PCM
            writeLE(1, 2) // Mono
            writeLE(SAMPLE_RATE, 4)
            writeLE(SAMPLE_RATE * 2, 4)
            writeLE(2, 2)
            writeLE(16, 2)
            out.write("data".toByteArray(Charsets.US_ASCII))
            writeLE(payloadBytes, 4)
            samples.forEach { sample -> writeLE(sample.toInt() and 0xffff, 2) }
            out.flush()
            fileOut.fd.sync()
        }
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
    }
}

internal fun formatClock(millis: Long): String = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(millis))
internal fun formatDate(millis: Long): String = SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID")).format(Date(millis))
internal fun formatDuration(millis: Long): String = "${millis / 3_600_000L}j ${(millis / 60_000L) % 60}m"
