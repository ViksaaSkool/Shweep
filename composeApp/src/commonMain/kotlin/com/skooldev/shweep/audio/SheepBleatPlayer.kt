package com.skooldev.shweep.audio

/**
 * Plays the bundled sheep bleat on the counting screen.
 *
 * [prepare] is called once with the shared MP3 bytes. Implementations may load
 * asynchronously, so [play] must safely do nothing until the sound is ready.
 */
interface SheepBleatPlayer {

    fun prepare(soundBytes: ByteArray)

    /** Plays one bleat, restarting the clip if it is already playing. */
    fun play()

    /** Stops any playing bleat, for example when the app goes to the background. */
    fun stop()

    fun release()
}

expect fun createSheepBleatPlayer(): SheepBleatPlayer
