package com.calogeroturco.binauralcompanion.audio

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The 2019 MIT/Martorell auditory waveform: a 1 ms, 10 kHz tone pip every
 * 25 ms. Both channels are identical; this is not a binaural beat.
 */
class GenusTonePipGenerator(
    private val sampleRate: Int,
    private val toneHz: Float = 10_000f,
    repetitionHz: Float = 40f,
    pulseDurationMs: Float = 1f,
) : StereoPcmGenerator {
    val periodFrames: Int = (sampleRate / repetitionHz).roundToInt()
    val pulseFrames: Int = (sampleRate * pulseDurationMs / 1_000f).roundToInt()
    private var frameInPeriod = 0

    init {
        require(sampleRate > 0)
        require(toneHz > 0f && toneHz < sampleRate / 2f)
        require(periodFrames > pulseFrames && pulseFrames > 0)
    }

    override fun fill(target: ShortArray, level: Float, envelope: Float) {
        require(target.size % 2 == 0) { "A stereo buffer must contain pairs of samples." }
        val amplitude = safeAmplitude(level, envelope)
        var index = 0
        while (index < target.size) {
            val value = if (frameInPeriod < pulseFrames) {
                val phase = TWO_PI * toneHz * frameInPeriod / sampleRate
                toPcm(sin(phase) * amplitude)
            } else {
                0
            }
            target[index] = value.toShort()
            target[index + 1] = value.toShort()
            frameInPeriod = (frameInPeriod + 1) % periodFrames
            index += 2
        }
    }
}

/** A 1 kHz carrier with a 100% 40 Hz sinusoidal amplitude envelope. */
class AmplitudeModulatedToneGenerator(
    private val sampleRate: Int,
    private val carrierHz: Float = 1_000f,
    private val modulationHz: Float = 40f,
) : StereoPcmGenerator {
    private var carrierPhase = 0.0
    private var modulationPhase = 0.0
    private val carrierStep = TWO_PI * carrierHz / sampleRate
    private val modulationStep = TWO_PI * modulationHz / sampleRate

    init {
        require(sampleRate > 0)
        require(carrierHz > 0f && carrierHz < sampleRate / 2f)
        require(modulationHz > 0f && modulationHz < carrierHz)
    }

    override fun fill(target: ShortArray, level: Float, envelope: Float) {
        require(target.size % 2 == 0) { "A stereo buffer must contain pairs of samples." }
        val amplitude = safeAmplitude(level, envelope)
        var index = 0
        while (index < target.size) {
            val modulationEnvelope = 0.5 + 0.5 * kotlin.math.cos(modulationPhase)
            val value = toPcm(sin(carrierPhase) * modulationEnvelope * amplitude).toShort()
            target[index] = value
            target[index + 1] = value
            carrierPhase = (carrierPhase + carrierStep) % TWO_PI
            modulationPhase = (modulationPhase + modulationStep) % TWO_PI
            index += 2
        }
    }
}

/** A broadband, alternating-polarity 1 ms click every 25 ms. */
class ClickTrainGenerator(
    private val sampleRate: Int,
    repetitionHz: Float = 40f,
    pulseDurationMs: Float = 1f,
) : StereoPcmGenerator {
    val periodFrames: Int = (sampleRate / repetitionHz).roundToInt()
    val pulseFrames: Int = (sampleRate * pulseDurationMs / 1_000f).roundToInt()
    private var frameInPeriod = 0
    private var polarity = 1

    init {
        require(sampleRate > 0)
        require(periodFrames > pulseFrames && pulseFrames > 0)
    }

    override fun fill(target: ShortArray, level: Float, envelope: Float) {
        require(target.size % 2 == 0) { "A stereo buffer must contain pairs of samples." }
        val amplitude = safeAmplitude(level, envelope)
        var index = 0
        while (index < target.size) {
            val value = if (frameInPeriod < pulseFrames) {
                toPcm(polarity * amplitude)
            } else {
                0
            }
            target[index] = value.toShort()
            target[index + 1] = value.toShort()
            frameInPeriod += 1
            if (frameInPeriod == periodFrames) {
                frameInPeriod = 0
                polarity *= -1
            }
            index += 2
        }
    }
}

fun createGenerator(config: SessionConfig, sampleRate: Int): StereoPcmGenerator =
    when (config.preset.stimulusType) {
        StimulusType.BINAURAL_BEAT -> StereoToneGenerator(
            sampleRate = sampleRate,
            carrierHz = config.carrierHz,
            beatHz = config.startBeatHz,
        )
        StimulusType.GENUS_TONE_PIPS -> GenusTonePipGenerator(sampleRate)
        StimulusType.ASSR_AM_TONE -> AmplitudeModulatedToneGenerator(sampleRate)
        StimulusType.ASSR_CLICK_TRAIN -> ClickTrainGenerator(sampleRate)
    }

private fun safeAmplitude(level: Float, envelope: Float): Double =
    Short.MAX_VALUE.toDouble() *
        level.coerceIn(0f, StereoToneGenerator.MAX_LEVEL).toDouble() *
        envelope.coerceIn(0f, 1f).toDouble()

private fun toPcm(value: Double): Int =
    value.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

private const val TWO_PI = 2.0 * PI
