package com.guguabc.kids.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguShadow
import com.guguabc.kids.design.GuguShadowBox
import kotlinx.coroutines.withTimeoutOrNull

/** design.md §5 / §7.5：长按 ≥1500ms 才响应，短按无任何反应（防宝宝误入）。 */
@Composable
fun ParentGateButton(
    size: Dp,
    iconRes: Int,
    iconSize: Dp,
    tag: String = "btn_parent",
    onOpen: () -> Unit,
) {
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
                            // withTimeoutOrNull 超时 = 按住达到 1500ms → 触发家长门
                            val released = withTimeoutOrNull(1500L) { tryAwaitRelease() }
                            if (released == null) onOpen()
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
