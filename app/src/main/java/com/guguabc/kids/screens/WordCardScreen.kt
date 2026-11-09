package com.guguabc.kids.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.guguabc.kids.design.components.PrimaryButton
import com.guguabc.kids.design.components.ProgressDots
import com.guguabc.kids.design.components.RoundIconButton
import com.guguabc.kids.design.components.SoundButton

/** design.md §6.2 ② 认一认（3:241）。点插画底盘 = 点喇叭。 */
@Composable
fun WordCardScreen(
    word: Word,
    cardIndex: Int,
    soundEnabled: Boolean,
    onSound: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onToggleSound: () -> Unit,
) {
    GuguScreenContainer(background = GuguTheme.colors.background, tag = "screen_card") {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(GuguDimens.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(GuguDimens.cardGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val cardShape = RoundedCornerShape(GuguDimens.cardRadius)
            GuguShadowBox(
                spec = GuguShadow.card,
                shape = cardShape,
                modifier = Modifier
                    .width(GuguDimens.cardWidth)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GuguTheme.colors.surface, cardShape),
                    verticalArrangement = Arrangement.spacedBy(
                        GuguDimens.cardInnerGap, Alignment.CenterVertically
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IllustrationCircle(
                        circleSize = GuguDimens.cardCircle,
                        imageSize = GuguDimens.cardArt,
                        imageRes = word.imageRes,
                        background = GuguColor.cream,
                        onClick = onSound,
                    )
                    GuguText(word.en, GuguType.wordEn, Modifier.testTag("word_en"))
                    GuguText(word.zh, GuguType.wordZh, Modifier.testTag("word_zh"))
                }
            }

            Column(
                modifier = Modifier
                    .width(GuguDimens.cardRightWidth)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(
                    GuguDimens.cardRightGap, Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SoundButton(
                    size = GuguDimens.soundLgSize,
                    iconRes = R.drawable.ic_sound_lg,
                    iconSize = GuguDimens.soundLgIcon,
                    shadow = GuguShadow.coral,
                    tag = "btn_sound",
                    onClick = onSound,
                )
                GuguText(Strings.SOUND_HINT, GuguType.soundHint)
                PrimaryButton(
                    text = Strings.NEXT_CARD,
                    height = GuguDimens.nextButtonHeight,
                    width = GuguDimens.cardRightWidth,
                    textStyle = GuguType.nextButton,
                    iconRes = R.drawable.ic_arrow_right,
                    iconAtStart = false,
                    iconSize = GuguDimens.nextIcon,
                    gap = GuguDimens.nextButtonGap,
                    tag = "btn_next",
                    onClick = onNext,
                )
            }
        }

        Box(Modifier.offset(GuguDimens.progressX, GuguDimens.progressY)) {
            ProgressDots(activeIndex = cardIndex)
        }
        Box(Modifier.offset(GuguDimens.backX, GuguDimens.backY)) {
            RoundIconButton(
                size = GuguDimens.roundButton,
                iconRes = R.drawable.ic_back,
                iconSize = GuguDimens.roundButtonIcon,
                tag = "btn_back",
                onClick = onBack,
            )
        }
        Box(Modifier.offset(GuguDimens.volumeX, GuguDimens.backY)) {
            RoundIconButton(
                size = GuguDimens.roundButton,
                iconRes = R.drawable.ic_volume,
                iconSize = GuguDimens.roundButtonIcon,
                tag = "btn_volume",
                iconAlpha = if (soundEnabled) 1f else 0.45f,
                onClick = onToggleSound,
            )
        }
    }
}
