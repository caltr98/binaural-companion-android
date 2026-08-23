package com.calogeroturco.binauralcompanion.audio

import org.junit.Assert.assertTrue
import org.junit.Test

class AudioModelsTest {
    @Test
    fun everyPresetStatesAPossibleBenefitInPlainLanguage() {
        BeatPreset.entries.forEach { preset ->
            assertTrue("${preset.title} needs a benefit statement", preset.benefit.isNotBlank())
            assertTrue(
                "${preset.title} must describe a possible benefit without promising an outcome",
                preset.benefit.startsWith("May ") || preset.benefit.startsWith("Helps "),
            )
            assertTrue("${preset.title} benefit should remain concise", preset.benefit.length <= 220)
        }
    }

    @Test
    fun everyFortyHertzPresetExplainsTheHumanBenefitBoundary() {
        val fortyHertzPresets = BeatPreset.entries.filter { it.isFortyHz }

        assertTrue("Expected the four current 40 Hz modes", fortyHertzPresets.size >= 4)
        fortyHertzPresets.forEach { preset ->
            assertTrue(preset.benefit.contains("temporarily synchronize auditory brain activity at 40 Hz"))
            assertTrue(preset.benefit.contains("attention or memory-related networks"))
            assertTrue(preset.benefit.contains("benefits are not proven"))
        }
    }
}
