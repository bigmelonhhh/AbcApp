package com.guguabc.kids.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.guguabc.kids.design.components.ProgressDots
import com.guguabc.kids.design.components.RoundIconButton
import com.guguabc.kids.design.guguClick

/** design.md §6.3 ③ 听一听（3:242）。选项图上禁止出现任何文字 / 字母 / 序号。 */
@Composable
fun ListenQuizScreen(
    targetWord: Word,
    optionA: Word,
    optionB: Word,
    quizIndex: Int,
    soundEnabled: Boolean,
    onSound: () -> Unit,
    onPick: (Word) -> Unit,
    onBack: () -> Unit,
    onToggleSound: () -> Unit,
) {
    GuguScreenContainer(background = GuguTheme.colors.background, tag = "screen_quiz") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(GuguDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(
                GuguDimens.quizGap, Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val hintShape = RoundedCornerShape(GuguDimens.quizHintRadius)
            GuguShadowBox(
                spec = GuguShadow.cardSm,
                shape = hintShape,
                modifier = Modifier
                    .width(GuguDimens.quizHintWidth)
                    .height(GuguDimens.quizHintHeight),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GuguTheme.colors.surface, hintShape)
                        .padding(GuguDimens.quizHintPadding)
                        .guguClick(tag = "btn_sound", onClick = onSound),
                    horizontalArrangement = Arrangement.spacedBy(GuguDimens.quizHintGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 提示条整体可点（幼儿更易命中），小喇叭本身只是图形，避免嵌套点击
                    SpeakerBadge(size = GuguDimens.soundMdSize, iconSize = GuguDimens.soundMdIcon)
                    Column(verticalArrangement = Arrangement.spacedBy(GuguDimens.quizTextGap)) {
                        GuguText(Strings.QUIZ_TITLE.format(targetWord.en), GuguType.quizTitle)
                        GuguText(Strings.QUIZ_HINT, GuguType.quizHint)
                    }
                }
            }

            // 两卡间距固定 72dp，不得改成 SPACE_BETWEEN
            Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.optionGap)) {
                QuizOption(word = optionA, tag = "option_a", onPick = onPick)
                QuizOption(word = optionB, tag = "option_b", onPick = onPick)
            }

            GuguText(
                text = Strings.QUIZ_FOOTER,
                style = GuguType.quizFooter,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        Box(Modifier.offset(GuguDimens.progressX, GuguDimens.progressY)) {
            ProgressDots(activeIndex = quizIndex)
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

@Composable
private fun SpeakerBadge(size: Dp, iconSize: Dp) {
    Box(
        modifier = Modifier.size(size).background(GuguColor.coral, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_sound_md),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun QuizOption(word: Word, tag: String, onPick: (Word) -> Unit) {
    val shape = RoundedCornerShape(GuguDimens.optionRadius)
    GuguShadowBox(
        spec = GuguShadow.option,
        shape = shape,
        modifier = Modifier.size(GuguDimens.optionWidth, GuguDimens.optionHeight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GuguTheme.colors.surface, shape)
                .guguClick(tag = tag, onClick = { onPick(word) }),
            contentAlignment = Alignment.Center,
        ) {
            IllustrationCircle(
                circleSize = GuguDimens.optionCircle,
                imageSize = GuguDimens.optionArt,
                imageRes = word.imageRes,
                background = GuguColor.cream,
            )
        }
    }
}
