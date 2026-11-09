package com.guguabc.kids.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * design.md §7.4：
 * - 全部 res/raw 本地预置，禁止 TTS / 网络音频 / ExoPlayer
 * - 轻量 MediaPlayer（§7.4 明确允许），切页释放
 * - 申请 AUDIOFOCUS_GAIN_TRANSIENT，退到后台立即停止
 */
class AudioPlayer(private val context: Context) {

    @Volatile
    var enabled: Boolean = true
        private set

    private var current: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) stop()
    }

    fun play(@RawRes resId: Int) {
        if (!enabled) return
        stop()
        audioManager.requestAudioFocus(focusRequest)
        val player = MediaPlayer.create(context, resId) ?: return
        current = player
        player.setOnCompletionListener {
            releaseCurrent()
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
        player.start()
    }

    /** App 退到后台 / 切页时立即停止并释放。 */
    fun stop() {
        current?.runCatching {
            if (isPlaying) stop()
            release()
        }
        current = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun releaseCurrent() {
        current?.runCatching { release() }
        current = null
    }

    fun release() = stop()
}
