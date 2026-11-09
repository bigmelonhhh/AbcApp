package com.guguabc.kids.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.guguabc.kids.R
import com.guguabc.kids.data.Strings
import com.guguabc.kids.data.Word
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguShadow
import com.guguabc.kids.design.GuguShadowBox
import com.guguabc.kids.design.GuguText
import com.guguabc.kids.design.GuguTheme
import com.guguabc.kids.design.GuguType
import com.guguabc.kids.design.components.GuguScreenContainer
import com.guguabc.kids.design.components.IllustrationCircle
import com.guguabc.kids.design.components.ParentGateButton
import com.guguabc.kids.design.components.PrimaryButton
import com.guguabc.kids.design.guguClick

/** design.md §6.5 ⑤ 今天真棒（3:244）。家长卡数据全部来自运行时。 */
@Composable
fun DoneScreen(
    learnedWords: List<Word>,
    minutesUsed: Int,
    replayEnabled: Boolean,
    onReplay: () -> Unit,
    onRest: () -> Unit,
    onParentGate: () -> Unit,
) {
    GuguScreenContainer(background = GuguTheme.colors.background, tag = "screen_done") {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(GuguDimens.donePadding),
            horizontalArrangement = Arrangement.spacedBy(GuguDimens.doneGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(GuguDimens.doneLeftWidth)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(
                    GuguDimens.doneLeftGap, Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IllustrationCircle(
                    circleSize = GuguDimens.doneMascotCircle,
                    imageSize = GuguDimens.doneMascotArt,
                    imageRes = R.drawable.ic_gugu_mascot,
                    background = GuguColor.cream,
                )
                GuguText(Strings.DONE_TITLE, GuguType.doneTitle)
                GuguText(Strings.DONE_SUBTITLE.format(learnedWords.size), GuguType.doneSubtitle)

                Row(
                    modifier = Modifier.testTag("sticker_wall"),
                    horizontalArrangement = Arrangement.spacedBy(GuguDimens.stickerGap),
                ) {
                    learnedWords.forEach { Sticker(it) }
                }
            }

            Column(
                modifier = Modifier
                    .width(GuguDimens.doneRightWidth)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(
                    GuguDimens.doneRightGap, Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryButton(
                    text = Strings.REPLAY,
                    height = GuguDimens.replayHeight,
                    width = GuguDimens.doneRightWidth,
                    textStyle = GuguType.replayButton,
                    tag = "btn_replay",
                    enabled = replayEnabled,
                    onClick = onReplay,
                )

                val restShape = RoundedCornerShape(GuguDimens.restHeight / 2)
                Box(
                    modifier = Modifier
                        .width(GuguDimens.doneRightWidth)
                        .height(GuguDimens.restHeight)
                        .background(GuguTheme.colors.surface, restShape)
                        .border(GuguDimens.restStroke, GuguColor.divider, restShape)
                        .guguClick(tag = "btn_rest", onClick = onRest),
                    contentAlignment = Alignment.Center,
                ) {
                    GuguText(Strings.REST, GuguType.restButton)
                }

                val cardShape = RoundedCornerShape(GuguDimens.parentCardRadius)
                Column(
                    modifier = Modifier
                        .width(GuguDimens.doneRightWidth)
                        .height(GuguDimens.parentCardHeight)
                        .background(GuguColor.parentCard, cardShape)
                        .padding(GuguDimens.parentCardPadding)
                        .testTag("parent_summary"),
                    verticalArrangement = Arrangement.spacedBy(
                        GuguDimens.parentCardGap, Alignment.CenterVertically
                    ),
                ) {
                    GuguText(Strings.PARENT_CARD_TITLE, GuguType.parentTitle)
                    GuguText(
                        Strings.PARENT_CARD_DATA.format(
                            minutesUsed,
                            learnedWords.joinToString(" / ") { it.en },
                        ),
                        GuguType.parentData,
                    )
                    GuguText(Strings.PARENT_CARD_HINT, GuguType.parentHint)
                }
            }
        }

        Box(
            Modifier
                .offset(GuguDimens.doneBadgeX, GuguDimens.doneBadgeY)
                .size(GuguDimens.doneBadgeWidth, GuguDimens.doneBadgeHeight),
            contentAlignment = Alignment.Center,
        ) {
            GuguText(Strings.DONE_BADGE, GuguType.doneBadge)
        }
        Box(Modifier.offset(GuguDimens.doneParentX, GuguDimens.doneParentY)) {
            ParentGateButton(
                size = GuguDimens.doneParentSize,
                iconRes = R.drawable.ic_parent_sm,
                iconSize = GuguDimens.doneParentIcon,
                onOpen = onParentGate,
            )
        }
    }
}

@Composable
private fun Sticker(word: Word) {
    val shape = RoundedCornerShape(GuguDimens.stickerRadius)
    GuguShadowBox(
        spec = GuguShadow.sticker,
        shape = shape,
        modifier = Modifier.size(GuguDimens.stickerSize),
    ) {
        Box(
            Modifier.fillMaxSize().background(GuguTheme.colors.surface, shape),
            contentAlignment = Alignment.Center,
        ) {
            // 设计稿贴纸内只有 132×132 插画，没有圆形底盘 → 直接用 Image(Fit)
            Image(
                painter = painterResource(word.imageRes),
                contentDescription = null,
                modifier = Modifier.size(GuguDimens.stickerArt),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
