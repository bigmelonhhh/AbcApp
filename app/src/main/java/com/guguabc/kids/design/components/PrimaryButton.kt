package com.guguabc.kids.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguShadow
import com.guguabc.kids.design.GuguShadowBox
import com.guguabc.kids.design.GuguText
import com.guguabc.kids.design.GuguType
import com.guguabc.kids.design.guguClick

/**
 * design.md §5：全圆角黄底 + shadowPrimary；文字色 #4A3423。
 * 高度由调用方传入，组件内部不写死；宽度可指定或填满父容器。
 *
 * 2026-11-09 交互修复（design_v2.md 附录⑦）：禁用态仍接收点击并分流到
 * onDisabledClick 给一次可见提示，不再用 clickable(enabled=false) 把点击整个吞掉。
 * 禁用态视觉只在交互时出现，V1 五张像素基准图拍的都是启用态，不受影响。
 */
@Composable
fun PrimaryButton(
    text: String,
    height: Dp,
    width: Dp? = null,
    textStyle: TextStyle = GuguType.startButton,
    iconRes: Int? = null,
    iconAtStart: Boolean = true,
    iconSize: Dp = GuguDimens.homePlayIcon,
    gap: Dp = GuguDimens.homeStartGap,
    tag: String? = null,
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(height / 2)
    GuguShadowBox(
        spec = GuguShadow.primary,
        shape = shape,
        modifier = Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) GuguColor.primary
                    else GuguColor.primary.copy(alpha = GuguDimens.disabledAlpha),
                    shape,
                )
                .guguClick(tag = tag) {
                    if (enabled) onClick() else onDisabledClick?.invoke()
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                // 启用态不挂任何 alpha modifier，保证 V1 五张像素基准零渲染差异
                modifier = if (enabled) Modifier
                else Modifier.alpha(GuguDimens.disabledTextAlpha),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (iconRes != null && iconAtStart) ButtonIcon(iconRes, iconSize)
                GuguText(text = text, style = textStyle)
                if (iconRes != null && !iconAtStart) ButtonIcon(iconRes, iconSize)
            }
        }
    }
}

@Composable
private fun ButtonIcon(iconRes: Int, iconSize: Dp) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(iconSize),
        contentScale = ContentScale.Fit,
    )
}
