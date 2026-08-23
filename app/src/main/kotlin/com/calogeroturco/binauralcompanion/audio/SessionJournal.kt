package com.calogeroturco.binauralcompanion.audio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SelfRating(
    val calm: Int,
    val focus: Int,
    val energy: Int,
) {
    init {
        require(calm in RATING_RANGE && focus in RATING_RANGE && energy in RATING_RANGE)
    }

    companion object {
        val RATING_RANGE = 1..5
    }
}

data class ReflectionPrompt(
    val presetTitle: String,
    val before: SelfRating,
)

data class ProgressSummary(
    val ratedSessions: Int = 0,
    val lastResult: String? = null,
)

/** A tiny, private journal backed only by app-local SharedPreferences. */
object SessionJournal {
    private val mutablePrompt = MutableStateFlow<ReflectionPrompt?>(null)
    val prompt: StateFlow<ReflectionPrompt?> = mutablePrompt.asStateFlow()

    private val mutableProgress = MutableStateFlow(ProgressSummary())
    val progress: StateFlow<ProgressSummary> = mutableProgress.asStateFlow()

    fun initialize(context: Context) {
        val preferences = preferences(context)
        mutableProgress.value = ProgressSummary(
            ratedSessions = preferences.getInt(KEY_RATED_SESSIONS, 0),
            lastResult = preferences.getString(KEY_LAST_RESULT, null),
        )
        mutablePrompt.value = if (
            preferences.getBoolean(KEY_ACTIVE, false) &&
            preferences.getBoolean(KEY_ENDED, false)
        ) {
            preferences.readPrompt()
        } else {
            null
        }
    }

    fun begin(context: Context, config: SessionConfig, before: SelfRating) {
        preferences(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_ENDED, false)
            .putString(KEY_PRESET_TITLE, config.preset.title)
            .putInt(KEY_BEFORE_CALM, before.calm)
            .putInt(KEY_BEFORE_FOCUS, before.focus)
            .putInt(KEY_BEFORE_ENERGY, before.energy)
            .apply()
        mutablePrompt.value = null
    }

    fun markEnded(context: Context) {
        val preferences = preferences(context)
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return
        preferences.edit().putBoolean(KEY_ENDED, true).apply()
        mutablePrompt.value = preferences.readPrompt()
    }

    fun submit(context: Context, after: SelfRating) {
        val preferences = preferences(context)
        val prompt = preferences.readPrompt() ?: return
        val result = listOf(
            "Calm ${formatDelta(after.calm - prompt.before.calm)}",
            "Focus ${formatDelta(after.focus - prompt.before.focus)}",
            "Energy ${formatDelta(after.energy - prompt.before.energy)}",
        ).joinToString(" · ")
        val total = preferences.getInt(KEY_RATED_SESSIONS, 0) + 1
        preferences.edit()
            .putInt(KEY_RATED_SESSIONS, total)
            .putString(KEY_LAST_RESULT, "${prompt.presetTitle}: $result")
            .removeActiveSession()
            .apply()
        mutablePrompt.value = null
        mutableProgress.value = ProgressSummary(total, "${prompt.presetTitle}: $result")
    }

    fun skip(context: Context) {
        preferences(context).edit().removeActiveSession().apply()
        mutablePrompt.value = null
    }

    fun cancelActive(context: Context) = skip(context)

    private fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun SharedPreferences.readPrompt(): ReflectionPrompt? {
        if (!getBoolean(KEY_ACTIVE, false)) return null
        return ReflectionPrompt(
            presetTitle = getString(KEY_PRESET_TITLE, null) ?: return null,
            before = SelfRating(
                calm = getInt(KEY_BEFORE_CALM, 3).coerceIn(SelfRating.RATING_RANGE),
                focus = getInt(KEY_BEFORE_FOCUS, 3).coerceIn(SelfRating.RATING_RANGE),
                energy = getInt(KEY_BEFORE_ENERGY, 3).coerceIn(SelfRating.RATING_RANGE),
            ),
        )
    }

    private fun SharedPreferences.Editor.removeActiveSession(): SharedPreferences.Editor =
        remove(KEY_ACTIVE)
            .remove(KEY_ENDED)
            .remove(KEY_PRESET_TITLE)
            .remove(KEY_BEFORE_CALM)
            .remove(KEY_BEFORE_FOCUS)
            .remove(KEY_BEFORE_ENERGY)

    private fun formatDelta(value: Int): String = when {
        value > 0 -> "+$value"
        else -> value.toString()
    }

    private const val PREFERENCES_NAME = "private_session_journal"
    private const val KEY_ACTIVE = "active"
    private const val KEY_ENDED = "ended"
    private const val KEY_PRESET_TITLE = "preset_title"
    private const val KEY_BEFORE_CALM = "before_calm"
    private const val KEY_BEFORE_FOCUS = "before_focus"
    private const val KEY_BEFORE_ENERGY = "before_energy"
    private const val KEY_RATED_SESSIONS = "rated_sessions"
    private const val KEY_LAST_RESULT = "last_result"
}
