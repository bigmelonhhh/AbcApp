package com.guguabc.kids.audio

import androidx.annotation.RawRes

/** 设备音频输出边界；生命周期测试可记录播放请求，无需依赖扬声器或音量。 */
interface AudioOutput {
    fun setEnabled(value: Boolean)
    fun play(@RawRes resId: Int)
    fun stop()
    fun release()
}
