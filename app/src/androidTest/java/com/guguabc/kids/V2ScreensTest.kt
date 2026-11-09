package com.guguabc.kids

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.guguabc.kids.data.Strings
import com.guguabc.kids.data.Words
import com.guguabc.kids.design.components.ParentGateButton
import com.guguabc.kids.design.components.PrimaryButton
import com.guguabc.kids.screens.StickerBookScreen
import com.guguabc.kids.state.ParentPanelContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** design_v2.md 两页 + 交互修复组件的 UI 契约。 */
class V2ScreensTest {

    @get:Rule val rule = createComposeRule()

    private fun panel(
        currentPackId: String = Words.P1,
        soundEnabled: Boolean = true,
        minutesUsed: Int = 3,
        onSelectPack: (String) -> Unit = {},
        onResetToday: () -> Unit = {},
        onToggleSound: () -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        rule.setGuguScreen {
            ParentPanelContent(
                currentPackId = currentPackId,
                soundEnabled = soundEnabled,
                minutesUsed = minutesUsed,
                onSelectPack = onSelectPack,
                onResetToday = onResetToday,
                onToggleSound = onToggleSound,
                onClose = onClose,
            )
        }
    }

    // ---- 贴纸册 ----
    @Test
    fun stickerBookRendersAll24CellsInOneScreen() {
        rule.setGuguScreen {
            StickerBookScreen(
                words = Words.all, learnedIds = emptySet(), onPlay = {}, onBack = {},
            )
        }
        rule.onNodeWithTag("screen_sticker_book").assertExists()
        rule.onNodeWithTag("sticker_grid").assertExists()
        rule.onNodeWithText(Strings.STICKER_BOOK_TITLE).assertExists()
        rule.onNodeWithText(Strings.STICKER_BOOK_HINT).assertExists()
        Words.all.forEach { rule.onNodeWithTag("sticker_${it.id}").assertExists() }
        rule.onNodeWithTag("btn_back").assertExists()
    }

    @Test
    fun stickerBookPlaysOnlyUnlockedWordsAndCanGoBack() {
        val played = mutableListOf<String>()
        var back = 0
        rule.setGuguScreen {
            StickerBookScreen(
                words = Words.all.take(6), learnedIds = setOf("cat", "dog"),
                onPlay = { played.add(it.id) }, onBack = { back++ },
            )
        }
        rule.tap("sticker_cat")
        rule.tap("sticker_dog")
        assertEquals(listOf("cat", "dog"), played)
        // 未解锁贴纸：直接注入触摸（disabled 节点没有 onClick semantics，不能用 performClick）
        rule.onNodeWithTag("sticker_pig").performTouchInput { click() }
        assertEquals(listOf("cat", "dog"), played)
        rule.tap("btn_back")
        assertEquals(1, back)
    }

    @Test
    fun stickerBookShowsNoLockedCounterOrQuestionMarks() {
        rule.setGuguScreen {
            StickerBookScreen(
                words = Words.all.take(6), learnedIds = emptySet(), onPlay = {}, onBack = {},
            )
        }
        rule.onNodeWithText("?").assertDoesNotExist()
        rule.onNodeWithText("0/24").assertDoesNotExist()
    }

    // ---- 家长面板 ----
    @Test
    fun parentPanelRendersEightPacksAndParentOnlyNotices() {
        panel()
        rule.onNodeWithTag("pack_list").assertExists()
        (1..8).forEach { rule.onNodeWithTag("pack_row_$it").assertExists() }
        rule.onNodeWithText(Strings.PARENT_PANEL_TODAY_PACK).assertExists()
        rule.onNodeWithText(Strings.TTS_NOTICE_LINE).assertExists()
        rule.onNodeWithText(Strings.PARENT_PANEL_TODAY.format(3)).assertExists()
        rule.onNodeWithText(Strings.PACK_LEVEL_L2).assertExists()
        rule.onNodeWithText(Strings.STATE_ON).assertExists()
    }

    @Test
    fun selectingAPackForwardsItsIdAndReflectsSelectedCourse() {
        var selected: String? = null
        panel(currentPackId = Words.P1, onSelectPack = { selected = it })
        rule.tap("pack_row_2")
        assertEquals(Words.P2, selected)
        panel(currentPackId = Words.P8)
        rule.onNodeWithTag("pack_row_8").assertExists()
    }

