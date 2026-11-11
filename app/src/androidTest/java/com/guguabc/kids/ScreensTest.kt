package com.guguabc.kids

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.guguabc.kids.data.Strings
import com.guguabc.kids.data.Words
import com.guguabc.kids.screens.CheerScreen
import com.guguabc.kids.screens.DoneScreen
import com.guguabc.kids.screens.HomeScreen
import com.guguabc.kids.screens.ListenQuizScreen
import com.guguabc.kids.screens.WordCardScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** design.md §6 五屏的组件级交互契约：回调、文案、tag、禁用态。 */
class ScreensTest {

    @get:Rule val rule = createComposeRule()

    // ---- ① 首页 ----
    @Test
    fun homeShowsFourCategoriesAndForwardsSelection() {
        val selected = mutableListOf<com.guguabc.kids.data.LearningCategory>()
        rule.setGuguScreen { HomeScreen(onCategory = { selected.add(it) }, onStickerBook = {}, onParentGate = {}) }
        rule.onNodeWithText(Strings.EXPLORE_TITLE).assertExists()
        com.guguabc.kids.data.LearningCategory.entries.forEach {
            rule.onNodeWithText(it.title).assertExists()
            rule.tap("category_${it.id}")
        }
        assertEquals(com.guguabc.kids.data.LearningCategory.entries.toList(), selected)
    }

    @Test
    fun homeStickerBookEntryIsDirectlyAvailable() {
        var opened = 0
        rule.setGuguScreen { HomeScreen(onCategory = {}, onStickerBook = { opened++ }, onParentGate = {}) }
        rule.tap("btn_sticker_book")
        assertEquals(1, opened)
    }

    // ---- ② 认一认 ----
    @Test
    fun wordCardShowsWordAndEveryButtonFiresItsCallback() {
        val word = Words.all[0]
        var sound = 0
        var next = 0
        var back = 0
        var toggle = 0
        rule.setGuguScreen {
            WordCardScreen(
                word = word, cardIndex = 0, soundEnabled = true,
                onSound = { sound++ }, onNext = { next++ },
                onBack = { back++ }, onToggleSound = { toggle++ },
            )
        }
        rule.onNodeWithTag("screen_card").assertExists()
        rule.onNodeWithText(word.en).assertExists()
        rule.onNodeWithText(word.zh).assertExists()
        rule.onNodeWithTag("word_en").assertExists()
        rule.onNodeWithTag("word_zh").assertExists()
        rule.onNodeWithTag("progress_dots").assertExists()
        rule.tap("btn_sound")
        rule.tap("btn_next")
        rule.tap("btn_back")
        rule.tap("btn_volume")
        assertEquals(1, sound)
        assertEquals(1, next)
        assertEquals(1, back)
        assertEquals(1, toggle)
    }

    @Test
    fun wordCardSoundHintRowAlsoTriggersPlayback() {
        // 「点一下，听发音」这行字本身是点击指引，整行可点；回归 2026-11-09 修复。
        val word = Words.all[0]
        var sound = 0
        rule.setGuguScreen {
            WordCardScreen(
                word = word, cardIndex = 0, soundEnabled = true,
                onSound = { sound++ }, onNext = {}, onBack = {}, onToggleSound = {},
            )
        }
        rule.onNodeWithText(Strings.SOUND_HINT).assertExists()
        rule.tap("btn_sound_hint")
        assertEquals(1, sound)
    }

    @Test
    fun wordCardRendersSecondWordAndSoundOffState() {
        val word = Words.all[1]
        rule.setGuguScreen {
            WordCardScreen(
                word = word, cardIndex = 1, soundEnabled = false,
                onSound = {}, onNext = {}, onBack = {}, onToggleSound = {},
            )
        }
        rule.onNodeWithText(word.en).assertExists()
        rule.onNodeWithText(word.zh).assertExists()
        rule.onNodeWithTag("btn_volume").assertExists()
    }

    // ---- ③ 听一听 ----
    @Test
    fun quizRendersTwoTextlessOptionsAndForwardsPicks() {
        val pack = Words.of(Words.P1)
        val target = pack.words[0]
        val other = pack.words[1]
        var picked: String? = null
        var sound = 0
        var back = 0
        var toggle = 0
        rule.setGuguScreen {
            ListenQuizScreen(
                targetWord = target, optionA = target, optionB = other,
                quizIndex = 0, soundEnabled = true,
                onSound = { sound++ }, onPick = { picked = it.id },
                onBack = { back++ }, onToggleSound = { toggle++ },
            )
        }
        rule.onNodeWithTag("screen_quiz").assertExists()
        rule.onNodeWithTag("option_a").assertExists()
        rule.onNodeWithTag("option_b").assertExists()
        rule.onNodeWithTag("progress_dots").assertExists()
        rule.onNodeWithText(Strings.QUIZ_TITLE.format(target.en)).assertExists()
        rule.onNodeWithText(Strings.QUIZ_FOOTER).assertExists()
        rule.tap("option_a")
        assertEquals(target.id, picked)
        rule.tap("option_b")
        assertEquals(other.id, picked)
        rule.tap("btn_sound")
        rule.tap("btn_back")
        rule.tap("btn_volume")
        assertEquals(1, sound)
        assertEquals(1, back)
        assertEquals(1, toggle)
    }

