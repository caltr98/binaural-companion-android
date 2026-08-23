package com.calogeroturco.binauralcompanion.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GammaStimulusGeneratorsTest {
    @Test
    fun genusWaveformUsesOneMillisecondPipsEveryTwentyFiveMilliseconds() {
        val generator = GenusTonePipGenerator(sampleRate = SAMPLE_RATE)
        val buffer = ShortArray(generator.periodFrames * 2 * 2)

        generator.fill(buffer, level = 0.06f)

        assertEquals(1_200, generator.periodFrames)
        assertEquals(48, generator.pulseFrames)
        assertTrue((0 until generator.pulseFrames).any { left(buffer, it) != 0.toShort() })
        assertTrue((generator.pulseFrames until generator.periodFrames).all { left(buffer, it) == 0.toShort() })
        assertTrue((generator.periodFrames until generator.periodFrames + generator.pulseFrames)
            .any { left(buffer, it) != 0.toShort() })
        assertTrue(buffer.indices.step(2).all { buffer[it] == buffer[it + 1] })
    }

    @Test
    fun amplitudeModulatedTonePlacesFortyHertzSidebandsInBothChannels() {
        val generator = AmplitudeModulatedToneGenerator(sampleRate = SAMPLE_RATE)
        val buffer = ShortArray(SAMPLE_RATE * 2)

        generator.fill(buffer, level = 0.06f)

        val carrier = spectralMagnitude(buffer, channel = 0, frequencyHz = 1_000)
        val lowerSideband = spectralMagnitude(buffer, channel = 0, frequencyHz = 960)
        val upperSideband = spectralMagnitude(buffer, channel = 0, frequencyHz = 1_040)
        assertTrue(carrier > 100.0)
        assertTrue(lowerSideband > carrier * 0.35)
        assertTrue(upperSideband > carrier * 0.35)
        assertTrue(buffer.indices.step(2).all { buffer[it] == buffer[it + 1] })
    }

    @Test
    fun clickTrainAlternatesPolarityAndKeepsSilenceBetweenClicks() {
        val generator = ClickTrainGenerator(sampleRate = SAMPLE_RATE)
        val buffer = ShortArray(generator.periodFrames * 2 * 2)

        generator.fill(buffer, level = 0.04f)

        assertTrue((0 until generator.pulseFrames).all { left(buffer, it) > 0 })
        assertTrue((generator.pulseFrames until generator.periodFrames).all { left(buffer, it) == 0.toShort() })
        assertTrue((generator.periodFrames until generator.periodFrames + generator.pulseFrames)
            .all { left(buffer, it) < 0 })
    }

    @Test
    fun sampleWiseMusicMixPreservesBothBinauralCarriers() {
        val generator = StereoToneGenerator(SAMPLE_RATE, carrierHz = 205f, beatHz = 10f)
        val layer = ShortArray(SAMPLE_RATE * 2)
        generator.fill(layer, level = 0.04f)

        val mixed = layer.copyOf()
        repeat(SAMPLE_RATE) { frame ->
            val leftMusic = sin(TWO_PI * 523.0 * frame / SAMPLE_RATE) * Short.MAX_VALUE * 0.08
            val rightMusic = sin(TWO_PI * 659.0 * frame / SAMPLE_RATE) * Short.MAX_VALUE * 0.08
            mixed[frame * 2] = (mixed[frame * 2] + leftMusic).toInt().toShort()
            mixed[frame * 2 + 1] = (mixed[frame * 2 + 1] + rightMusic).toInt().toShort()
        }

        val originalLeft = spectralMagnitude(layer, channel = 0, frequencyHz = 200)
        val originalRight = spectralMagnitude(layer, channel = 1, frequencyHz = 210)
        assertTrue(spectralMagnitude(mixed, channel = 0, frequencyHz = 200) > originalLeft * 0.95)
        assertTrue(spectralMagnitude(mixed, channel = 1, frequencyHz = 210) > originalRight * 0.95)
    }

    @Test
    fun musicAssistIsModestAndNeverExceedsGeneratorCap() {
        assertEquals(0.075f, MusicMixPolicy.effectiveLevel(0.06f, true, true), 0.0001f)
        assertEquals(0.06f, MusicMixPolicy.effectiveLevel(0.06f, false, true), 0.0001f)
        assertEquals(0.06f, MusicMixPolicy.effectiveLevel(0.06f, true, false), 0.0001f)
        assertEquals(
            StereoToneGenerator.MAX_LEVEL,
            MusicMixPolicy.effectiveLevel(StereoToneGenerator.MAX_LEVEL, true, true),
            0.0001f,
        )
    }

    private fun left(buffer: ShortArray, frame: Int): Short = buffer[frame * 2]

    private fun spectralMagnitude(buffer: ShortArray, channel: Int, frequencyHz: Int): Double {
        var real = 0.0
        var imaginary = 0.0
        val frames = buffer.size / 2
        repeat(frames) { frame ->
            val angle = TWO_PI * frequencyHz * frame / SAMPLE_RATE
            val sample = buffer[frame * 2 + channel].toDouble()
            real += sample * cos(angle)
            imaginary -= sample * sin(angle)
        }
        return kotlin.math.sqrt(real * real + imaginary * imaginary) / frames
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val TWO_PI = 2.0 * PI
    }
}
