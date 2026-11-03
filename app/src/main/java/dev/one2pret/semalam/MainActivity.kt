package dev.one2pret.semalam

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SemalamScreen() }
    }
}

private val navy = Color(0xFF11192B)
private val panel = Color(0xFF222F48)
private val accent = Color(0xFFA0B6FF)

@Composable
private fun SemalamScreen() {
    val context = LocalContext.current
    val store = remember { SessionStore(context) }
    var sessions by remember { mutableStateOf(store.list()) }
    var selectedId by remember { mutableStateOf(sessions.firstOrNull()?.id) }
    var recording by remember { mutableStateOf(RecorderStatus.isRecording) }
    var recordingPhase by remember { mutableStateOf(RecorderStatus.phase) }
    var calibrationPercent by remember { mutableStateOf(RecorderStatus.calibrationPercent) }
    var inputDb by remember { mutableStateOf(RecorderStatus.inputDb) }
    var detectedEvents by remember { mutableStateOf(RecorderStatus.detectedEvents) }
    var savedClips by remember { mutableStateOf(RecorderStatus.savedClips) }
    var storage by remember { mutableStateOf(store.storageSummary()) }
    var reminderEnabled by remember { mutableStateOf(BedtimeSchedule.isEnabled(context)) }
    var reminderHour by remember { mutableStateOf(BedtimeSchedule.hour(context)) }
    var reminderMinute by remember { mutableStateOf(BedtimeSchedule.minute(context)) }
    var autoStopEnabled by remember { mutableStateOf(AutoStopSettings.enabled(context)) }
    var autoStopHour by remember { mutableStateOf(AutoStopSettings.hour(context)) }
    var autoStopMinute by remember { mutableStateOf(AutoStopSettings.minute(context)) }
    var stopDeadline by remember { mutableStateOf(RecorderStatus.scheduledStopAt) }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showDeleteSession by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var player: MediaPlayer? by remember { mutableStateOf(null) }
    var playingClip by remember { mutableStateOf<String?>(null) }
    var playbackSession by remember { mutableStateOf<String?>(null) }
    var playbackQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var queueIndex by remember { mutableStateOf(-1) }
    var isPaused by remember { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableStateOf(0) }
    var playbackDurationMs by remember { mutableStateOf(0) }
    var playbackSerial by remember { mutableStateOf(0) }
    var clipAmplitude by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var reviewedAmplitudeSession by remember { mutableStateOf<String?>(null) }
    var selectedTimeBin by remember { mutableStateOf<Int?>(null) }
    var selectedIncidentStart by remember { mutableStateOf<Long?>(null) }
    var incidentSort by remember { mutableStateOf("Menonjol") }
    var showAllIncidents by remember { mutableStateOf(false) }

    fun refresh() {
        store.markStaleSessions()
        sessions = store.list()
        val active = sessions.firstOrNull { it.status == "recording" }
        if (RecorderStatus.isRecording && active != null) selectedId = active.id
        if (selectedId == null || sessions.none { it.id == selectedId }) {
            selectedId = sessions.firstOrNull()?.id
        }
        recording = RecorderStatus.isRecording
        recordingPhase = RecorderStatus.phase
        calibrationPercent = RecorderStatus.calibrationPercent
        inputDb = RecorderStatus.inputDb
        detectedEvents = RecorderStatus.detectedEvents
        savedClips = RecorderStatus.savedClips
        stopDeadline = RecorderStatus.scheduledStopAt
        storage = store.storageSummary()
        if (recordingPhase != "starting" && recordingPhase != "stopping" &&
            statusText in listOf("Memulai perekaman…", "Menghentikan dan menyimpan sesi…")
        ) statusText = ""
    }

    fun clearPlayer() {
        playbackSerial++
        val previous = player
        player = null
        playingClip = null
        isPaused = false
        playbackPositionMs = 0
        playbackDurationMs = 0
        try { previous?.setOnCompletionListener(null); previous?.stop(); previous?.release() } catch (_: Exception) {
            try { previous?.release() } catch (_: Exception) { }
        }
    }

    fun stopPlayback() {
        clearPlayer()
        playbackSession = null
        playbackQueue = emptyList()
        queueIndex = -1
    }

    fun playQueueAt(sessionId: String, clipIds: List<String>, index: Int) {
        if (index !in clipIds.indices) { stopPlayback(); return }
        clearPlayer()
        val clipId = clipIds[index]
        val clip = store.get(sessionId)?.clips?.firstOrNull { it.id == clipId }
        if (clip == null) {
            statusText = "Cuplikan sudah tidak tersedia. Daftar putar dihentikan."
            stopPlayback()
            return
        }
        val serial = playbackSerial
        try {
            val file = store.clipFile(sessionId, clip)
            val next = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDataSource(file.absolutePath)
                prepare()
            }
            playbackSession = sessionId
            playbackQueue = clipIds
            queueIndex = index
            playbackDurationMs = next.duration
            playbackPositionMs = 0
            playingClip = clipId
            player = next
            next.setOnCompletionListener {
                if (serial == playbackSerial && playbackSession == sessionId) {
                    if (index + 1 < clipIds.size) playQueueAt(sessionId, clipIds, index + 1)
                    else stopPlayback()
                }
            }
            next.start()
            statusText = ""
        } catch (_: Exception) {
            stopPlayback()
            statusText = "Cuplikan tidak bisa diputar. Periksa file audio."
        }
    }

    fun togglePause() {
        val active = player ?: return
        try {
            if (active.isPlaying) {
                active.pause()
                isPaused = true
            } else {
                active.start()
                isPaused = false
            }
        } catch (_: Exception) { stopPlayback() }
    }

    fun startRecording() {
        try {
            RecordingService.start(context)
            statusText = "Memulai perekaman…"
        } catch (e: Exception) {
            statusText = "Tidak dapat memulai: ${e.javaClass.simpleName}"
        }
    }

    val audioPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) startRecording() else statusText = "Semalam membutuhkan izin mikrofon untuk merekam."
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) statusText = "Pengingat sudah diatur, tetapi notifikasi belum diizinkan."
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(1_500L)
        }
    }
    LaunchedEffect(playingClip, isPaused) {
        while (playingClip != null && !isPaused) {
            try { playbackPositionMs = player?.currentPosition ?: 0 } catch (_: Exception) { }
            delay(120L)
        }
    }
    // Amplitude summary is a bounded WAV sample, computed off the UI thread.
    // Recomputed when changing session or when a clip is added/deleted.
    LaunchedEffect(selectedId, sessions.firstOrNull { it.id == selectedId }?.clips?.map { it.id }) {
        val source = sessions.firstOrNull { it.id == selectedId }
        reviewedAmplitudeSession = null
        clipAmplitude = emptyMap()
        if (source != null && source.status != "recording") {
            val peaks = withContext(Dispatchers.IO) {
                source.clips.associate { clip ->
                    val sampled = WavWaveform.readPeaks(store.clipFile(source.id, clip), bars = 24)
                    clip.id to (sampled.maxOrNull() ?: 0f)
                }
            }
            clipAmplitude = peaks
            reviewedAmplitudeSession = source.id
        }
    }
    DisposableEffect(Unit) {
        onDispose { try { player?.release() } catch (_: Exception) { } }
    }

    val current = sessions.firstOrNull { it.id == selectedId }
    val dashboard = current?.let { SoundDashboard.summarize(it.clips.map { clip -> clip.label }) }
    val sessionPeaks = if (current?.id == reviewedAmplitudeSession) clipAmplitude else emptyMap()
    val incidents = remember(current?.clips, sessionPeaks) {
        NightReview.group(current?.clips ?: emptyList(), sessionPeaks)
    }
    val sessionDuration = current?.let { (it.finishedMs ?: System.currentTimeMillis()) - it.startedMs } ?: 0L
    val timeBins = remember(current?.clips, sessionPeaks, sessionDuration / NightReview.BIN_MS) {
        NightReview.bins(current?.clips ?: emptyList(), sessionPeaks, sessionDuration)
    }
    val isCurrentRecording = recordingPhase in setOf("starting", "calibrating", "recording", "stopping") ||
        (recording && sessions.any { it.status == "recording" })
    val colors = darkColorScheme(
        primary = accent, onPrimary = navy, background = navy,
        surface = navy, surfaceContainer = panel,
        onSurface = Color(0xFFF3F6FF), onBackground = Color(0xFFF3F6FF)
    )

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = navy) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("☾  Semalam", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Kenali kejadian suara saat tidur, tanpa mengunggah rekaman ke internet.", color = Color(0xFFBFCBE3))
                Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(when (recordingPhase) {
                            "starting" -> "● Memulai perekaman"
                            "calibrating" -> "● Kalibrasi suara kamar"
                            "recording" -> "● Sedang merekam"
                            "stopping" -> "● Menghentikan perekaman"
                            else -> "Siap untuk malam ini"
                        }, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (isCurrentRecording) "Mikrofon aktif. Layar boleh dikunci. Hentikan ketika bangun."
                            else "Mulai sesi saat siap tidur. Selama 12 detik pertama, aplikasi mengukur kebisingan kamar.",
                            color = Color(0xFFD2DCED)
                        )
                        if (recordingPhase == "calibrating") {
                            Text("Kalibrasi: ${calibrationPercent}% — usahakan kamar tenang selama 12 detik.")
                        }
                        if (recordingPhase == "recording") {
                            Text("Level mikrofon: ${inputDb?.toInt() ?: "—"} dBFS (diagnostik)")
                            Text("Kejadian: " + detectedEvents + " · Cuplikan disimpan: " + savedClips)
                        }
                        if (isCurrentRecording && stopDeadline != null) {
                            Text("Berhenti otomatis sekitar " + formatClock(stopDeadline!!) + " jika sesi tetap aktif.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCBE3))
                        }
                        if (recordingPhase == "stopping") {
                            Text("Menyimpan hasil sesi…")
                        } else if (isCurrentRecording) {
                            Button(onClick = {
                                RecordingService.stop(context)
                                statusText = "Menghentikan dan menyimpan sesi…"
                            }, modifier = Modifier.fillMaxWidth()) { Text("■  Hentikan Perekaman") }
                        } else {
                            Button(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    startRecording()
                                } else {
                                    val permissions = if (Build.VERSION.SDK_INT >= 33) {
                                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                                    } else arrayOf(Manifest.permission.RECORD_AUDIO)
                                    audioPermissions.launch(permissions)
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("◉  Mulai Tidur / Rekam") }
                        }
                        Text("Hanya cuplikan kejadian suara yang disimpan; bukan seluruh rekaman semalaman.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBC8E0))
                    }
                }
                if (statusText.isNotEmpty() || RecorderStatus.lastError != null) {
                    Text(RecorderStatus.lastError ?: statusText, color = Color(0xFFFFD8A0))
                }
                if (current != null && dashboard != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("Dashboard kejadian malam", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text(formatDate(current.startedMs) + " · " + dashboard.total + " cuplikan · " +
                                sessionStatusLabel(current.status), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(dashboard.snoring.toString() + " · Mirip dengkuran")
                                    Text(dashboard.sleepTalking.toString() + " · Mirip mengigau/bicara")
                                    Text(dashboard.bodySound.toString() + " · Batuk/suara tubuh")
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(dashboard.environment.toString() + " · Suara lingkungan")
                                    Text(dashboard.unreviewed.toString() + " · Belum ditinjau")
                                    Text(dashboard.other.toString() + " · Lainnya")
                                }
                            }
                            Text("Angka berasal dari LABEL yang Anda pilih sesudah mendengarkan, bukan klasifikasi otomatis atau diagnosis gangguan tidur.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCBE3))
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pengingat tidur", fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Aktifkan pengingat harian", modifier = Modifier.weight(1f))
                            Switch(checked = reminderEnabled, onCheckedChange = { enabled ->
                                reminderEnabled = enabled
                                BedtimeSchedule.update(context, enabled, reminderHour, reminderMinute)
                                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            })
                        }
                        OutlinedButton(onClick = {
                            TimePickerDialog(context, { _, hour, minute ->
                                reminderHour = hour
                                reminderMinute = minute
                                BedtimeSchedule.update(context, reminderEnabled, hour, minute)
                            }, reminderHour, reminderMinute, true).show()
                        }) { Text("Jam pengingat: %02d:%02d".format(reminderHour, reminderMinute)) }
                        Text("Pengingat tidak menyalakan mikrofon otomatis dan dapat terlambat akibat hemat baterai.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBC8E0))
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Berhenti otomatis", fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Aktifkan jam berhenti", modifier = Modifier.weight(1f))
                            Switch(checked = autoStopEnabled, enabled = !isCurrentRecording, onCheckedChange = { enabled ->
                                autoStopEnabled = enabled
                                AutoStopSettings.save(context, enabled, autoStopHour, autoStopMinute)
                            })
                        }
                        OutlinedButton(enabled = !isCurrentRecording, onClick = {
                            TimePickerDialog(context, { _, hour, minute ->
                                autoStopHour = hour
                                autoStopMinute = minute
                                AutoStopSettings.save(context, autoStopEnabled, hour, minute)
                            }, autoStopHour, autoStopMinute, true).show()
                        }) { Text("Jam berhenti: %02d:%02d".format(autoStopHour, autoStopMinute)) }
                        Text("Berlaku untuk sesi berikutnya setelah Anda tekan Mulai. File tetap tersimpan dan mikrofon tidak dinyalakan otomatis.",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCBE3))
                    }
                }
                HorizontalDivider(color = Color(0xFF40506D))
                Text("Riwayat perekaman", style = MaterialTheme.typography.titleLarge)
                Text("Penyimpanan lokal: ${formatStorageSize(storage.bytes)} · ${storage.sessions} sesi · ${storage.clips} cuplikan",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCBE3))
                if (sessions.isEmpty()) {
                    Text("Belum ada sesi. Mulai rekam, tunggu kalibrasi 12 detik, kemudian buat suara beberapa detik untuk uji.", color = Color(0xFFBFCBE3))
                }
                sessions.forEach { session ->
                    val selected = session.id == selectedId
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF344360) else panel),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            stopPlayback()
                            selectedId = session.id
                            selectedFilter = "Semua"
                            selectedTimeBin = null
                            selectedIncidentStart = null
                            showAllIncidents = false
                        }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(formatDate(session.startedMs), fontWeight = FontWeight.SemiBold)
                                Text("${formatClock(session.startedMs)} · ${session.clips.size} cuplikan · ${sessionStatusLabel(session.status)}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (selected) Text("✓", color = accent)
                        }
                    }
                }
                if (current != null) {
                    HorizontalDivider(color = Color(0xFF40506D))
                    Text("Detail sesi", style = MaterialTheme.typography.titleLarge)
                    Text("Mulai: ${formatClock(current.startedMs)} · Durasi sesi: ${formatDuration((current.finishedMs ?: System.currentTimeMillis()) - current.startedMs)}")
                    Text("Status: ${sessionStatusLabel(current.status)}", color = Color(0xFFBBC8E0))
                    if (current.message.isNotEmpty()) Text("Catatan: ${current.message}", color = Color(0xFFFFD8A0))
                    if (current.clips.isEmpty()) Text("Belum ada suara yang melewati ambang deteksi. Hasil kosong bukan berarti tidur tanpa gangguan.")
                    if (current.clips.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Peta aktivitas malam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Ketuk batang untuk memilih rentang 15 menit. Grafik hanya berasal dari cuplikan yang tersimpan; area kosong bukan bukti tidak ada suara.",
                                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBC8E0))
                                NightActivityTimeline(
                                    bins = timeBins,
                                    selectedIndex = selectedTimeBin,
                                    onSelect = { index ->
                                        stopPlayback()
                                        selectedIncidentStart = null
                                        selectedTimeBin = if (selectedTimeBin == index) null else index
                                        showAllIncidents = false
                                    }
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(formatClock(current.startedMs), style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.weight(1f))
                                    Text(formatClock(current.finishedMs ?: System.currentTimeMillis()),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                val selectedBinData = selectedTimeBin?.let { timeBins.getOrNull(it) }
                                if (selectedBinData != null) {
                                    Text("Rentang " + formatClock(current.startedMs + selectedBinData.startOffsetMs) +
                                        " · " + selectedBinData.eventCount + " cuplikan")
                                    OutlinedButton(onClick = {
                                        selectedTimeBin = null
                                        selectedIncidentStart = null
                                    }) { Text("Tampilkan semua waktu") }
                                }
                            }
                        }
                        val binData = selectedTimeBin?.let { timeBins.getOrNull(it) }
                        val timeRestricted = if (binData == null) incidents else incidents.filter {
                            it.lastOffsetMs > binData.startOffsetMs && it.firstOffsetMs < binData.endOffsetMs
                        }
                        val orderedIncidents = when (incidentSort) {
                            "Waktu" -> timeRestricted.sortedBy { it.firstOffsetMs }
                            "Terbaru" -> timeRestricted.sortedByDescending { it.firstOffsetMs }
                            "Terpanjang" -> timeRestricted.sortedByDescending { it.lastOffsetMs - it.firstOffsetMs }
                            else -> timeRestricted.sortedWith(compareByDescending<NightIncident> { it.prominentScore }
                                .thenByDescending { it.maxSampledAmplitude })
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("Kelompok kejadian · " + orderedIncidents.size,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Cuplikan dalam jeda maksimal 30 detik dikelompokkan secara virtual; file asli tidak digabung atau dihapus.",
                                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBC8E0))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Urutkan: ", style = MaterialTheme.typography.bodySmall)
                                    androidx.compose.foundation.layout.Box {
                                        var sortOpen by remember { mutableStateOf(false) }
                                        OutlinedButton(onClick = { sortOpen = true }) { Text(incidentSort) }
                                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                                            listOf("Menonjol", "Waktu", "Terbaru", "Terpanjang").forEach { choice ->
                                                DropdownMenuItem(text = { Text(choice) }, onClick = {
                                                    incidentSort = choice
                                                    sortOpen = false
                                                })
                                            }
                                        }
                                    }
                                }
                                if (reviewedAmplitudeSession != current.id) {
                                    Text("Menganalisis puncak sampel cuplikan…",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                orderedIncidents.take(if (showAllIncidents) orderedIncidents.size else 10).forEach { incident ->
                                    val selected = selectedIncidentStart == incident.firstOffsetMs
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF344360) else navy),
                                        onClick = {
                                            stopPlayback()
                                            selectedIncidentStart = if (selected) null else incident.firstOffsetMs
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(formatClock(current.startedMs + incident.firstOffsetMs) +
                                                " – " + formatClock(current.startedMs + incident.lastOffsetMs) +
                                                " · " + incident.clipIds.size + " cuplikan",
                                                fontWeight = FontWeight.SemiBold)
                                            Text("Indeks aktivitas: " + incident.prominentScore +
                                                "/100 · Puncak sampel: " + (incident.maxSampledAmplitude * 100).toInt() +
                                                "% · Ditandai: " + incident.labeledClips,
                                                style = MaterialTheme.typography.bodySmall)
                                            if (selected) {
                                                Text("Dipilih — cuplikan ditampilkan di bawah",
                                                    color = accent, style = MaterialTheme.typography.bodySmall)
                                            }
                                            OutlinedButton(onClick = {
                                                selectedIncidentStart = incident.firstOffsetMs
                                                playQueueAt(current.id, incident.clipIds, 0)
                                            }) { Text("▶ Putar kelompok") }
                                        }
                                    }
                                }
                                if (orderedIncidents.size > 10) {
                                    OutlinedButton(onClick = { showAllIncidents = !showAllIncidents }) {
                                        Text(if (showAllIncidents) "Ringkas daftar" else "Tampilkan semua " + orderedIncidents.size + " kelompok")
                                    }
                                }
                                Text("Indeks aktivitas adalah prioritas mendengar berdasarkan amplitudo sampel, durasi dan jumlah cuplikan, BUKAN tingkat gangguan tidur atau diagnosis.",
                                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBC8E0))
                            }
                        }
                    }
                    if (current.clips.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Filter: ", style = MaterialTheme.typography.bodySmall)
                            androidx.compose.foundation.layout.Box {
                                var showFilter by remember { mutableStateOf(false) }
                                OutlinedButton(onClick = { showFilter = true }) { Text(selectedFilter) }
                                DropdownMenu(expanded = showFilter, onDismissRequest = { showFilter = false }) {
                                    listOf("Semua", "Dengkuran", "Berbicara", "Lingkungan", "Suara tubuh", "Belum ditinjau").forEach { choice ->
                                        DropdownMenuItem(text = { Text(choice) }, onClick = {
                                            stopPlayback()
                                            selectedFilter = choice
                                            showFilter = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                    val filteredClips = current.clips.filter { SoundDashboard.matchesFilter(it.label, selectedFilter) }
                    val activeIncident = incidents.firstOrNull { it.firstOffsetMs == selectedIncidentStart }
                    val reviewClips = if (activeIncident == null) emptyList() else filteredClips.filter {
                        it.id in activeIncident.clipIds
                    }
                    if (selectedIncidentStart == null) {
                        Text("Pilih kelompok kejadian di atas untuk melihat dan mendengarkan cuplikannya.",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCBE3))
                    } else if (reviewClips.isEmpty()) {
                        Text("Tidak ada cuplikan dalam kelompok ini dengan filter sekarang.")
                    }
                    if (reviewClips.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("Pemutar kelompok", fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = {
                                        if (playbackSession == current.id && playingClip != null && playbackQueue.size == reviewClips.size &&
                                            playbackQueue == reviewClips.map { it.id }) togglePause()
                                        else playQueueAt(current.id, reviewClips.map { it.id }, 0)
                                    }) { Text(if (playbackSession == current.id && playingClip != null && playbackQueue.size == reviewClips.size && playbackQueue == reviewClips.map { it.id } && !isPaused) "⏸ Jeda" else "▶ Play All") }
                                    OutlinedButton(onClick = { stopPlayback() }, enabled = playingClip != null) { Text("■ Stop") }
                                }
                                if (playbackSession == current.id && playingClip != null) {
                                    val indexText = (queueIndex + 1).coerceAtLeast(1).toString() + "/" + playbackQueue.size
                                    Text("Cuplikan " + indexText + " · " + formatPlaybackTime(playbackPositionMs) +
                                        " / " + formatPlaybackTime(playbackDurationMs), style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        OutlinedButton(onClick = {
                                            if (queueIndex > 0) playQueueAt(current.id, playbackQueue, queueIndex - 1)
                                        }, enabled = queueIndex > 0) { Text("◀ Sebelumnya") }
                                        OutlinedButton(onClick = {
                                            if (queueIndex + 1 < playbackQueue.size) playQueueAt(current.id, playbackQueue, queueIndex + 1)
                                        }, enabled = queueIndex + 1 < playbackQueue.size) { Text("Berikutnya ▶") }
                                    }
                                }
                                Text("Play All memutar cuplikan dalam kelompok dan filter " + selectedFilter + "; lanjut otomatis.",
                                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCBE3))
                            }
                        }
                    }
                    reviewClips.forEach { clip ->
                        ClipCard(
                            session = current, clip = clip, isPlaying = playingClip == clip.id,
                            isPaused = isPaused, progressMs = playbackPositionMs, durationMs = playbackDurationMs,
                            audioFile = store.clipFile(current.id, clip),
                            editable = current.status != "recording",
                            onPlay = {
                                if (playbackSession == current.id && playingClip == clip.id) togglePause()
                                else playQueueAt(current.id, listOf(clip.id), 0)
                            },
                            onLabel = { label -> store.updateLabel(current.id, clip.id, label); refresh() },
                            onDelete = {
                                stopPlayback()
                                statusText = if (store.removeClip(current.id, clip.id)) "Cuplikan dan file audio telah dihapus."
                                else "Penghapusan gagal. File mungkin masih ada; coba kembali setelah sesi berhenti."
                                refresh()
                            },
                        )
                    }
                    if (current.status != "recording") {
                        OutlinedButton(onClick = { showDeleteSession = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Hapus sesi dan seluruh cuplikan")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Prototype riset pribadi, bukan alat diagnosis atau pengukur durasi tidur. Jika ada orang lain di kamar, minta persetujuannya sebelum merekam.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCBE3), textAlign = TextAlign.Center
                )
            }
        }
        if (showDeleteSession && current != null) {
            AlertDialog(
                onDismissRequest = { showDeleteSession = false },
                title = { Text("Hapus rekaman?") },
                text = { Text("Seluruh cuplikan audio sesi ini akan dihapus dari perangkat. Tindakan tidak bisa dibatalkan.") },
                confirmButton = { TextButton(onClick = {
                    stopPlayback()
                    statusText = if (store.removeSession(current.id)) "Sesi dan semua file audio dihapus."
                    else "Penghapusan sesi gagal; periksa penyimpanan perangkat."
                    showDeleteSession = false
                    refresh()
                }) { Text("Hapus permanen") } },
                dismissButton = { TextButton(onClick = { showDeleteSession = false }) { Text("Batal") } },
            )
        }
    }
}

