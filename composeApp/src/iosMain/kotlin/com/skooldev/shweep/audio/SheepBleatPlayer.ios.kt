package com.skooldev.shweep.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

actual fun createSheepBleatPlayer(): SheepBleatPlayer = IosSheepBleatPlayer()

/**
 * The ambient category respects the silent switch and mixes with other audio,
 * matching the app's quiet, bedtime use.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosSheepBleatPlayer : SheepBleatPlayer {

    private var player: AVAudioPlayer? = null
    private var isReleased = false

    override fun prepare(soundBytes: ByteArray) {
        if (isReleased || player != null || soundBytes.isEmpty()) return

        val data = soundBytes.usePinned { pinned ->
            NSData.dataWithBytes(
                bytes = pinned.addressOf(0),
                length = soundBytes.size.toULong()
            )
        }
        player = AVAudioPlayer(data = data, error = null).apply {
            prepareToPlay()
        }

        AVAudioSession.sharedInstance().setCategory(
            AVAudioSessionCategoryAmbient,
            error = null
        )
    }

    override fun play() {
        if (isReleased) return
        val currentPlayer = player ?: return

        AVAudioSession.sharedInstance().setActive(true, error = null)
        currentPlayer.stop()
        currentPlayer.currentTime = 0.0
        currentPlayer.play()
    }

    override fun stop() {
        player?.stop()
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        player?.stop()
        player = null
    }
}
