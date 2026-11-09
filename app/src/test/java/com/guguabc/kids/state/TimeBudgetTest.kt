package com.guguabc.kids.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * design_v2.md 附录「2026-11-09 交互修复」①②③⑤ · 5 分钟学习态时长的可执行断言。
 * 上一轮真机 bug 里有 2 个出在这里（贴纸册被踢回 DONE、旧口径 71 分钟），
 * 这些边界以前只有字符串检查，现在逐条跑真实状态机。
 */
class TimeBudgetTest {

    private companion object {
        const val TODAY = "2026-11-09"
        const val TOMORROW = "2026-11-10"
    }

    private fun budgetAt(seconds: Int) = TimeBudget(
        seconds = seconds,
        date = TODAY,
        timeUpHandled = seconds >= TIME_LIMIT_SECONDS,
    )

    // ---- 基础状态 ----
    @Test
    fun initialStateIsClean() {
        val b = budgetAt(0)
        assertEquals(0, b.seconds)
        assertEquals(0, b.minutesUsed)
        assertTrue(b.replayAllowed)
        assertFalse(b.isTimeUp)
    }

    @Test
    fun nonLearningRoutesNeverAccumulateTime() {
        val b = budgetAt(0)
        repeat(600) { assertFalse(b.tick(isLearning = false, today = TODAY).timeUp) }
        assertEquals(0, b.seconds)
        assertEquals(0, b.minutesUsed)
    }

    @Test
    fun learningRoutesAccumulateOneSecondPerTick() {
        val b = budgetAt(0)
        repeat(10) { b.tick(isLearning = true, today = TODAY) }
        assertEquals(10, b.seconds)
    }

    @Test
    fun stickerBookAndHomeStayAreNotKickedToDone() {
        // 模拟「学习 120s → 贴纸册停留 600s → 家长面板 600s → 回学习 1s」
        val b = budgetAt(0)
        repeat(120) { b.tick(true, TODAY) }
        repeat(600) { assertFalse(b.tick(false, TODAY).timeUp) }
        repeat(600) { assertFalse(b.tick(false, TODAY).timeUp) }
        assertEquals(120, b.seconds)
        assertFalse(b.tick(true, TODAY).timeUp)
        assertEquals(121, b.seconds)
    }

    // ---- 上限与单次跳转 ----
    @Test
    fun reachingLimitTriggersTimeUpExactlyOnce() {
        val b = budgetAt(0)
        var ups = 0
        repeat(TIME_LIMIT_SECONDS + 20) { if (b.tick(true, TODAY).timeUp) ups++ }
        assertEquals(1, ups)
        assertEquals(TIME_LIMIT_SECONDS, b.seconds)
        assertFalse(b.replayAllowed)
        assertTrue(b.isTimeUp)
    }

    @Test
    fun afterTimeUpNonLearningTicksStaySilent() {
        val b = budgetAt(0)
        repeat(TIME_LIMIT_SECONDS) { b.tick(true, TODAY) }
        repeat(50) { assertFalse(b.tick(false, TODAY).timeUp) }
        assertEquals(TIME_LIMIT_SECONDS, b.seconds)
    }

    @Test
    fun minutesUsedRoundsUpAtBoundaries() {
        mapOf(0 to 0, 1 to 1, 59 to 1, 60 to 1, 61 to 2, 120 to 2, 299 to 5, 300 to 5)
            .forEach { (seconds, minutes) ->
                assertEquals("seconds=$seconds", minutes, budgetAt(seconds).minutesUsed)
            }
    }

    @Test
    fun replayAllowedFlipsExactlyAtLimit() {
        assertTrue(budgetAt(299).replayAllowed)
        assertFalse(budgetAt(299).isTimeUp)
        assertFalse(budgetAt(300).replayAllowed)
        assertTrue(budgetAt(300).isTimeUp)
    }

