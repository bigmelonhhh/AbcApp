package com.guguabc.kids.state

import com.guguabc.kids.data.ColorActivity
import com.guguabc.kids.data.ColorGroup
import com.guguabc.kids.data.ColorRound
import com.guguabc.kids.data.Word
import com.guguabc.kids.data.LearningCategory

/**
 * V2 分类首页 → 图片选课 → 三词学习闭环；贴纸册可从首页和完成页进入。
 * Card / Quiz / Cheer 带 packId，保证跨课不串词。
 */
sealed interface GuguRoute {
    data object Home : GuguRoute
    data class ColorMenu(val group: ColorGroup, val choosingGame: Boolean = false) : GuguRoute
    data class ColorLearn(val group: ColorGroup, val index: Int, val exampleOffset: Int) : GuguRoute
    data class ColorGame(
        val round: ColorRound,
        val index: Int = 0,
        val feedback: ColorFeedback = ColorFeedback.NONE,
    ) : GuguRoute {
        val question get() = round.questions[index]
    }
    data class ColorDone(val group: ColorGroup, val activity: ColorActivity, val painting: ColorRound? = null) : GuguRoute
    data class ColorTimeUp(val group: ColorGroup, val activity: ColorActivity) : GuguRoute
    data class Lessons(val category: LearningCategory, val page: Int = 0) : GuguRoute
    data class Card(val index: Int, val packId: String) : GuguRoute              // 0..2
    data class Quiz(val index: Int, val packId: String) : GuguRoute              // 0..2
    data class Cheer(val index: Int, val word: Word, val packId: String) : GuguRoute
    data object Done : GuguRoute
    data object StickerBook : GuguRoute
}

enum class ColorFeedback { NONE, DEMO, CORRECT }
