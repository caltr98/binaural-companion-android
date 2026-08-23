package com.calogeroturco.binauralcompanion.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StereoToneGeneratorTest {
    @Test
    fun channelsAreCenteredAroundCarrier() {
        val generator = StereoToneGenerator(
            sampleRate = 48_000,
            carrierHz = 200f,
            beatHz = 10f,
        )

        assertEquals(195f, generator.leftFrequencyHz)
        assertEquals(205f, generator.rightFrequencyHz)
        assertEquals(10f, generator.rightFrequencyHz - generator.leftFrequencyHz)
    }

    @Test
    fun neutralControlUsesIdenticalFrequencies() {
        val generator = StereoToneGenerator(48_000, 220f, 0f)

        assertEquals(220f, generator.leftFrequencyHz)
        assertEquals(220f, generator.rightFrequencyHz)
        val buffer = ShortArray(4_800)
        generator.fill(buffer, level = 0.06f)
        assertTrue(buffer.indices.step(2).all { buffer[it] == buffer[it + 1] })
    }

    @Test
    fun beatCanGlideWithoutRecreatingGenerator() {
        val generator = StereoToneGenerator(48_000, 220f, 8f)

        generator.updateBeatHz(4f)

        assertEquals(4f, generator.beatHz)
        assertEquals(218f, generator.leftFrequencyHz)
        assertEquals(222f, generator.rightFrequencyHz)
    }

    @Test
    fun generatedStereoChannelsDivergeAndStayAtSafeAmplitude() {
        val generator = StereoToneGenerator(48_000, 200f, 10f)
        val buffer = ShortArray(4_800)

        generator.fill(buffer, level = 0.06f)

        assertTrue(buffer.any { it != 0.toShort() })
        assertTrue(buffer.indices.step(2).any { buffer[it] != buffer[it + 1] })
        assertTrue(buffer.maxOf { kotlin.math.abs(it.toInt()) } <= (Short.MAX_VALUE * 0.06f).toInt() + 1)
    }

    @Test
    fun presetIdsRoundTrip() {
        BeatPreset.entries.forEach { preset ->
            assertEquals(preset, BeatPreset.fromId(preset.id))
        }
        assertNotEquals(BeatPreset.WIND_DOWN, BeatPreset.fromId("missing"))
        assertEquals(BeatPreset.CALM_FOCUS, BeatPreset.fromId("missing"))
    }
}
