package com.guguabc.kids.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

/**
 * design.md §5：全屏底色容器。
 * 系统栏已隐藏，顶部 36dp 空白由各屏自己保留；各屏内容区 padding ≥ 48dp，
 * 已满足「底部 16dp 不被内容占用」，不额外叠加 padding 以免位移。
 * 所有 ABSOLUTE 元素在本 Box 内以左上角为原点定位。
 */
@Composable
fun GuguScreenContainer(
    background: Color,
    tag: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .testTag(tag),
        content = content,
    )
}
