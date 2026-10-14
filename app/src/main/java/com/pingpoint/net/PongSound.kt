package com.pingpoint.net

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.pingpoint.R

/** Loops the classic Pong hit while a ping, scan, or speed test is running. */
class PongSound(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null
    @Volatile
    private var enabled = true

    fun setEnabled(on: Boolean) {
        enabled = on
        if (!on) stopLoop()
    }

    fun startLoop() {
        if (!enabled) return
        try {
            val media = player ?: createPlayer() ?: return
            media.isLooping = true
            if (!media.isPlaying) {
                media.seekTo(0)
                media.start()
            }
        } catch (_: Exception) {
            releasePlayer()
        }
    }

    fun stopLoop() {
        try {
            player?.let { media ->
                if (media.isPlaying) media.pause()
                media.isLooping = false
                media.seekTo(0)
            }
        } catch (_: Exception) {
            releasePlayer()
        }
    }

    /** Short preview when turning sound back on with nothing running. */
    fun playOnce() {
        if (!enabled) return
        try {
            val media = player ?: createPlayer() ?: return
            media.isLooping = false
            if (media.isPlaying) return
            media.seekTo(0)
            media.start()
        } catch (_: Exception) {
            releasePlayer()
        }
    }

    fun release() {
        releasePlayer()
    }

    private fun createPlayer(): MediaPlayer? {
        return try {
            MediaPlayer.create(appContext, R.raw.pong)?.also { media ->
                media.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                player = media
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun releasePlayer() {
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
