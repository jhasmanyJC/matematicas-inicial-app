package com.example.comnicomatincial.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes

class MusicPlayer(private val context: Context) {
    private var mp: MediaPlayer? = null
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    fun start(@RawRes resId: Int, volume: Float = 0.25f) {
        stop()
        mp = MediaPlayer.create(context, resId).apply {
            setAudioAttributes(attrs)
            isLooping = true
            setVolume(volume, volume)
            start()
        }
    }

    fun pause() { mp?.pause() }

    fun resume() { if (mp != null && mp?.isPlaying == false) mp?.start() }

    fun stop() { mp?.release(); mp = null }
}
