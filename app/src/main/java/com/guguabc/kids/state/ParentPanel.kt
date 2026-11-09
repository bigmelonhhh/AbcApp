package com.guguabc.kids.state

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.guguabc.kids.data.Strings
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguText
import com.guguabc.kids.design.GuguTheme
import com.guguabc.kids.design.GuguType
import kotlinx.coroutines.delay

/**
 * design.md §7.5 / 附录 C ④：长按 1.5s 后的最小设置面板。
 * 不新增页面、不跳转；8 秒无操作自动关闭；短按无反应。
 * 样式由实现侧补充：40% 黑色遮罩 + 底部居中白色圆角 32dp 卡片。
 */
@Composable
fun ParentPanel(session: GuguSession, onClose: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(8_000L)
        onClose()
    }

    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GuguColor.scrim)
            .clickable(
                interactionSource = noRipple,
                indication = null,
                onClick = onClose,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -GuguDimens.panelBottom)
                .width(GuguDimens.panelWidth)
                .background(
                    GuguTheme.colors.surface,
                    RoundedCornerShape(GuguDimens.panelRadius),
                )
                .padding(GuguDimens.panelPadding)
                // 吃掉卡片内的点击，避免穿透到遮罩导致误关闭
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            verticalArrangement = Arrangement.spacedBy(GuguDimens.panelGap),
        ) {
            GuguText(Strings.PARENT_PANEL_TITLE, GuguType.panelTitle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GuguText(Strings.PARENT_PANEL_LIMIT, GuguType.panelLabel)
                GuguText(Strings.MINUTES_5, GuguType.panelValue)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { session.toggleSound() },
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GuguText(Strings.PARENT_PANEL_SOUND, GuguType.panelLabel)
                GuguText(
                    if (session.soundEnabled) Strings.STATE_ON else Strings.STATE_OFF,
                    GuguType.panelValue,
                )
            }

            GuguText(
                Strings.PARENT_PANEL_TODAY.format(session.minutesUsed),
                GuguType.panelValue,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                GuguText(
                    Strings.PARENT_PANEL_CLOSE,
                    GuguType.panelClose,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
