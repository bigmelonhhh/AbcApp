package com.guguabc.kids.design.components

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.guguabc.kids.data.Strings
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguShadow
import com.guguabc.kids.design.GuguShadowBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * design.md §5 / §7.5 + design_v2.md 附录「交互修复决议」⑧：
 * - 长按 ≥1500ms 才打开家长门；**短按不打开面板**（防宝宝误入的约束保持）；
 * - 按住期间画一圈 1.5s 进度环，松手立即取消 → 落实 §7.5「必须有反馈，禁止做成死按钮」；
 * - 短按给一次文字提示，解决「点一下毫无反应」的认知问题；
 * - 计时放在独立 LaunchedEffect，不再用 onPress + withTimeoutOrNull 阻塞手势循环。
 * 进度环 / 提示只在交互态出现，静态像素基准图零影响。
 *
 * 2026-11-09 测试体系补齐：长按计时由「SystemClock 轮询 + 16ms 忙循环」改为
 * 一次性 delay + Compose 动画驱动进度环。行为等价（1.5s 打开 / 松手取消 / 短按提示），
 * 但不再依赖真实系统时钟，Compose UI 测试可用虚拟时钟精确推进并断言；
 * 同时省掉按住期间每 16ms 一次重组，对儿童平板更省电。
 */
@Composable
fun ParentGateButton(
    size: Dp,
    iconRes: Int,
    iconSize: Dp,
    tag: String = "btn_parent",
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    var holding by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = tween(
            durationMillis = GuguDimens.parentGateHoldMs.toInt(),
            easing = LinearEasing,
        ),
        label = "parentGateProgress",
    )

    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        delay(GuguDimens.parentGateHoldMs)
        if (isActive) {
            holding = false
            onOpen()
        }
    }

    GuguShadowBox(
        spec = GuguShadow.parentGate,
        shape = CircleShape,
        modifier = Modifier.size(size),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(GuguColor.surface, CircleShape)
                .testTag(tag)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val pressedAt = SystemClock.uptimeMillis()
                            holding = true
                            tryAwaitRelease()
                            holding = false
                            if (SystemClock.uptimeMillis() - pressedAt < GuguDimens.parentGateHoldMs) {
                                Toast.makeText(
                                    context,
                                    Strings.PARENT_GATE_HINT,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            if (holding) {
                Canvas(Modifier.matchParentSize()) {
                    drawArc(
                        color = GuguColor.primary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = GuguDimens.parentGateRingWidth.toPx()),
                    )
                }
            }
        }
    }
}
