package com.guguabc.kids.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens

/**
 * design.md §5 / §6.2 / §6.3：激活点 20dp #FFC857，未激活 14dp #E8DDC8，
 * 圆心距 35dp，容器 90 × 20。由 SessionState 的 activeIndex 驱动，
 * 不使用 dots_card_1.svg / dots_quiz_2.svg（静态示意）。
 */
@Composable
fun ProgressDots(
    activeIndex: Int,
    count: Int = 3,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(GuguDimens.progressW, GuguDimens.progressH)
            .testTag("progress_dots"),
    ) {
        repeat(count) { i ->
            val active = i == activeIndex
            val dot = if (active) 20.dp else 14.dp
            val center = (10 + 35 * i).dp
            Box(
                Modifier
                    .offset(x = center - dot / 2, y = (20.dp - dot) / 2)
                    .size(dot)
                    .background(
                        if (active) GuguColor.primary else GuguColor.divider,
                        CircleShape,
                    ),
            )
        }
    }
}
