package com.guguabc.kids.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.guguabc.kids.R
import com.guguabc.kids.data.Strings
import com.guguabc.kids.data.Word
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguText
import com.guguabc.kids.design.GuguTheme
import com.guguabc.kids.design.GuguType
import com.guguabc.kids.design.components.GuguScreenContainer
import com.guguabc.kids.design.components.IllustrationCircle
import com.guguabc.kids.design.components.PrimaryButton
import com.guguabc.kids.design.components.RoundIconButton

/** design.md §6.4 ④ 答对啦（3:243）。第三颗星保持空心，不自行填满。 */
@Composable
fun CheerScreen(
    word: Word,
    quizNumber: Int,
    canContinue: Boolean,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    GuguScreenContainer(background = GuguTheme.colors.backgroundCheer, tag = "screen_cheer") {
        // 彩纸静帧，不做动画
        Image(
            painter = painterResource(R.drawable.confetti_left),
            contentDescription = null,
            modifier = Modifier
                .offset(GuguDimens.confettiLeftX, GuguDimens.confettiY)
                .size(GuguDimens.confettiWidth, GuguDimens.confettiHeight),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.confetti_right),
            contentDescription = null,
            modifier = Modifier
                .offset(GuguDimens.confettiRightX, GuguDimens.confettiY)
                .size(GuguDimens.confettiWidth, GuguDimens.confettiHeight),
            contentScale = ContentScale.Fit,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(GuguDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(
                GuguDimens.cheerGap, Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IllustrationCircle(
                circleSize = GuguDimens.cheerMascotCircle,
                imageSize = GuguDimens.cheerMascotArt,
                imageRes = R.drawable.ic_gugu_mascot,
                background = GuguColor.creamCheer,
            )
            GuguText(Strings.CHEER_TITLE, GuguType.cheerTitle)
            GuguText(Strings.CHEER_SUBTITLE.format(word.en), GuguType.cheerSubtitle)

            Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.starGap)) {
                listOf(
                    R.drawable.star_filled_1,
                    R.drawable.star_filled_2,
                    R.drawable.star_empty,
                ).forEach { star ->
                    Image(
                        painter = painterResource(star),
                        contentDescription = null,
                        modifier = Modifier.size(GuguDimens.starSize),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            PrimaryButton(
                text = Strings.CONTINUE,
                height = GuguDimens.continueHeight,
                width = GuguDimens.continueWidth,
                textStyle = GuguType.continueButton,
                iconRes = R.drawable.ic_arrow_right_sm,
                iconAtStart = false,
                iconSize = GuguDimens.continueIcon,
                gap = GuguDimens.continueGap,
                tag = "btn_continue",
                enabled = canContinue,
                onClick = onContinue,
            )
            GuguText(
                text = Strings.CHEER_FOOTER,
                style = GuguType.cheerFooter,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
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
        Box(
            Modifier
                .offset(GuguDimens.cheerProgressX, GuguDimens.cheerProgressY)
                .size(GuguDimens.cheerProgressWidth, GuguDimens.cheerProgressHeight),
            contentAlignment = Alignment.Center,
        ) {
            GuguText(
                text = Strings.CHEER_PROGRESS.format(quizNumber),
                style = GuguType.cheerProgress,
                textAlign = TextAlign.Center,
            )
        }
    }
}
