package com.calogeroturco.binauralcompanion.audio

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

interface StereoPcmGenerator {
    fun fill(target: ShortArray, level: Float, envelope: Float = 1f)
}

/** Pure PCM generator with a carrier-centered frequency difference between channels. */
class StereoToneGenerator(
    val sampleRate: Int,
    val carrierHz: Float,
    beatHz: Float,
) : StereoPcmGenerator {
    var beatHz: Float = beatHz
        private set
    var leftFrequencyHz: Float = 0f
        private set
    var rightFrequencyHz: Float = 0f
        private set

    private var leftPhase = 0.0
    private var rightPhase = 0.0
    private var leftStep = 0.0
    private var rightStep = 0.0

    init {
        require(sampleRate > 0)
        require(carrierHz in 80f..900f)
        updateBeatHz(beatHz)
    }

    /** Changes the perceived difference without resetting phase, so glides remain click-free. */
    fun updateBeatHz(value: Float) {
        require(value in 0f..40f)
        beatHz = value
        leftFrequencyHz = carrierHz - value / 2f
        rightFrequencyHz = carrierHz + value / 2f
        leftStep = TWO_PI * leftFrequencyHz / sampleRate
        rightStep = TWO_PI * rightFrequencyHz / sampleRate
    }

    override fun fill(target: ShortArray, level: Float, envelope: Float) {
        require(target.size % 2 == 0) { "A stereo buffer must contain pairs of samples." }
        val amplitude = Short.MAX_VALUE * level.coerceIn(0f, MAX_LEVEL) * envelope.coerceIn(0f, 1f)

        var index = 0
        while (index < target.size) {
            target[index] = (sin(leftPhase) * amplitude)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            target[index + 1] = (sin(rightPhase) * amplitude)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()

            leftPhase = (leftPhase + leftStep) % TWO_PI
            rightPhase = (rightPhase + rightStep) % TWO_PI
            index += 2
        }
    }

    companion object {
        const val MAX_LEVEL = 0.16f
        private const val TWO_PI = 2.0 * PI
    }
}
