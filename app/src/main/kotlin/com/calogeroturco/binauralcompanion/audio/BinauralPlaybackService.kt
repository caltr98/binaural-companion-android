package com.calogeroturco.binauralcompanion.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.calogeroturco.binauralcompanion.MainActivity
import com.calogeroturco.binauralcompanion.R
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.min

class BinauralPlaybackService : Service() {
    private val sessionToken = AtomicInteger(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var worker: Thread? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                stopSession()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ContextCompat.registerReceiver(
            this,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession(intent.toSessionConfig())
            ACTION_STOP -> stopSession()
            else -> if (worker == null) stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { unregisterReceiver(noisyReceiver) }
        stopEngine(resetStore = true)
        super.onDestroy()
    }

    private fun startSession(config: SessionConfig) {
        stopEngine(resetStore = false)
        startForeground(NOTIFICATION_ID, buildNotification(config))
        PlaybackStore.begin(config)

        val token = sessionToken.incrementAndGet()
        worker = thread(name = "binaural-pcm", priority = Thread.NORM_PRIORITY + 1) {
            runToneLoop(config, token)
        }
    }

    private fun runToneLoop(config: SessionConfig, token: Int) {
        var localTrack: AudioTrack? = null
        try {
            val minimum = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            check(minimum > 0) { "Android could not create a stereo audio buffer." }
            val bufferBytes = maxOf(minimum, 4_096 * BYTES_PER_STEREO_FRAME)
            val buffer = ShortArray(bufferBytes / Short.SIZE_BYTES)
            val generator = StereoToneGenerator(
                sampleRate = SAMPLE_RATE,
                carrierHz = config.carrierHz,
                beatHz = config.startBeatHz,
            )

            localTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            check(localTrack.state == AudioTrack.STATE_INITIALIZED) {
                "Android could not initialize stereo playback."
            }
            audioTrack = localTrack

            // Intentionally do not request audio focus. Android can mix this quiet layer with
            // Spotify or another media app while that app remains the primary audio experience.
            localTrack.play()
            val startedAt = SystemClock.elapsedRealtime()
            val durationMs = config.durationMinutes * 60_000L
            var lastUiSecond = -1

            while (sessionToken.get() == token && !Thread.currentThread().isInterrupted) {
                val elapsedMs = SystemClock.elapsedRealtime() - startedAt
                val remainingMs = durationMs - elapsedMs
                if (remainingMs <= 0L) break

                val progress = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
                val currentBeatHz = config.startBeatHz +
                    (config.endBeatHz - config.startBeatHz) * progress
                generator.updateBeatHz(currentBeatHz)
                val fadeIn = (elapsedMs / 2_000f).coerceIn(0f, 1f)
                val fadeOut = (remainingMs / 3_000f).coerceIn(0f, 1f)
                generator.fill(buffer, level = config.level, envelope = min(fadeIn, fadeOut))
                val written = localTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                check(written >= 0) { "Stereo playback stopped unexpectedly." }

                val remainingSeconds = ceil(remainingMs / 1_000.0).toInt()
                if (remainingSeconds != lastUiSecond) {
                    lastUiSecond = remainingSeconds
                    PlaybackStore.progress(
                        remainingSeconds = remainingSeconds,
                        routeLabel = AudioRouteMonitor.labelFor(localTrack.routedDevice),
                        currentBeatHz = currentBeatHz,
                    )
                }
            }

            if (sessionToken.get() == token) {
                mainHandler.post {
                    if (sessionToken.get() == token) stopSession()
                }
            }
        } catch (failure: Throwable) {
            if (sessionToken.get() == token) {
                val message = failure.message ?: "Stereo playback could not start."
                mainHandler.post {
                    if (sessionToken.get() == token) failSession(message)
                }
            }
        } finally {
            if (audioTrack === localTrack) audioTrack = null
            runCatching { localTrack?.pause() }
            runCatching { localTrack?.flush() }
            runCatching { localTrack?.stop() }
            runCatching { localTrack?.release() }
        }
    }

    private fun stopSession() {
        stopEngine(resetStore = true)
        SessionJournal.markEnded(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun failSession(message: String) {
        stopEngine(resetStore = false)
        SessionJournal.cancelActive(applicationContext)
        PlaybackStore.fail(message)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopEngine(resetStore: Boolean) {
        sessionToken.incrementAndGet()
        val track = audioTrack
        audioTrack = null
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        worker?.interrupt()
        worker = null
        if (resetStore) PlaybackStore.stop()
    }

    private fun buildNotification(config: SessionConfig): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BinauralPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${config.preset.title} layer is playing")
            .setContentText("${formatBeatRange(config)} · ${config.durationMinutes} min")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Stop layer", stopIntent).build())
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun Intent.toSessionConfig(): SessionConfig {
        val preset = BeatPreset.fromId(getStringExtra(EXTRA_PRESET_ID))
        return SessionConfig(
            preset = preset,
            startBeatHz = getFloatExtra(EXTRA_START_BEAT_HZ, preset.startBeatHz).coerceIn(0f, 40f),
            endBeatHz = getFloatExtra(EXTRA_END_BEAT_HZ, preset.endBeatHz).coerceIn(0f, 40f),
            carrierHz = getFloatExtra(EXTRA_CARRIER_HZ, preset.carrierHz).coerceIn(80f, 900f),
            level = getFloatExtra(EXTRA_LEVEL, DEFAULT_LEVEL).coerceIn(0.01f, StereoToneGenerator.MAX_LEVEL),
            durationMinutes = getIntExtra(EXTRA_DURATION_MINUTES, 20).coerceIn(5, 90),
        )
    }

    companion object {
        private const val CHANNEL_ID = "binaural_sessions"
        private const val NOTIFICATION_ID = 4102
        private const val SAMPLE_RATE = 48_000
        private const val BYTES_PER_STEREO_FRAME = 4
        const val DEFAULT_LEVEL = 0.06f

        private const val ACTION_START = "com.calogeroturco.binauralcompanion.START"
        private const val ACTION_STOP = "com.calogeroturco.binauralcompanion.STOP"
        private const val EXTRA_PRESET_ID = "preset_id"
        private const val EXTRA_START_BEAT_HZ = "start_beat_hz"
        private const val EXTRA_END_BEAT_HZ = "end_beat_hz"
        private const val EXTRA_CARRIER_HZ = "carrier_hz"
        private const val EXTRA_LEVEL = "level"
        private const val EXTRA_DURATION_MINUTES = "duration_minutes"

        fun start(context: Context, config: SessionConfig) {
            val intent = Intent(context, BinauralPlaybackService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_PRESET_ID, config.preset.id)
                .putExtra(EXTRA_START_BEAT_HZ, config.startBeatHz)
                .putExtra(EXTRA_END_BEAT_HZ, config.endBeatHz)
                .putExtra(EXTRA_CARRIER_HZ, config.carrierHz)
                .putExtra(EXTRA_LEVEL, config.level)
                .putExtra(EXTRA_DURATION_MINUTES, config.durationMinutes)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, BinauralPlaybackService::class.java).setAction(ACTION_STOP),
            )
        }

        private fun formatHz(value: Float): String =
            if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)

        private fun formatBeatRange(config: SessionConfig): String =
            if (config.startBeatHz == config.endBeatHz) {
                "${formatHz(config.startBeatHz)} Hz difference"
            } else {
                "${formatHz(config.startBeatHz)} → ${formatHz(config.endBeatHz)} Hz glide"
            }
    }
}
