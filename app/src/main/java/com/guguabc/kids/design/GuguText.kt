package com.guguabc.kids.design

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * 用 foundation 的 BasicText，不引入 Material 组件默认外观（design.md §2 / §10）。
 * 所有字号、字重、字族、颜色均来自 GuguType，不接受调用方传裸数值。
 */
@Composable
fun GuguText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (textAlign != null) style.copy(textAlign = textAlign) else style,
    )
}
