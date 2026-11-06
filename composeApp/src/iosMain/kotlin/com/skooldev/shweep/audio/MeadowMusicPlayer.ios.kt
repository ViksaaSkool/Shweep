package com.skooldev.shweep.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

actual fun createMeadowMusicPlayer(): MeadowMusicPlayer = IosMeadowMusicPlayer()

/**
 * AVFoundation loops the track natively and retains the paused position.
 *
 * The ambient category keeps the piano quiet when the silent switch is on and
 * mixes it with any other audio the player has running.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosMeadowMusicPlayer : MeadowMusicPlayer {

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var player: AVAudioPlayer? = null
    private var playWhenReady = false
    private var isReleased = false

    override fun prepare(musicBytes: ByteArray) {
        if (isReleased || player != null || musicBytes.isEmpty()) return

        val data = musicBytes.usePinned { pinned ->
            NSData.dataWithBytes(
                bytes = pinned.addressOf(0),
                length = musicBytes.size.toULong()
            )
        }
        player = AVAudioPlayer(data = data, error = null).apply {
            numberOfLoops = -1L
            prepareToPlay()
        }

        AVAudioSession.sharedInstance().setCategory(
            AVAudioSessionCategoryAmbient,
            error = null
        )

        if (playWhenReady) {
            startPlayback()
        }
    }

    override fun play() {
        if (isReleased) return
        playWhenReady = true
        if (player == null) return
        startPlayback()
    }

    override fun pause() {
        playWhenReady = false
        if (isReleased) return
        player?.pause()
        _isPlaying.value = false
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        playWhenReady = false
        player?.stop()
        player = null
        _isPlaying.value = false
    }

    private fun startPlayback() {
        val currentPlayer = player ?: return
        AVAudioSession.sharedInstance().setActive(true, error = null)
        playWhenReady = currentPlayer.play()
        _isPlaying.value = playWhenReady
    }
}