@Composable
private fun ClipCard(
    session: SleepSession,
    clip: SoundClip,
    isPlaying: Boolean,
    isPaused: Boolean,
    progressMs: Int,
    durationMs: Int,
    audioFile: File,
    editable: Boolean,
    onPlay: () -> Unit,
    onLabel: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showLabels by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${formatClock(session.startedMs + clip.offsetMs)} · ${clip.durationMs / 1000}s · ${clip.label}")
            if (isPlaying) {
                ClipWaveform(audioFile, if (durationMs > 0) progressMs.toFloat() / durationMs else 0f)
                Text(formatPlaybackTime(progressMs) + " / " + formatPlaybackTime(durationMs),
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCBE3))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPlay) { Text(if (isPlaying && !isPaused) "⏸ Jeda" else if (isPlaying) "▶ Lanjut" else "▶ Putar") }
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { showLabels = true }, enabled = editable) { Text("Label") }
                    DropdownMenu(expanded = showLabels, onDismissRequest = { showLabels = false }) {
                        listOf("Mirip dengkuran", "Mirip berbicara", "Suara sekitar", "Suara keras dari luar", "Batuk / suara tubuh", "Lainnya", "Tidak yakin").forEach { label ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { showLabels = false; onLabel(label) })
                        }
                    }
                }
                TextButton(onClick = { confirmDelete = true }, enabled = editable) { Text("Hapus") }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Hapus cuplikan?") },
        text = { Text("File suara ini akan dihapus permanen.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } },
    )
}

