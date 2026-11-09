package com.guguabc.kids.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.guguabc.kids.R
import com.guguabc.kids.data.Strings
import com.guguabc.kids.data.WordPack
import com.guguabc.kids.data.Words
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

/**
 * design.md §6.1 ① 首页 · 横屏双栏（3:240）。
 * design_v2.md §6：课程标题与三词头像按课包动态；基准课保留 V1 逐字文案，
 * 保证 5 张像素基准图继续可复现。
 */
@Composable
fun HomeScreen(
    pack: WordPack,
    onStart: () -> Unit,
    onParentGate: () -> Unit,
) {
    GuguScreenContainer(background = GuguTheme.colors.background, tag = "screen_home") {
        val isBaseline = pack.id == Words.DEFAULT_PACK_ID
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(GuguDimens.statusBarSpace)) // 顶部 36dp 空白，不绘制状态栏

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(GuguDimens.homePadding),
                horizontalArrangement = Arrangement.spacedBy(GuguDimens.homeGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左栏 560dp：问候 / 品牌 / 气泡 / 咕咕
                Column(
                    modifier = Modifier
                        .width(GuguDimens.homeLeftWidth)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(
                        GuguDimens.homeLeftGap, Alignment.CenterVertically
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GuguText(Strings.HOME_GREETING, GuguType.homeGreeting)
                        Spacer(Modifier.height(GuguDimens.homeBrandGap))
                        GuguText(Strings.BRAND, GuguType.homeBrand)
                    }

                    GuguShadowBox(
                        spec = GuguShadow.cardSm,
                        shape = RoundedCornerShape(GuguDimens.homeBubbleRadius),
                        modifier = Modifier.wrapContentSize(),
                    ) {
                        Box(
                            Modifier
                                .background(
                                    GuguTheme.colors.surface,
                                    RoundedCornerShape(GuguDimens.homeBubbleRadius),
                                )
                                .padding(GuguDimens.homeBubblePadding),
                        ) {
                            GuguText(Strings.MASCOT_BUBBLE, GuguType.homeBubble)
                        }
                    }

                    IllustrationCircle(
                        circleSize = GuguDimens.homeMascotCircle,
                        imageSize = GuguDimens.homeMascotArt,
                        imageRes = R.drawable.ic_gugu_mascot,
                        background = GuguColor.cream,
                    )
                }

                // 右栏 568dp：今日一课 / 三词头像 / 开始玩 / 底部说明
                Column(
                    modifier = Modifier
                        .width(GuguDimens.homeRightWidth)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(
                        GuguDimens.homeRightGap, Alignment.CenterVertically
                    ),
                    horizontalAlignment = Alignment.Start,
                ) {
                    val lessonShape = RoundedCornerShape(GuguDimens.homeLessonRadius)
                    GuguShadowBox(
                        spec = GuguShadow.cardSm,
                        shape = lessonShape,
                        modifier = Modifier
                            .width(GuguDimens.homeRightWidth)
                            .height(GuguDimens.homeLessonCardHeight),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GuguTheme.colors.surface, lessonShape)
                                .padding(GuguDimens.homeLessonPadding),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            GuguText(Strings.LESSON_LABEL, GuguType.lessonLabel)
                            GuguText(
                                if (isBaseline) Strings.LESSON_TITLE else pack.title,
                                GuguType.lessonTitle,
                            )
                            GuguText(
                                if (isBaseline) Strings.LESSON_META else Strings.LESSON_META_SHORT,
                                GuguType.lessonMeta,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.homeAvatarGap)) {
                        pack.words.forEach { word ->
                            IllustrationCircle(
                                circleSize = GuguDimens.homeAvatarSize,
                                imageSize = GuguDimens.homeAvatarSize,
                                imageRes = word.imageRes,
                                background = GuguColor.cream,
                            )
                        }
                    }

                    PrimaryButton(
                        text = Strings.START,
                        height = GuguDimens.homeStartHeight,
                        width = GuguDimens.homeRightWidth,
                        textStyle = GuguType.startButton,
                        iconRes = R.drawable.ic_play,
                        iconSize = GuguDimens.homePlayIcon,
                        gap = GuguDimens.homeStartGap,
                        tag = "btn_start",
                        onClick = onStart,
                    )

                    GuguText(
                        text = Strings.HOME_FOOTER,
                        style = GuguType.footer,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // ABSOLUTE (1140, 48) —— 刻意位于安全区之外，不要居中修正
        Box(Modifier.offset(GuguDimens.homeParentX, GuguDimens.homeParentY)) {
            ParentGateButton(
                size = GuguDimens.homeParentSize,
                iconRes = R.drawable.ic_parent,
                iconSize = GuguDimens.homeParentIcon,
                onOpen = onParentGate,
            )
        }
    }
}
