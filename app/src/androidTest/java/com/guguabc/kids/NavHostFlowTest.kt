package com.guguabc.kids

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 端到端：真实 GuguApp / GuguNavHost / GuguSession / DataStore / MediaPlayer，
 * 在横屏模拟器的 1920×1200 上按 1280×800dp 设计密度运行。
 * 覆盖五屏闭环、无失败态、贴纸册停留豁免、家长选课后回首页可再开始。
 */
@RunWith(AndroidJUnit4::class)
class NavHostFlowTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // 显式接管 content，让测试规则的虚拟时钟能驱动 NavHost 里的 delay/动画
        rule.setContent { GuguApp() }
    }

    /** 首页 → 三张卡 → Quiz(0)。btn_next 连续点同一 tag，需真实等过 400ms 防连点。 */
    private fun goToFirstQuiz() {
        rule.onNodeWithTag("screen_home").assertExists()
        rule.tap("btn_start")
        rule.onNodeWithTag("screen_card").assertExists()
        rule.tap("btn_next")
        rule.tap("btn_next", sameTagDelayMs = 450)
        rule.tap("btn_next", sameTagDelayMs = 450)
        rule.onNodeWithTag("screen_quiz").assertExists()
    }

    /** CHEER 的按钮需 1.2s 延迟才可点；btn_continue 会连点三次，同样要真实等待。 */
    private fun passCheer() {
        rule.onNodeWithTag("screen_cheer").assertExists()
        rule.mainClock.advanceTimeBy(1_300)
        rule.waitForIdle()
        rule.tap("btn_continue", sameTagDelayMs = 450)
    }

    /** 基准课与非基准课的 quiz0 正确项都在 A、quiz1 在 B、quiz2 在 A（index 奇偶轮换）。 */
    private fun goToDone() {
        goToFirstQuiz()
        rule.tap("option_a")
        passCheer()
        rule.tap("option_b")
        passCheer()
        rule.tap("option_a")
        passCheer()
        rule.onNodeWithTag("screen_done").assertExists()
    }

    @Test
    fun fullLessonFlowReachesDone() {
        goToDone()
        rule.onNodeWithTag("sticker_wall").assertExists()
        rule.onNodeWithTag("parent_summary").assertExists()
        rule.onNodeWithTag("btn_replay").assertExists()
    }

    @Test
    fun wrongAnswerOnlyReplaysAndStaysOnQuiz() {
        goToFirstQuiz()
        rule.tap("option_b")            // quiz0 的错误项
        rule.onNodeWithTag("screen_quiz").assertExists()
        rule.onNodeWithTag("screen_cheer").assertDoesNotExist()
        // 再点正确项仍能正常推进 → 无失败态、不锁按钮、不记录错误
        rule.tap("option_a", sameTagDelayMs = 450)
        rule.onNodeWithTag("screen_cheer").assertExists()
    }

    @Test
    fun stickerBookStaysOpenAndReturnsToDone() {
        goToDone()
        rule.tap("btn_sticker_book")
        rule.onNodeWithTag("screen_sticker_book").assertExists()
        // 推进 12 次 1s tick：贴纸册是非学习态，不得被 5 分钟上限踢回 DONE
        rule.mainClock.advanceTimeBy(12_000)
        rule.waitForIdle()
        rule.onNodeWithTag("screen_sticker_book").assertExists()
        rule.onNodeWithTag("screen_done").assertDoesNotExist()
        rule.tap("btn_back")
        rule.onNodeWithTag("screen_done").assertExists()
    }

    @Test
    fun parentGateSelectPackReturnsHomeAndCanStartAgain() {
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("screen_home").assertExists()
        rule.onNodeWithTag("btn_parent").performTouchInput {
            down(center)
            rule.mainClock.advanceTimeBy(1_600L)
            up()
        }
        rule.waitForIdle()
        rule.onNodeWithTag("pack_list").assertExists()
        rule.tap("pack_row_1")
        rule.onNodeWithTag("screen_home").assertExists()
        rule.mainClock.autoAdvance = true
        rule.tap("btn_start")
        rule.onNodeWithTag("screen_card").assertExists()
    }
}