    // ---- 跨天 ----
    @Test
    fun crossingMidnightClearsTimeAndAllowsNewLimit() {
        val b = budgetAt(100)
        val r = b.tick(true, TOMORROW)
        assertTrue(r.shouldPersist)
        assertFalse(r.timeUp)
        assertEquals(TOMORROW, b.date)
        assertEquals(1, b.seconds)   // 清零后本次学习态仍计 1 秒
    }

    @Test
    fun crossingMidnightOnIdleRouteOnlyClears() {
        val b = budgetAt(250)
        val r = b.tick(false, TOMORROW)
        assertTrue(r.shouldPersist)
        assertFalse(r.timeUp)
        assertEquals(0, b.seconds)
    }

    // ---- 冷启动恢复 / V1 旧数据迁移 ----
    @Test
    fun restoreClearsLegacyOversizedValue() {
        val b = TimeBudget()
        b.restore(4260, TODAY, TODAY)   // 真机实测 V1 旧口径约 4260s
        assertEquals(0, b.seconds)
        assertTrue(b.replayAllowed)
    }

    @Test
    fun restoreIsIdempotentAndCannotBypassLimit() {
        val b = TimeBudget()
        b.restore(4260, TODAY, TODAY)
        b.restore(0, TODAY, TODAY)
        assertEquals(0, b.seconds)
        // 恢复后仍必须能在 300 秒时正常到点
        var ups = 0
        repeat(TIME_LIMIT_SECONDS) { if (b.tick(true, TODAY).timeUp) ups++ }
        assertEquals(1, ups)
    }

    @Test
    fun restoreKeepsNormalSameDayProgress() {
        val b = TimeBudget()
        b.restore(120, TODAY, TODAY)
        assertEquals(120, b.seconds)
        assertEquals(2, b.minutesUsed)
    }

    @Test
    fun restoreAtLimitDoesNotJumpAgainOnColdStart() {
        val b = TimeBudget()
        b.restore(TIME_LIMIT_SECONDS, TODAY, TODAY)
        assertEquals(TIME_LIMIT_SECONDS, b.seconds)
        repeat(10) { assertFalse(b.tick(true, TODAY).timeUp) }
        assertEquals(TIME_LIMIT_SECONDS, b.seconds)
    }

    @Test
    fun restoreOnNewDayClearsEverything() {
        val b = TimeBudget()
        b.restore(4260, "2026-11-08", TODAY)
        assertEquals(0, b.seconds)
        assertEquals(TODAY, b.date)
        assertTrue(b.replayAllowed)
    }

    @Test
    fun restoreClampsNegativeStoredValue() {
        val b = TimeBudget()
        b.restore(-5, TODAY, TODAY)
        assertEquals(0, b.seconds)
    }

    // ---- 家长重置 ----
    @Test
    fun resetTodayTimeReenablesLearningAndAllowsANewLimit() {
        val b = budgetAt(0)
        repeat(TIME_LIMIT_SECONDS) { b.tick(true, TODAY) }
        assertTrue(b.isTimeUp)
        b.reset(TODAY)
        assertEquals(0, b.seconds)
        assertEquals(TODAY, b.date)
        assertTrue(b.replayAllowed)
        assertFalse(b.isTimeUp)
        var ups = 0
        repeat(TIME_LIMIT_SECONDS) { if (b.tick(true, TODAY).timeUp) ups++ }
        assertEquals(1, ups)
    }

    // ---- 持久化节流 ----
    @Test
    fun persistenceIsThrottledToEveryFiveLearningSeconds() {
        val b = budgetAt(0)
        val persisted = (1..10).map { b.tick(true, TODAY).shouldPersist }
        assertEquals(
            listOf(false, false, false, false, true, false, false, false, false, true),
            persisted,
        )
    }

    @Test
    fun timeUpAlwaysPersistsImmediately() {
        val b = budgetAt(299)
        val r = b.tick(true, TODAY)
        assertTrue(r.timeUp)
        assertTrue(r.shouldPersist)
    }
}
