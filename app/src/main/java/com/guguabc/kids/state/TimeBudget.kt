package com.guguabc.kids.state

import kotlin.math.ceil

/** tick() 的结果：本次是否触发「到点跳 DONE」、是否需要写一次 DataStore。 */
data class TickResult(val timeUp: Boolean, val shouldPersist: Boolean)

/**
 * design_v2.md 附录「2026-11-09 交互修复」①②③⑤ · 5 分钟时长口径。
 *
 * 纯状态机，零 Android 依赖：只接收「当前是否学习态」与「今天日期」，
 * 由 GuguSession 负责把结果同步到 Compose state / 路由 / DataStore。
 * 从 GuguSession 抽出的原因与 QuizPicker 相同 —— 上一轮 3 个真机 bug 里
 * 有 2 个出在时长与上限跳转，这类边界必须有可执行的断言而不是字符串检查。
 */
class TimeBudget(
    seconds: Int = 0,
    date: String = "",
    timeUpHandled: Boolean = false,
) {
    var seconds: Int = seconds.coerceAtLeast(0)
        private set
    var date: String = date
        private set
    private var timeUpHandled = timeUpHandled
    private var persistTick = 0

    val minutesUsed: Int get() = ceil(seconds / 60f).toInt()
    val replayAllowed: Boolean get() = seconds < TIME_LIMIT_SECONDS
    val isTimeUp: Boolean get() = seconds >= TIME_LIMIT_SECONDS

    /**
     * 冷启动恢复：跨天清零；同日若读到 V1 旧口径的超额值（> 300，页面停留也算过），
     * 一次性归零。新口径下该值恒 ≤ 300，故归零幂等、只在升级后首次启动生效，
     * 不会让冷启动变成绕过上限的后门。
     */
    fun restore(storedSeconds: Int, storedDate: String, today: String) {
        if (storedDate != today) {
            date = today
            seconds = 0
            timeUpHandled = false
        } else {
            date = storedDate
            seconds = if (storedSeconds > TIME_LIMIT_SECONDS) 0
            else storedSeconds.coerceAtLeast(0)
            timeUpHandled = seconds >= TIME_LIMIT_SECONDS
        }
        persistTick = 0
    }

    /**
     * 只在「学习态」累计：isLearning 由调用方按 route 判定（Card / Quiz / Cheer）。
     * 首页 / DONE / 贴纸册 / 家长面板既不累加，也不会被上限踢走。
     * 到 300s 只返回一次 timeUp=true，之后不再累加。
     */
    fun tick(isLearning: Boolean, today: String): TickResult {
        var persist = false
        if (today != date) {
            date = today
            seconds = 0
            timeUpHandled = false
            persistTick = 0
            persist = true
        }
        if (!isLearning) return TickResult(timeUp = false, shouldPersist = persist)
        if (seconds >= TIME_LIMIT_SECONDS) return TickResult(timeUp = false, shouldPersist = persist)
        seconds++
        persistTick++
        if (persistTick >= 5) {
            persistTick = 0
            persist = true
        }
        if (seconds >= TIME_LIMIT_SECONDS && !timeUpHandled) {
            timeUpHandled = true
            return TickResult(timeUp = true, shouldPersist = true)
        }
        return TickResult(timeUp = false, shouldPersist = persist)
    }

    /** 家长面板「重置今日时长」：只清时长与上限标志，不动学习进度 / 课程 / 音效。 */
    fun reset(today: String) {
        seconds = 0
        persistTick = 0
        timeUpHandled = false
        date = today
    }
}
