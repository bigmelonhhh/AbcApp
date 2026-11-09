package com.guguabc.kids.state

import com.guguabc.kids.data.Word

/**
 * design.md §7.1 单一闭环 + design_v2.md §4：宝宝侧不新增课程表 / 关卡地图 / 底部导航。
 * Card / Quiz / Cheer 带 packId，保证跨课不串词。
 */
sealed interface GuguRoute {
    data object Home : GuguRoute
    data class Card(val index: Int, val packId: String) : GuguRoute              // 0..2
    data class Quiz(val index: Int, val packId: String) : GuguRoute              // 0..2
    data class Cheer(val index: Int, val word: Word, val packId: String) : GuguRoute
    data object Done : GuguRoute
    data object StickerBook : GuguRoute                                          // V2 新页
}
