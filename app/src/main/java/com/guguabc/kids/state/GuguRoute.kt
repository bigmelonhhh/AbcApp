package com.guguabc.kids.state

import com.guguabc.kids.data.Word
import com.guguabc.kids.data.LearningCategory

/**
 * V2 分类首页 → 图片选课 → 三词学习闭环；贴纸册可从首页和完成页进入。
 * Card / Quiz / Cheer 带 packId，保证跨课不串词。
 */
sealed interface GuguRoute {
    data object Home : GuguRoute
    data class Lessons(val category: LearningCategory, val page: Int = 0) : GuguRoute
    data class Card(val index: Int, val packId: String) : GuguRoute              // 0..2
    data class Quiz(val index: Int, val packId: String) : GuguRoute              // 0..2
    data class Cheer(val index: Int, val word: Word, val packId: String) : GuguRoute
    data object Done : GuguRoute
    data object StickerBook : GuguRoute                                          // V2 新页
}
