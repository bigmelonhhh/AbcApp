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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.guguabc.kids.data.Strings
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguText
import com.guguabc.kids.design.GuguTheme
import com.guguabc.kids.design.GuguType
import com.guguabc.kids.design.guguClick
import kotlinx.coroutines.delay

/**
 * design.md §7.5 / 附录 C ④ + design_v2.md §5.2 / 附录「交互修复决议」⑤⑥：
 * 点击家长入口打开的面板；仅管理每日时长与声音；宝宝在分类页自主选课。
 * - 「每日时长上限」行改为可点「重置今日时长」，只清时长不清进度；
 * - 自动关闭由 8s 延长到 20s：给家长足够时间查看设置。
 */
@Composable
fun ParentPanel(session: GuguSession, onClose: () -> Unit) {
    ParentPanelContent(
        soundEnabled = session.soundEnabled,
        minutesUsed = session.minutesUsed,
        onResetToday = session::resetTodayTime,
        onToggleSound = session::toggleSound,
        onClose = onClose,
    )
}

/**
 * 家长面板的可测版本：状态与回调全部由参数注入，Compose UI 测试无需构造
 * Context / DataStore / MediaPlayer。生产入口 ParentPanel 只做一层转发，
 * 面板布局、20s 自动关闭、重置提示等行为完全一致。
 */
@Composable
fun ParentPanelContent(
    soundEnabled: Boolean,
    minutesUsed: Int,
    onResetToday: () -> Unit,
    onToggleSound: () -> Unit,
    onClose: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(20_000L)
        onClose()
    }

    var resetDone by remember { mutableStateOf(false) }
    LaunchedEffect(resetDone) {
        if (resetDone) {
            delay(2_000L)
            resetDone = false
        }
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
                .width(GuguDimens.panelWidthV2)
                .height(GuguDimens.panelHeight)
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
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(GuguDimens.panelGap),
        ) {
            GuguText(Strings.PARENT_PANEL_TITLE, GuguType.panelTitle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .guguClick(tag = "btn_reset_today") {
                        onResetToday()
                        resetDone = true
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GuguText(
                    Strings.PARENT_PANEL_LIMIT + " · " + Strings.MINUTES_5,
                    GuguType.panelLabel,
                )
                GuguText(
                    if (resetDone) Strings.RESET_DONE else Strings.RESET_TODAY,
                    GuguType.panelValue,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleSound,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GuguText(Strings.PARENT_PANEL_SOUND, GuguType.panelLabel)
                GuguText(
                    if (soundEnabled) Strings.STATE_ON else Strings.STATE_OFF,
                    GuguType.panelValue,
                )
            }

            GuguText(
                Strings.PARENT_PANEL_TODAY.format(minutesUsed),
                GuguType.panelValue,
            )

            // design_v2.md §8：合成语音需向家长明示，不放在宝宝侧
            GuguText(Strings.TTS_NOTICE_LINE, GuguType.ttsNotice)

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
