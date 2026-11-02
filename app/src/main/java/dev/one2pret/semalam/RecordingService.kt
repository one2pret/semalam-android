package dev.one2pret.semalam

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/** State in the current process only; on a process kill SessionStore reconciles stale sessions. */
internal object RecorderStatus {
    @Volatile var isRecording = false
    @Volatile var lastError: String? = null
    @Volatile var phase: String = "idle"
    @Volatile var calibrationPercent: Int = 0
    @Volatile var inputDb: Double? = null
    @Volatile var detectedEvents: Int = 0
    @Volatile var savedClips: Int = 0
    @Volatile var scheduledStopAt: Long? = null
}

/** User-initiated foreground service, never started by a bedtime alarm. */
class RecordingService : Service() {
    companion object {
        const val ACTION_START = "dev.one2pret.semalam.START_RECORDING"
        const val ACTION_STOP = "dev.one2pret.semalam.STOP_RECORDING"
        private const val CHANNEL_ID = "semalam_recording"
        private const val NOTIFICATION_ID = 101
        private const val SAMPLE_RATE = 16_000
        private const val MAX_SESSION_MS = 12L * 60L * 60L * 1000L

        fun start(context: Context) {
            val intent = Intent(context, RecordingService::class.java).setAction(ACTION_START)
            RecorderStatus.phase = "starting"
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (error: Exception) {
                RecorderStatus.phase = "idle"
                throw error
            }
        }
        fun stop(context: Context) {
            context.startService(Intent(context, RecordingService::class.java).setAction(ACTION_STOP))
        }
    }

