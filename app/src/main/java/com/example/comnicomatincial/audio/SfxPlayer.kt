package com.example.comnicomatincial.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes

class SfxPlayer(private val context: Context) {
    private var mp: MediaPlayer? = null

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun play(@RawRes resId: Int, loop: Boolean = false, volume: Float = 1f) {
        stop()
        mp = MediaPlayer.create(context, resId).apply {
            setAudioAttributes(attrs)
            isLooping = loop
            setVolume(volume, volume)
            start()
        }
    }

    fun stop() {
        mp?.release()
        mp = null
    }
}
