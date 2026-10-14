package com.pingpoint.net

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.pingpoint.R

/** Plays the classic Pong hit when a ping gets a reply. */
class PongSound(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null
    @Volatile
    private var enabled = true

    fun setEnabled(on: Boolean) {
        enabled = on
        if (!on) stop()
    }

    fun play() {
        if (!enabled) return
        try {
            val media = player ?: createPlayer() ?: return
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

    private fun stop() {
        try {
            player?.takeIf { it.isPlaying }?.pause()
        } catch (_: Exception) {
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
