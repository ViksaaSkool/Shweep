package com.skooldev.shweep.audio

import android.media.AudioAttributes
import android.media.SoundPool
import com.skooldev.shweep.ShweepApplication
import java.io.File

actual fun createSheepBleatPlayer(): SheepBleatPlayer = AndroidSheepBleatPlayer()

/**
 * SoundPool keeps the decoded clip in memory and plays it with low latency.
 *
 * Media usage keeps playback on the normal media stream, so the device volume
 * controls apply and other audio is not interrupted.
 */
private class AndroidSheepBleatPlayer : SheepBleatPlayer {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var soundId = 0
    private var streamId = 0
    private var isLoaded = false
    private var isReleased = false
    private var cachedFile: File? = null

    init {
        soundPool.setOnLoadCompleteListener { _, _, status ->
            synchronized(this) {
                if (!isReleased) {
                    isLoaded = status == 0
                }
            }
        }
    }

    override fun prepare(soundBytes: ByteArray) {
        synchronized(this) {
            if (isReleased || soundId != 0) return

            val file = File(ShweepApplication.instance.cacheDir, CACHE_FILE_NAME)
            val written = runCatching { file.writeBytes(soundBytes) }.isSuccess
            if (!written) return

            cachedFile = file
            soundId = soundPool.load(file.absolutePath, 1)
        }
    }

    override fun play() {
        synchronized(this) {
            if (isReleased || !isLoaded || soundId == 0) return

            if (streamId != 0) {
                soundPool.stop(streamId)
            }
            streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun stop() {
        synchronized(this) {
            if (isReleased || streamId == 0) return
            soundPool.stop(streamId)
            streamId = 0
        }
    }

    override fun release() {
        synchronized(this) {
            if (isReleased) return
            isReleased = true
            isLoaded = false
            streamId = 0
            soundPool.release()
            cachedFile?.delete()
            cachedFile = null
        }
    }

    private companion object {
        const val CACHE_FILE_NAME = "mee_sheep.mp3"
    }
}