private fun sessionStatusLabel(status: String): String = when (status) {
    "recording" -> "Sedang merekam"
    "completed" -> "Selesai"
    "interrupted" -> "Terputus"
    "auto_stopped" -> "Berhenti otomatis"
    "scheduled_stop" -> "Berhenti sesuai jadwal"
    else -> status
}

private fun formatStorageSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes.toDouble() / (1024L * 1024L))
    bytes >= 1024L -> "%.0f KB".format(bytes.toDouble() / 1024L)
    else -> bytes.toString() + " B"
}

private fun formatPlaybackTime(valueMs: Int): String {
    val seconds = valueMs.coerceAtLeast(0) / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun ClipWaveform(file: File, progress: Float) {
    var amplitudes by remember(file.absolutePath, file.length()) { mutableStateOf<List<Float>>(emptyList()) }
    LaunchedEffect(file.absolutePath, file.length()) {
        amplitudes = withContext(Dispatchers.IO) { WavWaveform.readPeaks(file) }
    }
    if (amplitudes.isEmpty()) {
        Text("Memuat gelombang suara…", style = MaterialTheme.typography.bodySmall)
        return
    }
    val fraction = progress.coerceIn(0f, 1f)
    Canvas(modifier = Modifier.fillMaxWidth().height(52.dp)) {
        val count = amplitudes.size
        val pitch = size.width / count
        amplitudes.forEachIndexed { index, amplitude ->
            val x = (index + 0.5f) * pitch
            val halfHeight = (amplitude.coerceIn(0f, 1f) * size.height * 0.46f).coerceAtLeast(2.dp.toPx())
            drawLine(
                color = if (index.toFloat() / count <= fraction) accent else Color(0xFF60718C),
                start = Offset(x, size.height / 2f - halfHeight),
                end = Offset(x, size.height / 2f + halfHeight),
                strokeWidth = (pitch * 0.60f).coerceAtLeast(1.dp.toPx()),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun NightActivityTimeline(
    bins: List<NightHour>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit
) {
    if (bins.isEmpty()) return
    val maxEvents = (bins.maxOfOrNull { it.eventCount } ?: 1).coerceAtLeast(1)
    Canvas(modifier = Modifier.fillMaxWidth().height(88.dp).pointerInput(bins.size) {
        detectTapGestures { point ->
            if (size.width > 0 && bins.isNotEmpty()) {
                val index = ((point.x / size.width) * bins.size).toInt().coerceIn(0, bins.lastIndex)
                onSelect(index)
            }
        }
    }) {
        val widthPerBin = size.width / bins.size
        val baseline = size.height * .91f
        bins.forEachIndexed { index, bin ->
            val rate = (bin.eventCount.toFloat() / maxEvents).coerceIn(0f, 1f)
            val barHeight = if (bin.eventCount == 0) 2.dp.toPx()
                else (12.dp.toPx() + rate * size.height * .67f).coerceAtMost(size.height * .83f)
            val x = index * widthPerBin
            drawRect(
                color = if (selectedIndex == index) Color(0xFFFFC779)
                    else if (bin.peak >= .6f) accent else Color(0xFF788FBF),
                topLeft = Offset(x + widthPerBin * .12f, baseline - barHeight),
                size = Size(widthPerBin * .76f, barHeight)
            )
        }
        drawLine(Color(0xFF506384), Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())
    }
}