    private val running = AtomicBoolean(false)
    private var requestedStop = false
    private var worker: Thread? = null
    private var recorder: AudioRecord? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> requestStop()
            ACTION_START -> {
                if (running.get() || worker?.isAlive == true) return START_NOT_STICKY
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    RecorderStatus.lastError = "Izin mikrofon belum diberikan."
                    stopSelf()
                    return START_NOT_STICKY
                }
                try {
                    begin()
                } catch (error: Exception) {
                    RecorderStatus.lastError = "Gagal memulai: ${error.javaClass.simpleName}"
                    running.set(false)
                    RecorderStatus.isRecording = false
                    RecorderStatus.phase = "idle"
                    RecorderStatus.scheduledStopAt = null
                    releaseResources()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun begin() {
        createNotificationChannel()
        val notif = recordingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
        val minBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        check(minBytes > 0) { "Perangkat tidak menyediakan buffer audio." }
        val frameSamples = 1024
        val localRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBytes, frameSamples * 4)
        )
        if (localRecorder.state != AudioRecord.STATE_INITIALIZED) {
            localRecorder.release()
            error("Mikrofon tidak dapat diinisialisasi.")
        }
        recorder = localRecorder
        val sessionStore = SessionStore(this)
        val session = sessionStore.startSession()
        RecorderStatus.scheduledStopAt = AutoStopSettings.deadline(this, session.startedMs)
        requestedStop = false
        RecorderStatus.lastError = null
        RecorderStatus.calibrationPercent = 0
        RecorderStatus.inputDb = null
        RecorderStatus.detectedEvents = 0
        RecorderStatus.savedClips = 0
        RecorderStatus.phase = "calibrating"
        running.set(true)
        RecorderStatus.isRecording = true
        worker = Thread({ captureLoop(localRecorder, sessionStore, session.id) }, "SemalamAudioCapture")
            .also { it.start() }
    }

    private fun captureLoop(audio: AudioRecord, store: SessionStore, sessionId: String) {
        var status = "completed"
        var message = ""
        var clipLimitReached = false
        var lastHeartbeat = SystemClock.elapsedRealtime()
        val started = lastHeartbeat
        try {
            val power = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Semalam:AudioCapture")
                .apply { acquire(MAX_SESSION_MS + 60_000L) }
            audio.startRecording()
            val engine = SoundEventEngine(SAMPLE_RATE) { pcm, offsetMs, durationMs ->
                RecorderStatus.detectedEvents++
                if (!clipLimitReached) {
                    val saved = store.addClip(sessionId, pcm, offsetMs, durationMs)
                    if (saved) {
                        RecorderStatus.savedClips++
                    } else {
                        clipLimitReached = true
                        RecorderStatus.lastError = "Batas cuplikan atau penyimpanan hampir penuh."
                    }
                }
            }
            val frame = ShortArray(1024)
            var silentReads = 0
            while (running.get()) {
                val scheduled = RecorderStatus.scheduledStopAt
                if (scheduled != null && System.currentTimeMillis() >= scheduled) {
                    status = "scheduled_stop"
                    message = "Sesi berhenti otomatis sesuai jam yang dipilih."
                    break
                }
                if (SystemClock.elapsedRealtime() - started >= MAX_SESSION_MS) {
                    status = "auto_stopped"
                    message = "Sesi dihentikan otomatis setelah 12 jam."
                    break
                }
                val size = audio.read(frame, 0, frame.size, AudioRecord.READ_BLOCKING)
                when {
                    size > 0 -> {
                        val allZero = (0 until size).all { frame[it] == 0.toShort() }
                        silentReads = if (allZero) silentReads + 1 else 0
                        if (silentReads >= 6000) {
                            status = "interrupted"
                            message = "Mikrofon mengirim data nol terlalu lama; kemungkinan input dibisukan sistem."
                            RecorderStatus.lastError = message
                            break
                        }
                        engine.accept(frame, size)
                        RecorderStatus.calibrationPercent = engine.calibrationPercent
                        RecorderStatus.inputDb = engine.lastInputDb
                        if (engine.isCalibrated && RecorderStatus.phase != "stopping") {
                            RecorderStatus.phase = "recording"
                        }
                    }
                    !running.get() -> break
                    else -> {
                        status = "interrupted"
                        message = "Error membaca audio (kode $size)."
                        break
                    }
                }
                if (SystemClock.elapsedRealtime() - lastHeartbeat > 30_000L) {
                    store.heartbeat(sessionId)
                    lastHeartbeat = SystemClock.elapsedRealtime()
                }
            }
            engine.finish()
            if (!requestedStop && status == "completed") {
                status = "interrupted"
                message = "Sesi berhenti tanpa permintaan pengguna."
            }
            if (clipLimitReached && message.isEmpty()) {
                message = "Batas cuplikan/penyimpanan tercapai; sebagian kejadian tidak disimpan."
            }
        } catch (error: Exception) {
            status = "interrupted"
            message = "Perekaman terhenti: ${error.javaClass.simpleName}."
            RecorderStatus.lastError = message
        } finally {
            running.set(false)
            RecorderStatus.isRecording = false
            RecorderStatus.phase = "idle"
            RecorderStatus.scheduledStopAt = null
            try { store.finish(sessionId, status, message) } catch (_: Exception) { /* recovery on next launch */ }
            releaseResources()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun requestStop() {
        if (!running.get()) {
            stopSelf()
            return
        }
        requestedStop = true
        RecorderStatus.phase = "stopping"
        running.set(false)
        try { recorder?.stop() } catch (_: Exception) { }
    }

    private fun releaseResources() {
        try { recorder?.release() } catch (_: Exception) { }
        recorder = null
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) { }
        wakeLock = null
    }

    override fun onDestroy() {
        if (running.get()) {
            running.set(false) // System teardown is not a user-requested clean stop.
            try { recorder?.stop() } catch (_: Exception) { }
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Perekaman Semalam", NotificationManager.IMPORTANCE_LOW))
    }

    private fun recordingNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 100, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 101, Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(dev.one2pret.semalam.R.drawable.ic_semalam)
            .setContentTitle("Semalam sedang merekam")
            .setContentText("Kejadian suara disimpan lokal di HP.")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "Hentikan", stopIntent)
            .build()
    }
}
