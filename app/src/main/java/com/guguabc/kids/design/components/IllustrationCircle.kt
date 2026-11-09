package com.guguabc.kids.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.guguabc.kids.design.guguClick

/**
 * design.md §5：圆形底盘 + 透明底插画。
 * 底盘与插画尺寸分别由调用方按 §6 传入，禁止 ContentScale.Crop。
 * ② 点插画底盘等同于点喇叭，故允许挂 onClick。
 */
@Composable
fun IllustrationCircle(
    circleSize: Dp,
    imageSize: Dp,
    imageRes: Int,
    background: Color,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(circleSize)
            .background(background, CircleShape)
            .then(
                if (onClick != null) Modifier.guguClick(tag = tag, onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(imageSize),
            contentScale = ContentScale.Fit,
        )
    }
}
