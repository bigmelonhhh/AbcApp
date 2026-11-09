package com.guguabc.kids.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.guguabc.kids.design.components.RoundIconButton
import com.guguabc.kids.design.guguClick

/**
 * design_v2.md §5.1 贴纸册：24 词单屏铺满，点已解锁贴纸听音复习。
 * 网格按 6 列 × 4 行实现（规格原文的 4×6 在 800dp 高画布上需 1040dp，放不下，
 * 已在 design_v2.md 实现附录修正）；不显示数量、进度、问号或锁图标。
 */
@Composable
fun StickerBookScreen(
    words: List<Word>,
    learnedIds: Set<String>,
    onPlay: (Word) -> Unit,
    onBack: () -> Unit,
) {
    GuguScreenContainer(background = GuguTheme.colors.background, tag = "screen_sticker_book") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = GuguDimens.stickerBookTitleY),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GuguText(Strings.STICKER_BOOK_TITLE, GuguType.stickerBookTitle)
            Spacer(Modifier.height(GuguDimens.stickerBookHintGap))
            GuguText(Strings.STICKER_BOOK_HINT, GuguType.stickerBookHint)
            Spacer(Modifier.height(GuguDimens.stickerBookGridGap))

            Column(
                modifier = Modifier.testTag("sticker_grid"),
                verticalArrangement = Arrangement.spacedBy(GuguDimens.stickerBookGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                words.chunked(GuguDimens.stickerBookColumns).forEach { rowWords ->
                    Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.stickerBookGap)) {
                        rowWords.forEach { word ->
                            StickerCell(
                                word = word,
                                unlocked = word.id in learnedIds,
                                onPlay = onPlay,
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.offset(GuguDimens.stickerBookBackX, GuguDimens.stickerBookBackY)) {
            RoundIconButton(
                size = GuguDimens.roundButton,
                iconRes = R.drawable.ic_back,
                iconSize = GuguDimens.roundButtonIcon,
                tag = "btn_back",
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun StickerCell(word: Word, unlocked: Boolean, onPlay: (Word) -> Unit) {
    val shape = RoundedCornerShape(GuguDimens.stickerBookRadius)
    GuguShadowBox(
        spec = GuguShadow.sticker,
        shape = shape,
        modifier = Modifier.size(GuguDimens.stickerBookCell),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (unlocked) GuguTheme.colors.surface else GuguColor.divider,
                    shape,
                )
                .guguClick(tag = "sticker_${word.id}", enabled = unlocked) { onPlay(word) },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(word.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(GuguDimens.stickerBookArt)
                    .alpha(if (unlocked) 1f else GuguDimens.stickerBookLockedAlpha),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
