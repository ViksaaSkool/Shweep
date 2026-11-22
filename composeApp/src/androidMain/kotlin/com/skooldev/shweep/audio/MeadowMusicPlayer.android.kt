package com.skooldev.shweep.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.skooldev.shweep.ShweepApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual fun createMeadowMusicPlayer(): MeadowMusicPlayer = AndroidMeadowMusicPlayer()

/**
 * MediaPlayer is used instead of SoundPool because the piano loop is long:
 * SoundPool holds short effects in memory, while MediaPlayer natively loops
 * long tracks and keeps the paused position.
 *
 * No audio focus is requested, so the piano mixes with any audio the player
 * already has running.
 */
private class AndroidMeadowMusicPlayer : MeadowMusicPlayer {

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var cachedFile: File? = null
    private var isPrepared = false
    private var playWhenReady = false
    private var isReleased = false

    override fun prepare(musicBytes: ByteArray) {
        synchronized(this) {
            if (isReleased || mediaPlayer != null || musicBytes.isEmpty()) return

            val file = File(ShweepApplication.instance.cacheDir, CACHE_FILE_NAME)
            val written = runCatching { file.writeBytes(musicBytes) }.isSuccess
            if (!written) return
            cachedFile = file

            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.isLooping = true

            player.setOnPreparedListener { preparedPlayer ->
                synchronized(this) {
                    if (isReleased) return@synchronized
                    isPrepared = true
                    if (playWhenReady) {
                        runCatching { preparedPlayer.start() }
                            .onSuccess { _isPlaying.value = true }
                            .onFailure {
                                playWhenReady = false
                                _isPlaying.value = false
                            }
                    }
                }
            }

            player.setOnErrorListener { _, _, _ ->
                synchronized(this) {
                    if (!isReleased) {
                        isPrepared = false
                        playWhenReady = false
                        _isPlaying.value = false
                    }
                }
                true
            }

            runCatching {
                player.setDataSource(file.absolutePath)
                player.prepareAsync()
            }.onFailure {
                playWhenReady = false
                isPrepared = false
                mediaPlayer = null
                cachedFile = null
                runCatching { player.release() }
                file.delete()
            }
        }
    }

    override fun play() {
        synchronized(this) {
            if (isReleased) return
            playWhenReady = true

            val player = mediaPlayer ?: return
            if (!isPrepared) return

            runCatching { player.start() }
                .onSuccess { _isPlaying.value = true }
                .onFailure {
                    playWhenReady = false
                    _isPlaying.value = false
                }
        }
    }

    override fun pause() {
        synchronized(this) {
            playWhenReady = false
            if (isReleased) return

            val player = mediaPlayer
            if (
                player != null &&
                isPrepared &&
                runCatching { player.isPlaying }.getOrDefault(false)
            ) {
                runCatching { player.pause() }
            }
            _isPlaying.value = false
        }
    }

    override fun release() {
        synchronized(this) {
            if (isReleased) return
            isReleased = true
            isPrepared = false
            playWhenReady = false
            _isPlaying.value = false

            mediaPlayer?.let { player ->
                runCatching {
                    if (player.isPlaying) player.stop()
                }
                runCatching { player.release() }
            }
            mediaPlayer = null
            cachedFile?.delete()
            cachedFile = null
        }
    }

    private companion object {
        const val CACHE_FILE_NAME = "piano_sheep.mp3"
    }
}
