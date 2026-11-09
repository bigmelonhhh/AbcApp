package com.guguabc.kids.state

import com.guguabc.kids.data.Word

/** design.md §7.1：唯一流程，禁止加页面。 */
sealed interface GuguRoute {
    data object Home : GuguRoute
    data class Card(val index: Int) : GuguRoute          // 0..2
    data class Quiz(val index: Int) : GuguRoute          // 0..2
    data class Cheer(val index: Int, val word: Word) : GuguRoute
    data object Done : GuguRoute
}
