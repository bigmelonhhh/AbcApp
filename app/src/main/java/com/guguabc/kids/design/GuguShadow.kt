package com.guguabc.kids.design

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * design.md §4.3 / §5：
 * 用一层独立的柔和形状做阴影（颜色 / 透明度 / dy 精确取自 GuguShadow），
 * 不用 Modifier.shadow(elevation)（黑色材质阴影），也不用 Material elevation。
 *
 * 实现说明：BlurMaskFilter 在硬件加速 Canvas 上会被忽略，因此改用 Compose 官方
 * Modifier.blur（API 31+ 走 RenderEffect，目标机 Android 14 支持；低于 31 时退化为
 * 无模糊的纯色阴影，不影响布局）。Figma blur → Compose radius 按 §4.3 的 ÷2 经验换算。
 * 真机连接后仍需按 §13 ③ 做 ±2dp 校准。
 */
@Composable
fun GuguShadowBox(
    spec: GuguShadowSpec,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = spec.dy)
                .blur(
                    radius = (spec.blur.value / 2f).dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .background(spec.color.copy(alpha = spec.alpha), shape)
        )
        content()
    }
}

/**
 * design.md §5 / §7.4：所有可点元素的统一行为。
 * - 无涟漪（indication = null）
 * - 按压反馈 scale 0.97 / 100ms 回弹，不变色、不抖
 * - 400ms 防连点
 * - testTag 统一挂在这里
 */
@Composable
fun Modifier.guguClick(
    tag: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var lastClickAt by remember { mutableStateOf(0L) }
    val interaction = remember { MutableInteractionSource() }

    return this
        .graphicsLayer {
            val s = if (pressed) 0.97f else 1f
            scaleX = s
            scaleY = s
        }
        .clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            onClick = {
                val now = SystemClock.uptimeMillis()
                if (now - lastClickAt < 400L) return@clickable
                lastClickAt = now
                pressed = true
                scope.launch {
                    delay(100L)
                    pressed = false
                }
                onClick()
            },
        )
        .then(if (tag != null) Modifier.testTag(tag) else Modifier)
}
