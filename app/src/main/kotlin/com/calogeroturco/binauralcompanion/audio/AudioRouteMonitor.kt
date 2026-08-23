package com.calogeroturco.binauralcompanion.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioRouteStatus(
    val headphonesReady: Boolean,
    val label: String,
)

class AudioRouteMonitor(context: Context) : AutoCloseable {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mutableStatus = MutableStateFlow(readStatus())
    val status: StateFlow<AudioRouteStatus> = mutableStatus.asStateFlow()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
    }

    init {
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
    }

    private fun refresh() {
        mutableStatus.value = readStatus()
    }

    private fun readStatus(): AudioRouteStatus {
        val privateOutput = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull(::isPrivateStereoOutput)
        return if (privateOutput != null) {
            AudioRouteStatus(
                headphonesReady = true,
                label = labelFor(privateOutput),
            )
        } else {
            AudioRouteStatus(
                headphonesReady = false,
                label = "Connect wired, USB, or Bluetooth headphones",
            )
        }
    }

    override fun close() {
        audioManager.unregisterAudioDeviceCallback(callback)
    }

    companion object {
        fun isPrivateStereoOutput(device: AudioDeviceInfo): Boolean = when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_HEARING_AID,
            -> true
            else -> false
        }

        fun labelFor(device: AudioDeviceInfo?): String {
            if (device == null) return "Current Android audio output"
            val kind = when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                -> "Wired headphones"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headphones"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                -> "Bluetooth audio"
                AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing device"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
                else -> "Android audio output"
            }
            val product = device.productName?.toString()?.trim().orEmpty()
            return if (product.isBlank() || product.equals(kind, ignoreCase = true)) {
                kind
            } else {
                "$kind · $product"
            }
        }
    }
}