    @Test
    fun quizOptionNodesCarryNoWordText() {
        val pack = Words.of(Words.P1)
        rule.setGuguScreen {
            ListenQuizScreen(
                targetWord = pack.words[0], optionA = pack.words[0], optionB = pack.words[1],
                quizIndex = 2, soundEnabled = false,
                onSound = {}, onPick = {}, onBack = {}, onToggleSound = {},
            )
        }
        // 选项卡内不应出现英文/中文词，只有插画；标题里的 "Cat" 属于提示条，不算选项文字
        rule.onNodeWithTag("option_a").assertExists()
        rule.onNodeWithTag("option_b").assertExists()
        rule.onNodeWithText(Strings.QUIZ_TITLE.format("Cat")).assertExists()
    }

    // ---- ④ 答对啦 ----
    @Test
    fun cheerContinueIsBlockedBeforeDelay() {
        var continued = 0
        rule.setGuguScreen {
            CheerScreen(
                word = Words.all[0], quizNumber = 1, canContinue = false,
                onContinue = { continued++ }, onBack = {},
            )
        }
        rule.onNodeWithTag("screen_cheer").assertExists()
        rule.onNodeWithText(Strings.CHEER_TITLE).assertExists()
        rule.onNodeWithText(Strings.CHEER_SUBTITLE.format("Cat")).assertExists()
        rule.onNodeWithText(Strings.CHEER_PROGRESS.format(1)).assertExists()
        rule.tap("btn_continue")
        assertEquals(0, continued)
    }

    @Test
    fun cheerContinueWorksAfterDelay() {
        var continued = 0
        var back = 0
        rule.setGuguScreen {
            CheerScreen(
                word = Words.all[0], quizNumber = 3, canContinue = true,
                onContinue = { continued++ }, onBack = { back++ },
            )
        }
        rule.tap("btn_continue")
        rule.tap("btn_back")
        assertEquals(1, continued)
        assertEquals(1, back)
    }

    // ---- ⑤ 今天真棒 ----
    @Test
    fun doneEnabledReplayAndSecondaryActionsWork() {
        var replay = 0
        var rest = 0
        var book = 0
        rule.setGuguScreen {
            DoneScreen(
                learnedWords = Words.of(Words.P1).words, minutesUsed = 2,
                replayEnabled = true,
                onReplay = { replay++ }, onRest = { rest++ },
                onParentGate = {}, onStickerBook = { book++ },
            )
        }
        rule.onNodeWithTag("screen_done").assertExists()
        rule.onNodeWithTag("sticker_wall").assertExists()
        rule.onNodeWithTag("parent_summary").assertExists()
        rule.onNodeWithText(Strings.DONE_TITLE).assertExists()
        rule.onNodeWithText(Strings.STICKER_BOOK_ENTRY).assertExists()
        rule.onNodeWithTag("btn_parent").assertExists()
        rule.tap("btn_replay")
        rule.tap("btn_rest")
        rule.tap("btn_sticker_book")
        assertEquals(1, replay)
        assertEquals(1, rest)
        assertEquals(1, book)
    }

    @Test
    fun doneDisabledReplayShowsTwoSecondHintThenRestores() {
        rule.mainClock.autoAdvance = false
        rule.setGuguScreen {
            DoneScreen(
                learnedWords = Words.of(Words.P1).words, minutesUsed = 5,
                replayEnabled = false,
                onReplay = {}, onRest = {}, onParentGate = {}, onStickerBook = {},
            )
        }
        rule.tap("btn_replay")
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.onNodeWithText(Strings.TIME_UP_HINT).assertExists()
        rule.onNodeWithText(Strings.PARENT_CARD_HINT).assertDoesNotExist()
        rule.mainClock.advanceTimeBy(2_100)
        rule.waitForIdle()
        rule.onNodeWithText(Strings.TIME_UP_HINT).assertDoesNotExist()
        rule.onNodeWithText(Strings.PARENT_CARD_HINT).assertExists()
    }
}
