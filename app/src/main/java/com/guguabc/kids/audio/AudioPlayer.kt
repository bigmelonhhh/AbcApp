package com.guguabc.kids.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes

/**
 * design.md §7.4：全部 res/raw 本地预置，禁止 TTS / 网络音频 / ExoPlayer；
 * 轻量 MediaPlayer，切页释放，申请 AUDIOFOCUS_GAIN_TRANSIENT，退到后台立即停止。
 *
 * 2026-11-09 修复「点一下，听发音」点击后不响 / 只响半句：
 * 1. 原 MediaPlayer.create() 同步 prepare 且失败时返回 null，被 `?: return` 静默吞掉，
 *    既没有声音也没有任何日志，无法区分「没触发」「资源加载失败」「焦点失败」。
 *    改为显式创建 + prepareAsync + OnError / 全链路 GuguAudio 日志。
 * 2. 原 AudioAttributes 为 USAGE_MEDIA + CONTENT_TYPE_SONIFICATION（提示音策略，
 *    会受勿扰 / 通知音量影响）；单词发音是语音，更正为 CONTENT_TYPE_SPEECH，
 *    并让 MediaPlayer 与 AudioFocusRequest 使用同一组属性。
 * 3. 每次播放都新建 / 同步 release，短发音叠加自动播报与连点时 AudioTrack 频繁
 *    开关（设备日志出现 AudioFlinger write blocked / mediaplayer went away）。
 *    用 generation 令牌隔离在途回调，旧回调不会再 release 掉正在播放的新 player。
 */
class AudioPlayer(private val context: Context) : AudioOutput {

    @Volatile
    var enabled: Boolean = true
        private set

    private var current: MediaPlayer? = null

    /** 每次 play / stop 自增；回调只对「自己那一代」生效。仅主线程访问。 */
    private var generation = 0

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .build()

    override fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) stop()
    }

    override fun play(@RawRes resId: Int) {
        if (!enabled) {
            Log.d(TAG, "play 跳过：音效开关关闭")
            return
        }
        stop()

        val token = ++generation
        val focus = audioManager.requestAudioFocus(focusRequest)
        // 焦点失败不直接 return：3 岁宝宝点一下必须有反馈，系统多数时候仍允许出声。
        if (focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "音频焦点未立即授予 resId=$resId result=$focus，仍尝试播放")
        }

        val player = MediaPlayer()
        try {
            val afd: AssetFileDescriptor = context.resources.openRawResourceFd(resId)
                ?: throw IllegalStateException("openRawResourceFd 返回 null（资源被压缩？）")
            afd.use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            player.setAudioAttributes(attributes)
            player.setOnPreparedListener { mp ->
                if (token != generation) {
                    mp.runCatching { release() }
                    return@setOnPreparedListener
                }
                mp.runCatching { start() }.onFailure { Log.e(TAG, "start 失败 resId=$resId", it) }
            }
            player.setOnCompletionListener { mp -> finish(token, mp, resId, "complete") }
            player.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "播放错误 resId=$resId what=$what extra=$extra")
                finish(token, mp, resId, "error")
                true
            }
            current = player
            player.prepareAsync()
            Log.d(TAG, "play resId=$resId token=$token focus=$focus")
        } catch (t: Throwable) {
            Log.e(TAG, "播放初始化失败 resId=$resId", t)
            player.runCatching { release() }
            if (token == generation) {
                current = null
                abandonFocus()
            }
        }
    }

    /** 只释放当前这一代的 player 与焦点；过期回调仅丢弃自己，绝不误伤新播放。 */
    private fun finish(token: Int, mp: MediaPlayer, @RawRes resId: Int, reason: String) {
        if (token != generation) {
            mp.runCatching { release() }
            return
        }
        Log.d(TAG, "播放结束 resId=$resId reason=$reason")
        val old = current
        current = null
        (old ?: mp).runCatching { release() }
        abandonFocus()
    }

    /** App 退到后台 / 切页 / 重播前：立即停止并释放，并让所有在途回调失效。 */
    override fun stop() {
        generation++
        val old = current
        current = null                 // 先摘引用，避免回调拿到半释放状态
        old?.runCatching {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            if (isPlaying) stop()
            release()
        }
        abandonFocus()
    }

    private fun abandonFocus() {
        runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
    }

    override fun release() = stop()

    private companion object {
        const val TAG = "GuguAudio"
    }
}
