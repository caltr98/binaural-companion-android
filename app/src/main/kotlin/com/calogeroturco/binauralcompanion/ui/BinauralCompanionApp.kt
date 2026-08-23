package com.calogeroturco.binauralcompanion.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calogeroturco.binauralcompanion.audio.AudioRouteMonitor
import com.calogeroturco.binauralcompanion.audio.BeatPreset
import com.calogeroturco.binauralcompanion.audio.BinauralPlaybackService
import com.calogeroturco.binauralcompanion.audio.PlaybackSnapshot
import com.calogeroturco.binauralcompanion.audio.PlaybackStore
import com.calogeroturco.binauralcompanion.audio.ProgressSummary
import com.calogeroturco.binauralcompanion.audio.ReflectionPrompt
import com.calogeroturco.binauralcompanion.audio.SelfRating
import com.calogeroturco.binauralcompanion.audio.SessionConfig
import com.calogeroturco.binauralcompanion.audio.SessionJournal
import com.calogeroturco.binauralcompanion.ui.theme.DeepInk
import com.calogeroturco.binauralcompanion.ui.theme.DeepSurface
import com.calogeroturco.binauralcompanion.ui.theme.Mint
import com.calogeroturco.binauralcompanion.ui.theme.Periwinkle
import com.calogeroturco.binauralcompanion.ui.theme.RaisedSurface
import com.calogeroturco.binauralcompanion.ui.theme.TextSecondary
import com.calogeroturco.binauralcompanion.ui.theme.Warm
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BinauralCompanionApp() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("binaural_companion", Context.MODE_PRIVATE)
    }
    var showSafety by remember { mutableStateOf(!preferences.getBoolean("safety_acknowledged", false)) }
    var showEvidence by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(BeatPreset.CALM_FOCUS) }
    var startBeatHz by remember { mutableFloatStateOf(selectedPreset.startBeatHz) }
    var endBeatHz by remember { mutableFloatStateOf(selectedPreset.endBeatHz) }
    var carrierHz by remember { mutableFloatStateOf(selectedPreset.carrierHz) }
    var level by remember { mutableFloatStateOf(BinauralPlaybackService.DEFAULT_LEVEL) }
    var durationMinutes by remember { mutableIntStateOf(selectedPreset.defaultMinutes) }
    var musicAssist by remember { mutableStateOf(true) }
    var useGatewayPrep by remember { mutableStateOf(true) }
    var pendingStart by remember { mutableStateOf<PendingStart?>(null) }
    var gatewayPrepRequest by remember { mutableStateOf<PendingStart?>(null) }
    var checkInRequest by remember { mutableStateOf<PendingStart?>(null) }

    val playback by PlaybackStore.snapshot.collectAsStateWithLifecycle()
    val journalPrompt by SessionJournal.prompt.collectAsStateWithLifecycle()
    val progress by SessionJournal.progress.collectAsStateWithLifecycle()
    DisposableEffect(context.applicationContext) {
        SessionJournal.initialize(context.applicationContext)
        onDispose { }
    }
    val routeMonitor = remember { AudioRouteMonitor(context.applicationContext) }
    val route by routeMonitor.status.collectAsStateWithLifecycle()
    DisposableEffect(routeMonitor) {
        onDispose { routeMonitor.close() }
    }

    fun executeStart(request: PendingStart) {
        runCatching {
            BinauralPlaybackService.start(context, request.config)
            if (request.openSpotify) openSpotify(context)
        }.onFailure {
            SessionJournal.cancelActive(context)
            Toast.makeText(context, "The audio session could not start.", Toast.LENGTH_LONG).show()
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        pendingStart?.let(::executeStart)
        pendingStart = null
    }

    fun continueAfterCheckIn(request: PendingStart, before: SelfRating) {
        SessionJournal.begin(context, request.config, before)
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingStart = request
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            executeStart(request)
        }
    }

    fun requestStart(openSpotify: Boolean) {
        val request = PendingStart(
            config = SessionConfig(
                preset = selectedPreset,
                startBeatHz = startBeatHz,
                endBeatHz = endBeatHz,
                carrierHz = carrierHz,
                level = level,
                durationMinutes = durationMinutes,
                musicAssist = musicAssist,
            ),
            openSpotify = openSpotify,
        )
        if (useGatewayPrep) {
            gatewayPrepRequest = request
        } else {
            checkInRequest = request
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepInk),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { AppHeader(onEvidence = { showEvidence = true }) }
            item {
                SessionHero(
                    playback = playback,
                    selectedPreset = selectedPreset,
                    startBeatHz = startBeatHz,
                    endBeatHz = endBeatHz,
                )
            }
            item { RouteCard(label = if (playback.isPlaying) playback.routeLabel else route.label, ready = route.headphonesReady) }
            item { ProgressCard(progress) }
            item {
                SectionTitle(
                    eyebrow = "01 · CHOOSE AN INTENTION",
                    title = "Train one state at a time",
                    supporting = "Names are goals for a session, not guaranteed brain states. Use the ratings to learn your own response.",
                )
            }
            item {
                PresetRow(
                    selected = selectedPreset,
                    enabled = !playback.isPlaying,
                    onSelected = { preset ->
                        selectedPreset = preset
                        startBeatHz = preset.startBeatHz
                        endBeatHz = preset.endBeatHz
                        carrierHz = preset.carrierHz
                        durationMinutes = preset.defaultMinutes
                    },
                )
            }
            item {
                AdvancedControls(
                    preset = selectedPreset,
                    startBeatHz = startBeatHz,
                    endBeatHz = endBeatHz,
                    carrierHz = carrierHz,
                    level = level,
                    musicAssist = musicAssist,
                    enabled = !playback.isPlaying,
                    onStartBeatChanged = { startBeatHz = it },
                    onEndBeatChanged = { endBeatHz = it },
                    onCarrierChanged = { carrierHz = it },
                    onLevelChanged = { level = it },
                    onMusicAssistChanged = { musicAssist = it },
                )
            }
            item {
                GatewayPrepCard(
                    selected = useGatewayPrep,
                    enabled = !playback.isPlaying,
                    onSelected = { useGatewayPrep = it },
                )
            }
            item {
                SectionTitle(
                    eyebrow = "02 · SET A BOUNDARY",
                    title = "Session length",
                    supporting = "The layer fades in gently and stops automatically.",
                )
            }
            item {
                DurationSelector(
                    selected = durationMinutes,
                    enabled = !playback.isPlaying,
                    onSelected = { durationMinutes = it },
                )
            }
            item {
                PlaybackActions(
                    playback = playback,
                    canStart = route.headphonesReady,
                    onStartWithSpotify = { requestStart(openSpotify = true) },
                    onStartOnly = { requestStart(openSpotify = false) },
                    onStop = { BinauralPlaybackService.stop(context) },
                )
            }
            item { IntegrationNote() }
            item { PrivacyNote() }
            item { Footer() }
        }
    }

    if (showSafety) {
        SafetyDialog(
            onAccept = {
                preferences.edit().putBoolean("safety_acknowledged", true).apply()
                showSafety = false
            },
        )
    }
    if (showEvidence) {
        EvidenceDialog(onDismiss = { showEvidence = false })
    }
    gatewayPrepRequest?.let { request ->
        GatewayPrepDialog(
            onCancel = { gatewayPrepRequest = null },
            onContinue = {
                gatewayPrepRequest = null
                checkInRequest = request
            },
        )
    }
    checkInRequest?.let { request ->
        RatingDialog(
            title = "Before ${request.config.preset.title}",
            supporting = "Rate how you feel now. This stays only on your phone.",
            confirmLabel = "Start session",
            onCancel = { checkInRequest = null },
            onConfirm = { rating ->
                checkInRequest = null
                continueAfterCheckIn(request, rating)
            },
        )
    }
    if (journalPrompt != null && !playback.isPlaying && checkInRequest == null) {
        ReflectionDialog(
            prompt = journalPrompt!!,
            onSkip = { SessionJournal.skip(context) },
            onSubmit = { SessionJournal.submit(context, it) },
        )
    }
}