    @Test
    fun resetTodayTimeShowsDoneHintForTwoSeconds() {
        var reset = 0
        rule.mainClock.autoAdvance = false
        panel(onResetToday = { reset++ })
        rule.onNodeWithTag("btn_reset_today").performScrollTo()
        rule.tap("btn_reset_today")
        assertEquals(1, reset)
        rule.onNodeWithText(Strings.RESET_DONE).assertExists()
        rule.mainClock.advanceTimeBy(2_100)
        rule.waitForIdle()
        rule.onNodeWithText(Strings.RESET_TODAY).assertExists()
    }

    @Test
    fun parentPanelAutoClosesAfterTwentySeconds() {
        var closed = 0
        rule.mainClock.autoAdvance = false
        panel(onClose = { closed++ })
        rule.mainClock.advanceTimeBy(20_100)
        rule.waitForIdle()
        assertEquals(1, closed)
    }

    @Test
    fun soundToggleRowIsReachableAndClickable() {
        var toggled = 0
        panel(soundEnabled = false, onToggleSound = { toggled++ })
        rule.onNodeWithText(Strings.STATE_OFF).assertExists()
        rule.onNodeWithText(Strings.PARENT_PANEL_SOUND).performScrollTo()
        rule.onNodeWithText(Strings.PARENT_PANEL_SOUND).performTouchInput { click() }
        assertEquals(1, toggled)
    }

    // ---- 家长门：短按不打开 / 长按 1.5s 打开 ----
    @Test
    fun shortPressDoesNotOpenParentGate() {
        var opened = 0
        rule.setGuguScreen {
            ParentGateButton(
                size = 72.dp, iconRes = R.drawable.ic_parent, iconSize = 36.dp,
            ) { opened++ }
        }
        rule.onNodeWithTag("btn_parent").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(0, opened)
    }

    @Test
    fun longPressOpensParentGateAfter1500ms() {
        var opened = 0
        rule.mainClock.autoAdvance = false
        rule.setGuguScreen {
            ParentGateButton(
                size = 72.dp, iconRes = R.drawable.ic_parent, iconSize = 36.dp,
            ) { opened++ }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("btn_parent").performTouchInput {
            down(center)
            rule.mainClock.advanceTimeBy(1_600L)
            up()
        }
        rule.waitForIdle()
        assertEquals(1, opened)
    }

    @Test
    fun releasingBefore1500msCancelsParentGate() {
        var opened = 0
        rule.mainClock.autoAdvance = false
        rule.setGuguScreen {
            ParentGateButton(
                size = 72.dp, iconRes = R.drawable.ic_parent, iconSize = 36.dp,
            ) { opened++ }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("btn_parent").performTouchInput {
            down(center)
            rule.mainClock.advanceTimeBy(800L)
            up()
        }
        rule.mainClock.advanceTimeBy(2_000L)
        rule.waitForIdle()
        assertEquals(0, opened)
    }

    // ---- 主按钮启用 / 禁用态分流 ----
    @Test
    fun primaryButtonEnabledRoutesToOnClick() {
        var clicks = 0
        var disabledClicks = 0
        rule.setGuguScreen {
            PrimaryButton(
                text = "开始玩", height = 96.dp, tag = "btn_test", enabled = true,
                onDisabledClick = { disabledClicks++ }, onClick = { clicks++ },
            )
        }
        rule.tap("btn_test")
        assertEquals(1, clicks)
        assertEquals(0, disabledClicks)
    }

    @Test
    fun primaryButtonDisabledRoutesToOnDisabledClick() {
        var clicks = 0
        var disabledClicks = 0
        rule.setGuguScreen {
            PrimaryButton(
                text = "再玩一次", height = 96.dp, tag = "btn_test", enabled = false,
                onDisabledClick = { disabledClicks++ }, onClick = { clicks++ },
            )
        }
        rule.tap("btn_test")
        assertEquals(0, clicks)
        assertEquals(1, disabledClicks)
    }
}
