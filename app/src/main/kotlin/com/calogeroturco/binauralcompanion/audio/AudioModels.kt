package com.calogeroturco.binauralcompanion.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StimulusType(
    val label: String,
    val isBinaural: Boolean,
    val adjustableParameters: Boolean,
) {
    BINAURAL_BEAT("Binaural beat", isBinaural = true, adjustableParameters = true),
    GENUS_TONE_PIPS("40 Hz tone pips", isBinaural = false, adjustableParameters = false),
    ASSR_AM_TONE("40 Hz AM flutter", isBinaural = false, adjustableParameters = false),
    ASSR_CLICK_TRAIN("40 Hz click train", isBinaural = false, adjustableParameters = false),
}

enum class BeatPreset(
    val id: String,
    val title: String,
    val bandLabel: String,
    val startBeatHz: Float,
    val endBeatHz: Float,
    val carrierHz: Float,
    val description: String,
    val benefit: String,
    val defaultMinutes: Int,
    val evidenceLabel: String,
    val stimulusType: StimulusType = StimulusType.BINAURAL_BEAT,
) {
    WIND_DOWN(
        id = "wind_down",
        title = "Wind down",
        bandLabel = "8 → 4 Hz · glide",
        startBeatHz = 8f,
        endBeatHz = 4f,
        carrierHz = 220f,
        description = "A gradual slowing layer for a low-demand evening routine.",
        benefit = "May help you slow down and prepare for rest.",
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
        benefit = "May help you maintain a calm setting for reading or quiet work.",
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
        benefit = "May help you feel alert during a short work block.",
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
        benefit = "May support a short breathing and relaxation break.",
        defaultMinutes = 15,
        evidenceLabel = "Exploratory",
    ),
    LAB_GAMMA(
        id = "lab_gamma",
        title = "Binaural 40",
        bandLabel = "40 Hz · stereo difference",
        startBeatHz = 40f,
        endBeatHz = 40f,
        carrierHz = 400f,
        description = "The gentler gamma option, but it evokes a weaker 40 Hz response than acoustic modulation.",
        benefit = "May offer a gentler way to explore a 40 Hz stereo beat; cognitive benefits are unproven.",
        defaultMinutes = 10,
        evidenceLabel = "Weaker human ASSR",
    ),
    GENUS_40(
        id = "genus_40",
        title = "MIT tone pips",
        bandLabel = "40/s · 1 ms at 10 kHz",
        startBeatHz = 40f,
        endBeatHz = 40f,
        carrierHz = 10_000f,
        description = "Replicates the auditory waveform used by Martorell et al. in mice. It can sound sharp.",
        benefit = "May create a temporary 40 Hz auditory response; human memory or health benefits are unproven.",
        defaultMinutes = 10,
        evidenceLabel = "MIT mouse protocol",
        stimulusType = StimulusType.GENUS_TONE_PIPS,
    ),
    ASSR_AM_40(
        id = "assr_am_40",
        title = "Human AM 40",
        bandLabel = "40 Hz envelope · 1 kHz tone",
        startBeatHz = 40f,
        endBeatHz = 40f,
        carrierHz = 1_000f,
        description = "A 100% amplitude-modulated tone. Robust human ASSR and more distraction-resistant than clicks.",
        benefit = "May create a steady 40 Hz auditory response with less discomfort than clicks; lasting benefits are unproven.",
        defaultMinutes = 10,
        evidenceLabel = "Robust human ASSR",
        stimulusType = StimulusType.ASSR_AM_TONE,
    ),
    ASSR_CLICKS_40(
        id = "assr_clicks_40",
        title = "Human clicks 40",
        bandLabel = "40 clicks/s · 1 ms",
        startBeatHz = 40f,
        endBeatHz = 40f,
        carrierHz = 0f,
        description = "A broadband click train that reliably evokes ASSR, but is more jarring and attention-sensitive.",
        benefit = "May create a clear 40 Hz auditory response for research; everyday cognitive benefits are unproven.",
        defaultMinutes = 10,
        evidenceLabel = "Robust human ASSR",
        stimulusType = StimulusType.ASSR_CLICK_TRAIN,
    ),
    NEUTRAL(
        id = "neutral",
        title = "Neutral control",
        bandLabel = "0 Hz · identical tones",
        startBeatHz = 0f,
        endBeatHz = 0f,
        carrierHz = 220f,
        description = "No binaural difference. Compare it with active sessions.",
        benefit = "Helps you compare active modes with an identical-tone control.",
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
    val musicAssist: Boolean,
)

data class PlaybackSnapshot(
    val isPlaying: Boolean = false,
    val presetTitle: String = "",
    val beatHz: Float = 0f,
    val remainingSeconds: Int = 0,
    val routeLabel: String = "",
    val stimulusLabel: String = "",
    val musicDetected: Boolean = false,
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
            stimulusLabel = config.preset.bandLabel,
        )
    }

    fun progress(
        remainingSeconds: Int,
        routeLabel: String,
        currentBeatHz: Float,
        musicDetected: Boolean,
    ) {
        mutableSnapshot.value = mutableSnapshot.value.copy(
            isPlaying = true,
            beatHz = currentBeatHz,
            remainingSeconds = remainingSeconds.coerceAtLeast(0),
            routeLabel = routeLabel,
            musicDetected = musicDetected,
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
