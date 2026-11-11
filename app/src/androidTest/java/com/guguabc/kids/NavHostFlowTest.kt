package com.guguabc.kids

import android.view.ViewGroup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guguabc.kids.state.GuguPrefs
import com.guguabc.kids.state.Prefs
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.ExternalResource

/**
 * 端到端：真实 GuguApp / GuguNavHost / GuguSession / DataStore / MediaPlayer，
 * 在横屏模拟器的 1920×1200 上按 1280×800dp 设计密度运行。
 * 覆盖五屏闭环、无失败态、贴纸册停留豁免、家长选课后回首页可再开始。
 */
@RunWith(AndroidJUnit4::class)
class NavHostFlowTest {

    // 在 Activity 启动前隔离每日额度，结束后恢复，避免测试顺序与历史数据影响流程。
    @get:Rule(order = 0) val preferences = object : ExternalResource() {
        private lateinit var saved: GuguPrefs
        private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
        override fun before() = runBlocking {
            saved = Prefs.read(context)
            Prefs.write(context, GuguPrefs())
        }
        override fun after() = runBlocking { Prefs.write(context, saved) }
    }

    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // 显式接管 content，让测试规则的虚拟时钟能驱动 NavHost 里的 delay/动画
        // 先卸载 Activity 自建的 Composition，再由测试规则提供可控时钟的 Recomposer。
        rule.runOnUiThread {
            rule.activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
        }
        rule.setContent { GuguApp() }
    }

    /** 首页 → 三张卡 → Quiz(0)。btn_next 连续点同一 tag，需真实等过 400ms 防连点。 */
    private fun goToFirstQuiz() {
        rule.onNodeWithTag("screen_home").assertExists()
        rule.tap("category_animals")
        rule.tap("lesson_p1_friends")
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
    fun newCategoriesCanCompleteLessonsAndReturnToChooser() {
        val categories = com.guguabc.kids.data.LearningCategory.entries.drop(1)
        categories.forEach { category ->
            rule.tap("category_${category.id}")
            val pack = com.guguabc.kids.data.Words.inCategory(category).first()
            rule.tap("lesson_${pack.id}")
            rule.onNodeWithText(pack.words.first().en).assertExists()
            repeat(3) { rule.tap("btn_next", sameTagDelayMs = 450) }
            listOf("option_a", "option_b", "option_a").forEach {
                rule.tap(it)
                passCheer()
            }
            rule.tap("btn_sticker_book")
            rule.onNodeWithTag("sticker_${pack.words.first().id}").assertExists()
            rule.tap("btn_back")
            rule.tap("btn_choose_lesson")
            rule.onNodeWithTag("screen_lessons").assertExists()
            rule.tap("btn_back", sameTagDelayMs = 450)
            rule.onNodeWithTag("screen_home").assertExists()
        }
    }

    @Test
    fun animalPaginationExposesAllEightLessons() {
        rule.tap("category_animals")
        rule.onNodeWithTag("lesson_p1_friends").assertExists()
        rule.onNodeWithTag("lesson_p8_special").assertDoesNotExist()
        rule.tap("btn_lesson_page")
        rule.onNodeWithTag("lesson_p8_special").assertExists()
        rule.tap("lesson_p8_special")
        rule.onNodeWithText("Owl").assertExists()
        rule.tap("btn_back")
        rule.onNodeWithTag("lesson_p8_special").assertExists()
        rule.tap("btn_lesson_page", sameTagDelayMs = 450)
        rule.onNodeWithTag("lesson_p1_friends").assertExists()
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
    fun tappingParentOpensSettingsFromHomeLessonsAndDone() {
        fun openAndCloseSettings() {
            rule.onNodeWithTag("btn_parent").performTouchInput { click() }
            rule.waitForIdle()
            rule.onNodeWithText(com.guguabc.kids.data.Strings.PARENT_PANEL_TITLE).assertExists()
            rule.onNodeWithTag("pack_list", useUnmergedTree = true).assertDoesNotExist()
            rule.onNodeWithText(com.guguabc.kids.data.Strings.PARENT_PANEL_CLOSE).performClick()
            rule.waitForIdle()
        }
        rule.onNodeWithTag("screen_home").assertExists()
        openAndCloseSettings()
        rule.tap("category_animals")
        openAndCloseSettings()
        rule.tap("btn_back")
        goToDone()
        openAndCloseSettings()
        rule.onNodeWithTag("screen_done").assertExists()
    }
}
