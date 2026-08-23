package com.calogeroturco.binauralcompanion.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BeatPreset(
    val id: String,
    val title: String,
    val bandLabel: String,
    val startBeatHz: Float,
    val endBeatHz: Float,
    val carrierHz: Float,
    val description: String,
    val defaultMinutes: Int,
    val evidenceLabel: String,
) {
    WIND_DOWN(
        id = "wind_down",
        title = "Wind down",
        bandLabel = "8 → 4 Hz · glide",
        startBeatHz = 8f,
        endBeatHz = 4f,
        carrierHz = 220f,
        description = "A gradual slowing layer for a low-demand evening routine.",
        defaultMinutes = 30,
        evidenceLabel = "Exploratory",
    ),
    CALM_FOCUS(
        id = "calm_focus",
        title = "Calm focus",
        bandLabel = "10 Hz · alpha-range",
        startBeatHz = 10f,
        endBeatHz = 10f,
        carrierHz = 220f,
        description = "A steady layer for reading, planning, or quiet concentration.",
        defaultMinutes = 20,
        evidenceLabel = "Mixed evidence",
    ),
    DEEP_WORK(
        id = "deep_work",
        title = "Deep work",
        bandLabel = "16 Hz · beta-range",
        startBeatHz = 16f,
        endBeatHz = 16f,
        carrierHz = 240f,
        description = "A quicker pulse to test during a clearly defined work block.",
        defaultMinutes = 25,
        evidenceLabel = "Uncertain",
    ),
    RESET(
        id = "reset",
        title = "Reset",
        bandLabel = "7 Hz · theta-range",
        startBeatHz = 7f,
        endBeatHz = 7f,
        carrierHz = 220f,
        description = "A short pause for breathing, body relaxation, and reflection.",
        defaultMinutes = 15,
        evidenceLabel = "Exploratory",
    ),
    LAB_GAMMA(
        id = "lab_gamma",
        title = "Lab 40",
        bandLabel = "40 Hz · gamma-range",
        startBeatHz = 40f,
        endBeatHz = 40f,
        carrierHz = 340f,
        description = "A brief, clearly experimental attention setting.",
        defaultMinutes = 10,
        evidenceLabel = "Early evidence",
    ),
    NEUTRAL(
        id = "neutral",
        title = "Neutral control",
        bandLabel = "0 Hz · identical tones",
        startBeatHz = 0f,
        endBeatHz = 0f,
        carrierHz = 220f,
        description = "No binaural difference. Compare it with active sessions.",
        defaultMinutes = 10,
        evidenceLabel = "A/B control",
    );

    companion object {
        fun fromId(id: String?): BeatPreset = entries.firstOrNull { it.id == id } ?: CALM_FOCUS
    }
}

data class SessionConfig(
    val preset: BeatPreset,
    val startBeatHz: Float,
    val endBeatHz: Float,
    val carrierHz: Float,
    val level: Float,
    val durationMinutes: Int,
)

data class PlaybackSnapshot(
    val isPlaying: Boolean = false,
    val presetTitle: String = "",
    val beatHz: Float = 0f,
    val remainingSeconds: Int = 0,
    val routeLabel: String = "",
    val errorMessage: String? = null,
)

object PlaybackStore {
    private val mutableSnapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = mutableSnapshot.asStateFlow()

    fun begin(config: SessionConfig) {
        mutableSnapshot.value = PlaybackSnapshot(
            isPlaying = true,
            presetTitle = config.preset.title,
            beatHz = config.startBeatHz,
            remainingSeconds = config.durationMinutes * 60,
            routeLabel = "Connecting to audio output…",
        )
    }

    fun progress(remainingSeconds: Int, routeLabel: String, currentBeatHz: Float) {
        mutableSnapshot.value = mutableSnapshot.value.copy(
            isPlaying = true,
            beatHz = currentBeatHz,
            remainingSeconds = remainingSeconds.coerceAtLeast(0),
            routeLabel = routeLabel,
            errorMessage = null,
        )
    }

    fun stop() {
        mutableSnapshot.value = PlaybackSnapshot()
    }

    fun fail(message: String) {
        mutableSnapshot.value = PlaybackSnapshot(errorMessage = message)
    }
}
