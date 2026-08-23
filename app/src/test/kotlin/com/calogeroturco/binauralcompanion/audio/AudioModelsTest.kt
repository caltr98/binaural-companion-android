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
            assertTrue("${preset.title} benefit should remain concise", preset.benefit.length <= 130)
        }
    }
}
