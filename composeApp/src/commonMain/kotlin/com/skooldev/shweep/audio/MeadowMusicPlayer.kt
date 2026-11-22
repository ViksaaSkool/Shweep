package com.skooldev.shweep.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * Looping background music for the counting meadow.
 *
 * [isPlaying] reflects actual playback rather than a requested state, so the
 * play/pause control can stay in sync when the app pauses the music.
 */
interface MeadowMusicPlayer {

    val isPlaying: StateFlow<Boolean>

    /** Loads the loop. Playback starts only after [play] is called. */
    fun prepare(musicBytes: ByteArray)

    /** Starts or resumes the loop. Safe to call while the track is loading. */
    fun play()

    /** Pauses playback and keeps the current position for the next [play]. */
    fun pause()

    fun release()
}

expect fun createMeadowMusicPlayer(): MeadowMusicPlayer
