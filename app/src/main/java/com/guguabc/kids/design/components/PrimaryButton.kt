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
                    if (enabled) GuguColor.primary else GuguColor.primary.copy(alpha = 0.45f),
                    shape,
                )
                .guguClick(tag = tag, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Row(
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
