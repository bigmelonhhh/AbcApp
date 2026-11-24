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
import androidx.compose.foundation.layout.width
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
import com.guguabc.kids.data.ColorId
import com.guguabc.kids.data.ColorObject
import com.guguabc.kids.data.LearningCategory
import com.guguabc.kids.data.Words
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    colorStickerIds: Set<String> = emptySet(),
    onPlayColor: (ColorId) -> Unit = {},
) {
    var category: LearningCategory? by remember { mutableStateOf(initialCategory) }
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
            GuguText(
                if (category == null) Strings.COLOR_RECORD else Strings.STICKER_BOOK_HINT,
                GuguType.stickerBookHint,
            )
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
                Row(
                    Modifier.size(GuguDimens.menuChipWidth, GuguDimens.menuChipHeight)
                        .background(if (category == null) GuguColor.colorWorld else GuguColor.surface, RoundedCornerShape(GuguDimens.menuCardRadius))
                        .guguClick(tag = "sticker_category_colors") { category = null },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ColorBalloons(showcaseColors, Modifier.size(GuguDimens.menuChipIcon))
                    GuguText(Strings.COLOR_WORLD, GuguType.categoryAction)
                }
            }
            Spacer(Modifier.height(GuguDimens.menuHeaderGap))

            Column(
                modifier = Modifier.testTag("sticker_grid"),
                verticalArrangement = Arrangement.spacedBy(GuguDimens.stickerBookGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (category == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.stickerBookGap)) {
                        ColorId.entries.forEach { color ->
                            val unlocked = color.id in colorStickerIds
                            val label = if (unlocked) color.zh else Strings.COLOR_STICKER_LOCKED.format(color.zh)
                            Column(
                                Modifier.width(GuguDimens.colorStickerWidth)
                                    .background(GuguColor.surface, RoundedCornerShape(GuguDimens.stickerBookRadius))
                                    .semantics { contentDescription = label }
                                    .guguClick(tag = "sticker_color_${color.id}", enabled = unlocked) { onPlayColor(color) }
                                    .padding(GuguDimens.menuHeaderGap),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ColorArt(
                                    ColorObject.BALLOON, color.swatch,
                                    Modifier.size(GuguDimens.menuStickerArt)
                                        .alpha(if (unlocked) 1f else GuguDimens.stickerBookLockedAlpha),
                                )
                                GuguText(color.zh, GuguType.categoryAction)
                            }
                        }
                    }
                }
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
