package com.guguabc.kids

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.guguabc.kids.data.ColorActivity
import com.guguabc.kids.data.ColorGroup
import com.guguabc.kids.data.ColorRound
import com.guguabc.kids.data.Strings
import com.guguabc.kids.state.GuguPrefs
import com.guguabc.kids.state.Prefs
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource

class ColorFlowTest {
    @get:Rule(order = 0) val preferences = object : ExternalResource() {
        private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
        private lateinit var saved: GuguPrefs
        override fun before() = runBlocking {
            saved = Prefs.read(context)
            Prefs.write(context, GuguPrefs(learnedWordIds = setOf("cat")))
        }
        override fun after() = runBlocking { Prefs.write(context, saved) }
    }

    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun mount() {
        rule.runOnUiThread {
            rule.activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
        }
        rule.setContent { GuguApp() }
    }

    @Test
    fun exploreBothGroupsAndPlayAllGamesWithExistingWordProgress() {
        listOf("colors", "animals", "fruit", "vehicles", "everyday").forEach {
            rule.onNodeWithTag("category_$it").assertIsDisplayed()
        }
        capture("01-首页")
        rule.tap("category_colors")
        rule.onNodeWithTag("color_activity_learn").assertIsDisplayed()
        capture("02-颜色乐园")
        rule.tap("color_group_secondary")
        rule.tap("color_activity_learn")
        rule.onNodeWithText("Green").assertIsDisplayed()
        capture("03-认识颜色")
        rule.tap("color_card")
        rule.tap("color_hear")
        rule.tap("color_next")
        rule.onNodeWithText("Orange").assertIsDisplayed()
        rule.tap("color_previous")
        rule.onNodeWithText("Green").assertIsDisplayed()
        rule.tap("btn_back")
        rule.tap("color_activity_play")
        rule.onNodeWithTag("color_activity_paint").assertIsDisplayed()
        capture("04-游戏选择")

        val group = ColorGroup.SECONDARY
        listOf(ColorActivity.MATCH, ColorActivity.LISTEN, ColorActivity.PAINT).forEachIndexed { index, activity ->
            rule.tap("color_activity_${activity.name.lowercase()}")
            val round = ColorRound.create(group, activity, index + 1)
            rule.onNodeWithTag("screen_color_game").assertIsDisplayed()
            capture(listOf("05-同色配对", "06-听音找色", "07-颜色小画家")[index])
            if (activity == ColorActivity.MATCH) {
                val question = round.questions.first()
                val wrong = question.options.indexOfFirst { it != question.target }
                rule.tap("color_option_$wrong")
                rule.onNodeWithText(Strings.COLOR_TRY_AGAIN).assertIsDisplayed()
                rule.onNodeWithTag("color_option_${1 - wrong}").assertIsNotEnabled().performTouchInput { click() }
                rule.mainClock.advanceTimeBy(1_700)
                rule.onNodeWithTag("color_option_${1 - wrong}").assertIsEnabled()
            }
            round.questions.forEach { question ->
                val correct = question.options.indexOf(question.target)
                rule.tap("color_option_$correct")
                rule.onNodeWithText(Strings.COLOR_PRAISE).assertIsDisplayed()
                rule.onNodeWithTag("color_option_$correct").assertIsNotEnabled().performTouchInput { click() }
                rule.tap("color_continue")
            }
            rule.onNodeWithText(Strings.COLOR_DONE).assertIsDisplayed()
            if (activity == ColorActivity.PAINT) {
                rule.onNodeWithTag("color_finished_painting").assertIsDisplayed()
                capture("08-上色完成")
            } else {
                rule.tap("color_switch")
            }
        }
        rule.tap("color_replay")
        rule.onNodeWithTag("screen_color_game").assertIsDisplayed()
        rule.tap("btn_back")
        rule.tap("btn_back", sameTagDelayMs = 450)
        rule.tap("btn_sticker_book")
        rule.onNodeWithTag("sticker_cat").assertIsEnabled()
        rule.tap("sticker_category_colors")
        rule.onNodeWithText(Strings.COLOR_RECORD).assertIsDisplayed()
        rule.onNodeWithTag("sticker_color_green").assertIsEnabled()
        rule.onNodeWithTag("sticker_color_orange").assertIsEnabled()
        rule.onNodeWithTag("sticker_color_purple").assertIsEnabled()
        rule.onNodeWithTag("sticker_color_red").assertIsNotEnabled()
        rule.tap("sticker_color_green")
        capture("09-颜色贴纸")
    }

    @Test
    fun parentCanSwitchPromptLanguageAndMutedQuizStillShowsTarget() {
        rule.tap("category_colors")
        rule.tap("btn_parent")
        rule.onNodeWithText(Strings.COLOR_CHINESE).assertIsDisplayed()
        rule.tap("btn_color_help")
        rule.onNodeWithText(Strings.COLOR_ENGLISH).assertIsDisplayed()
        rule.onNodeWithText(Strings.PARENT_PANEL_SOUND).performClick()
        capture("10-家长设置")
        rule.onNodeWithText(Strings.PARENT_PANEL_CLOSE).performClick()
        rule.tap("color_activity_play")
        rule.tap("color_activity_listen")
        rule.onNodeWithTag("color_silent_prompt").assertIsDisplayed()
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("colorShots") != "true") return
        rule.waitForIdle()
        // UiAutomation captures the physical surface, which can lag the Compose test clock.
        Thread.sleep(350)
        val dir = rule.activity.getExternalFilesDir(null) ?: error("Screenshot directory unavailable")
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            ?: error("Could not capture $name")
        try {
            File(dir, "$name.png").outputStream().use {
                assertTrue(shot.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            shot.recycle()
        }
    }
}
