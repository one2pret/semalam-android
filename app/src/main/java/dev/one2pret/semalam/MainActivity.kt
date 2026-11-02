package dev.one2pret.semalam

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

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
    var reminderEnabled by remember { mutableStateOf(BedtimeSchedule.isEnabled(context)) }
    var reminderHour by remember { mutableStateOf(BedtimeSchedule.hour(context)) }
    var reminderMinute by remember { mutableStateOf(BedtimeSchedule.minute(context)) }
    var showDeleteSession by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var player: MediaPlayer? by remember { mutableStateOf(null) }
    var playingClip by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        store.markStaleSessions()
        sessions = store.list()
        val active = sessions.firstOrNull { it.status == "recording" }
        if (RecorderStatus.isRecording && active != null) selectedId = active.id
        if (selectedId == null || sessions.none { it.id == selectedId }) {
            selectedId = sessions.firstOrNull()?.id
        }
        recording = RecorderStatus.isRecording
    }

    fun stopPlayback() {
        try { player?.stop(); player?.release() } catch (_: Exception) { }
        player = null
        playingClip = null
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
    DisposableEffect(Unit) {
        onDispose { try { player?.release() } catch (_: Exception) { } }
    }

    val current = sessions.firstOrNull { it.id == selectedId }
    val isCurrentRecording = recording && sessions.any { it.status == "recording" }
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
                        Text(if (isCurrentRecording) "● Sedang merekam" else "Siap untuk malam ini", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (isCurrentRecording) "Mikrofon aktif. Layar boleh dikunci. Hentikan ketika bangun."
                            else "Mulai sesi saat siap tidur. Selama 12 detik pertama, aplikasi mengukur kebisingan kamar.",
                            color = Color(0xFFD2DCED)
                        )
                        if (isCurrentRecording) {
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
                HorizontalDivider(color = Color(0xFF40506D))
                Text("Riwayat perekaman", style = MaterialTheme.typography.titleLarge)
                if (sessions.isEmpty()) {
                    Text("Belum ada sesi. Mulai rekam, tunggu kalibrasi 12 detik, kemudian buat suara beberapa detik untuk uji.", color = Color(0xFFBFCBE3))
                }
                sessions.forEach { session ->
                    val selected = session.id == selectedId
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF344360) else panel),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { stopPlayback(); selectedId = session.id }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(formatDate(session.startedMs), fontWeight = FontWeight.SemiBold)
                                Text("${formatClock(session.startedMs)} · ${session.clips.size} cuplikan · ${session.status}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (selected) Text("✓", color = accent)
                        }
                    }
                }
                if (current != null) {
                    HorizontalDivider(color = Color(0xFF40506D))
                    Text("Detail sesi", style = MaterialTheme.typography.titleLarge)
                    Text("Mulai: ${formatClock(current.startedMs)} · Durasi sesi: ${formatDuration((current.finishedMs ?: System.currentTimeMillis()) - current.startedMs)}")
                    Text("Status: ${current.status}", color = Color(0xFFBBC8E0))
                    if (current.message.isNotEmpty()) Text("Catatan: ${current.message}", color = Color(0xFFFFD8A0))
                    if (current.clips.isEmpty()) Text("Belum ada suara yang melewati ambang deteksi. Hasil kosong bukan berarti tidur tanpa gangguan.")
                    current.clips.forEach { clip ->
                        ClipCard(
                            session = current, clip = clip, isPlaying = playingClip == clip.id,
                            onPlay = {
                                if (playingClip == clip.id) stopPlayback() else {
                                    stopPlayback()
                                    try {
                                        val file = store.clipFile(current.id, clip)
                                        val next = MediaPlayer().apply {
                                            setDataSource(file.absolutePath)
                                            prepare()
                                            setOnCompletionListener { stopPlayback() }
                                            start()
                                        }
                                        player = next
                                        playingClip = clip.id
                                    } catch (_: Exception) { statusText = "File cuplikan tidak dapat diputar." }
                                }
                            },
                            onLabel = { label -> store.updateLabel(current.id, clip.id, label); refresh() },
                            onDelete = { stopPlayback(); store.removeClip(current.id, clip.id); refresh() },
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
                    store.removeSession(current.id)
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
    onPlay: () -> Unit,
    onLabel: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showLabels by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${formatClock(session.startedMs + clip.offsetMs)} · ${clip.durationMs / 1000}s · ${clip.label}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPlay) { Text(if (isPlaying) "■ Stop" else "▶ Putar") }
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { showLabels = true }) { Text("Label") }
                    DropdownMenu(expanded = showLabels, onDismissRequest = { showLabels = false }) {
                        listOf("Mirip dengkuran", "Mirip berbicara", "Suara sekitar", "Lainnya", "Tidak yakin").forEach { label ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { showLabels = false; onLabel(label) })
                        }
                    }
                }
                TextButton(onClick = { confirmDelete = true }) { Text("Hapus") }
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
