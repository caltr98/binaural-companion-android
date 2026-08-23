package com.calogeroturco.binauralcompanion.audio

/**
 * Android performs the actual sample mixing. This policy only makes the locally generated
 * stream modestly more audible when another media player is active; it never captures or
 * analyzes that player's audio.
 */
object MusicMixPolicy {
    const val MUSIC_GAIN = 1.25f

    fun effectiveLevel(baseLevel: Float, musicDetected: Boolean, musicAssist: Boolean): Float {
        val safeBase = baseLevel.coerceIn(0f, StereoToneGenerator.MAX_LEVEL)
        return if (musicDetected && musicAssist) {
            (safeBase * MUSIC_GAIN).coerceAtMost(StereoToneGenerator.MAX_LEVEL)
        } else {
            safeBase
        }
    }
}