private data class PendingStart(val config: SessionConfig, val openSpotify: Boolean)

@Composable
private fun AppHeader(onEvidence: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "AUDITORY",
                color = Mint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.4.sp,
            )
            Text(
                text = "Companion",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        TextButton(onClick = onEvidence) {
            Text("How it works")
        }
    }
}

@Composable
private fun SessionHero(
    playback: PlaybackSnapshot,
    selectedPreset: BeatPreset,
    startBeatHz: Float,
    endBeatHz: Float,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WaveField(isPlaying = playback.isPlaying)
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (playback.isPlaying) {
                    formatTime(playback.remainingSeconds)
                } else {
                    formatBeatRange(startBeatHz, endBeatHz)
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (playback.isPlaying) 42.sp else 34.sp,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = if (playback.isPlaying) {
                    "${playback.presetTitle} · ${playback.stimulusLabel}"
                } else {
                    "${selectedPreset.title} · ready to layer"
                },
                color = TextSecondary,
                fontSize = 15.sp,
            )
            if (playback.isPlaying && playback.musicDetected) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Concurrent music detected · adaptive layer active",
                    color = Mint,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
            if (playback.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = playback.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: ProgressSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Mint.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "PRIVATE RESPONSE LOG",
                color = Mint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = if (progress.ratedSessions == 0) {
                    "No rated sessions yet"
                } else {
                    "${progress.ratedSessions} rated ${if (progress.ratedSessions == 1) "session" else "sessions"}"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = progress.lastResult ?: "Before/after ratings will show what actually helps you.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun WaveField(isPlaying: Boolean) {
    // Keep the visualization static between state changes. The sound is generated on its own
    // worker thread; a permanent UI animation would waste battery before Spotify is opened.
    val phase = if (isPlaying) 0.42f else 0f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
    ) {
        drawCircle(
            color = Mint.copy(alpha = 0.06f),
            radius = size.minDimension * 0.46f,
            center = center,
        )
        fun wavePath(offset: Float, cycles: Float): Path = Path().apply {
            val points = 100
            repeat(points + 1) { index ->
                val x = size.width * index / points
                val angle = cycles * 2f * PI.toFloat() * index / points + phase + offset
                val y = center.y + sin(angle) * size.height * 0.20f
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = wavePath(0f, 2.2f),
            color = Mint,
            style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = wavePath(0.72f, 2.45f),
            color = Periwinkle.copy(alpha = 0.86f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun RouteCard(label: String, ready: Boolean) {
    val color = if (ready) Mint else Warm
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (ready) "STEREO OUTPUT READY" else "HEADPHONES REQUIRED",
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )
                Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(eyebrow: String, title: String, supporting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = eyebrow,
            color = Mint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = supporting, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun PresetRow(
    selected: BeatPreset,
    enabled: Boolean,
    onSelected: (BeatPreset) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BeatPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Card(
                modifier = Modifier
                    .width(184.dp)
                    .clickable(enabled = enabled) { onSelected(preset) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Mint.copy(alpha = 0.15f) else DeepSurface,
                ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, Mint.copy(alpha = 0.75f))
                } else null,
            ) {
                Column(
                    modifier = Modifier.padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = preset.title,
                        color = if (isSelected) Mint else MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(text = preset.bandLabel, color = Periwinkle, fontSize = 12.sp)
                    Text(
                        text = preset.evidenceLabel.uppercase(),
                        color = Warm,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                    Text(
                        text = preset.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedControls(
    preset: BeatPreset,
    startBeatHz: Float,
    endBeatHz: Float,
    carrierHz: Float,
    level: Float,
    musicAssist: Boolean,
    enabled: Boolean,
    onStartBeatChanged: (Float) -> Unit,
    onEndBeatChanged: (Float) -> Unit,
    onCarrierChanged: (Float) -> Unit,
    onLevelChanged: (Float) -> Unit,
    onMusicAssistChanged: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (preset.stimulusType.adjustableParameters) {
                ControlLabel("Start difference", "${formatHz(startBeatHz)} Hz")
                Slider(
                    value = startBeatHz,
                    onValueChange = onStartBeatChanged,
                    enabled = enabled,
                    valueRange = 0f..40f,
                    steps = 39,
                )
                ControlLabel("End difference", "${formatHz(endBeatHz)} Hz")
                Slider(
                    value = endBeatHz,
                    onValueChange = onEndBeatChanged,
                    enabled = enabled,
                    valueRange = 0f..40f,
                    steps = 39,
                )
                Text(
                    text = "Use the same values for a steady layer, or different values for a gradual glide. 0 Hz is the neutral control.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                HorizontalDivider(color = RaisedSurface)
                ControlLabel("Carrier tone", "${carrierHz.roundToInt()} Hz")
                Slider(
                    value = carrierHz,
                    onValueChange = onCarrierChanged,
                    enabled = enabled,
                    valueRange = 160f..400f,
                    steps = 23,
                )
            } else {
                Text(
                    text = preset.stimulusType.label.uppercase(),
                    color = Periwinkle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )
                Text(
                    text = preset.bandLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Research waveform parameters are fixed so the signal is not accidentally changed into a different protocol.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            HorizontalDivider(color = RaisedSurface)
            ControlLabel("Layer level", "${(level * 100).roundToInt()}%")
            Slider(
                value = level,
                onValueChange = onLevelChanged,
                enabled = enabled,
                valueRange = 0.02f..0.12f,
                steps = 9,
            )
            Text(
                text = "Keep the layer faint beneath your music. Your phone's media volume still controls the final output.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            HorizontalDivider(color = RaisedSurface)
            FilterChip(
                selected = musicAssist,
                onClick = { onMusicAssistChanged(!musicAssist) },
                enabled = enabled,
                label = { Text(if (musicAssist) "Music assist on" else "Music assist off") },
            )
            Text(
                text = "When another media stream is active, Music assist smoothly raises only this local layer by 25%. It detects playback state, not song audio, and never records Spotify.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            if (!preset.stimulusType.isBinaural) {
                Text(
                    text = "For the closest research-waveform comparison, turn Music assist off and play this mode alone. Adding Spotify is a separate, unvalidated listening condition.",
                    color = Warm,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun GatewayPrepCard(
    selected: Boolean,
    enabled: Boolean,
    onSelected: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Periwinkle.copy(alpha = 0.09f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = "GATEWAY-INSPIRED PREPARATION",
                color = Periwinkle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = "Add a 3-minute mental setup",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Set distractions aside, breathe slowly, choose one concrete intention, and relax the body while staying alert.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            FilterChip(
                selected = selected,
                onClick = { onSelected(!selected) },
                enabled = enabled,
                label = { Text(if (selected) "Preparation on" else "Preparation off") },
            )
            Text(
                text = "Adapted from a declassified Army paper in the CIA archive; archive status is not scientific validation.",
                color = Warm,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun ControlLabel(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        Text(text = value, color = Mint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DurationSelector(selected: Int, enabled: Boolean, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(10, 20, 30, 45).forEach { minutes ->
            FilterChip(
                selected = selected == minutes,
                onClick = { onSelected(minutes) },
                enabled = enabled,
                label = { Text("$minutes min") },
            )
        }
    }
}

@Composable
private fun PlaybackActions(
    playback: PlaybackSnapshot,
    canStart: Boolean,
    onStartWithSpotify: () -> Unit,
    onStartOnly: () -> Unit,
    onStop: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (playback.isPlaying) {
            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Warm,
                    contentColor = DeepInk,
                ),
            ) {
                Text("Stop audio layer", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onStartWithSpotify,
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
            ) {
                Text("Start layer + open Spotify", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onStartOnly,
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Start layer only")
            }
            if (!canStart) {
                Text(
                    text = "Connect headphones to unlock playback.",
                    modifier = Modifier.fillMaxWidth(),
                    color = Warm,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun IntegrationNote() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Periwinkle.copy(alpha = 0.09f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("SEAMLESS, WITHOUT CAPTURE", color = Periwinkle, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Spotify keeps the music. This app adds the layer.",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Android mixes both apps sample-by-sample into the same headphone output. Music assist detects a concurrent player and smoothly adjusts only the generated layer; it never records, intercepts, downloads, or rewrites Spotify.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Mint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = Mint, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Private by design",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text("No microphone · no account · no analytics · no network permission", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Footer() {
    Text(
        text = "Independent educational prototype. Not affiliated with Spotify, the Monroe Institute, or the owners of the Hemi-Sync trademark.",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        color = TextSecondary.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    )
}

@Composable
private fun SafetyDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Listen safely") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SafetyLine("Use stereo headphones at a comfortable volume.")
                SafetyLine("The tone-pip and click modes can feel sharp; begin very low and never use them to test hearing limits.")
                SafetyLine("Do not use while driving or operating machinery.")
                SafetyLine("Stop immediately if you feel discomfort, dizziness, or distress.")
                SafetyLine("This is an audio experience, not medical treatment or a substitute for care.")
                Text(
                    text = "If you have a seizure disorder, neurological condition, or other health concern, ask a qualified clinician before use.",
                    color = Warm,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("I understand")
            }
        },
    )
}

@Composable
private fun SafetyLine(text: String) {
    Row {
        Text("•", color = Mint)
        Spacer(Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun GatewayPrepDialog(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("3-minute preparation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SafetyLine("Park distractions: imagine placing today's concerns in a box until the session ends.")
                SafetyLine("Take four slow, comfortable breaths. A gentle hum on the exhale is optional.")
                SafetyLine("Name one observable intention, such as finishing a reading block or feeling calmer.")
                SafetyLine("Release body tension while keeping your attention awake and curious.")
                Text(
                    text = "These are ordinary relaxation and intention practices adapted from the paper—not a route to paranormal abilities.",
                    color = Warm,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
        confirmButton = { Button(onClick = onContinue) { Text("Preparation complete") } },
    )
}

@Composable
private fun RatingDialog(
    title: String,
    supporting: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: (SelfRating) -> Unit,
) {
    var calm by remember { mutableIntStateOf(3) }
    var focus by remember { mutableIntStateOf(3) }
    var energy by remember { mutableIntStateOf(3) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = supporting, color = TextSecondary, lineHeight = 19.sp)
                RatingRow("Calm", calm) { calm = it }
                RatingRow("Focus", focus) { focus = it }
                RatingRow("Energy", energy) { energy = it }
                Text("1 = low · 5 = high", color = TextSecondary, fontSize = 12.sp)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
        confirmButton = {
            Button(onClick = { onConfirm(SelfRating(calm, focus, energy)) }) {
                Text(confirmLabel)
            }
        },
    )
}

@Composable
private fun RatingRow(label: String, selected: Int, onSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            SelfRating.RATING_RANGE.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(value.toString()) },
                )
            }
        }
    }
}

@Composable
private fun ReflectionDialog(
    prompt: ReflectionPrompt,
    onSkip: () -> Unit,
    onSubmit: (SelfRating) -> Unit,
) {
    RatingDialog(
        title = "After ${prompt.presetTitle}",
        supporting = "Rate how you feel now. The app compares this with your private baseline.",
        confirmLabel = "Save comparison",
        onCancel = onSkip,
        onConfirm = onSubmit,
    )
}

@Composable
private fun EvidenceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How this prototype works") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Acoustics",
                    color = Mint,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Binaural modes send slightly different tones to the two ears. GENUS tone pips, AM flutter, and click trains instead place a real 40 Hz pattern in both channels, which generally evokes a stronger auditory steady-state response.",
                    color = TextSecondary,
                    lineHeight = 20.sp,
                )
                Text("Evidence boundary", color = Mint, fontWeight = FontWeight.Bold)
                Text(
                    text = "The 1 ms, 10 kHz MIT auditory protocol comes from a 2019 mouse study, not the 2016 light-flicker paper. Human ASSR shows neural phase-locking to 40 Hz sounds, but that is not proof of treatment, cognitive enhancement, or everyday benefit. Music can mask a quiet binaural layer; Music assist improves audibility but cannot verify brain entrainment without EEG.",
                    color = TextSecondary,
                    lineHeight = 20.sp,
                )
                Text("CIA archive boundary", color = Mint, fontWeight = FontWeight.Bold)
                Text(
                    text = "The 1983 Gateway paper describes intention, breathing, relaxation, stereo stimulation, and many speculative claims. Another archived Army review says concentration was not convincingly demonstrated and medical claims lacked acceptable studies. Declassification means public access—not CIA endorsement or proof.",
                    color = TextSecondary,
                    lineHeight = 20.sp,
                )
                Text(
                    text = "This app supports state-training routines. It does not produce superhuman, paranormal, diagnostic, or therapeutic abilities.",
                    color = Warm,
                    lineHeight = 20.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

private fun openSpotify(context: Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
    if (launchIntent != null) {
        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    Toast.makeText(context, "Spotify is not installed; opening Spotify on the web.", Toast.LENGTH_LONG).show()
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds.coerceAtLeast(0) / 60
    val seconds = totalSeconds.coerceAtLeast(0) % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatHz(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)

private fun formatBeatRange(startBeatHz: Float, endBeatHz: Float): String =
    if (startBeatHz == endBeatHz) {
        "${formatHz(startBeatHz)} Hz"
    } else {
        "${formatHz(startBeatHz)} → ${formatHz(endBeatHz)} Hz"
    }

private const val SPOTIFY_PACKAGE = "com.spotify.music"
