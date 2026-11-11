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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import com.guguabc.kids.data.LearningCategory
import com.guguabc.kids.data.Words
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
 * 分类贴纸册：每个分类最多 24 词，6 列单屏；已解锁贴纸可听音复习。
 * 保留旧词 ID 与解锁记录，不显示数量、进度、问号或锁图标。
 */
@Composable
fun StickerBookScreen(
    words: List<Word>,
    learnedIds: Set<String>,
    onPlay: (Word) -> Unit,
    onBack: () -> Unit,
    initialCategory: LearningCategory = LearningCategory.ANIMALS,
) {
    var category by remember { mutableStateOf(initialCategory) }
    val visibleWords = words.filter { Words.of(it.packId).category == category }
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
            Spacer(Modifier.height(GuguDimens.menuHeaderGap))
            Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuTextGap)) {
                LearningCategory.entries.forEach { item ->
                    Row(
                        Modifier.size(GuguDimens.menuChipWidth, GuguDimens.menuChipHeight)
                            .background(if (category == item) categoryColor(item) else GuguColor.surface, RoundedCornerShape(GuguDimens.menuCardRadius))
                            .guguClick(tag = "sticker_category_${item.id}") { category = item },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Image(painterResource(Words.byId(item.heroIds.first())!!.imageRes), null, Modifier.size(GuguDimens.menuChipIcon))
                        GuguText(item.title, GuguType.categoryAction)
                    }
                }
            }
            Spacer(Modifier.height(GuguDimens.menuHeaderGap))

            Column(
                modifier = Modifier.testTag("sticker_grid"),
                verticalArrangement = Arrangement.spacedBy(GuguDimens.stickerBookGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                visibleWords.chunked(GuguDimens.stickerBookColumns).forEach { rowWords ->
                    Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.stickerBookGap)) {
                        rowWords.forEach { word ->
                            key(word.id) {
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
        modifier = Modifier.size(GuguDimens.menuStickerCell),
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
                contentDescription = word.zh,
                modifier = Modifier
                    .size(GuguDimens.menuStickerArt)
                    .alpha(if (unlocked) 1f else GuguDimens.stickerBookLockedAlpha),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
