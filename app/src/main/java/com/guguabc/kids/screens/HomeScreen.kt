package com.guguabc.kids.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.guguabc.kids.R
import com.guguabc.kids.data.LearningCategory
import com.guguabc.kids.data.Strings
import com.guguabc.kids.data.Words
import com.guguabc.kids.design.*
import com.guguabc.kids.design.components.GuguScreenContainer
import com.guguabc.kids.design.components.ParentGateButton

/** Approved V2: picture categories followed by picture-based lesson selection. */
@Composable
fun HomeScreen(onCategory: (LearningCategory) -> Unit, onStickerBook: () -> Unit, onParentGate: () -> Unit) {
    GuguScreenContainer(GuguColor.background, "screen_home") {
        Column(Modifier.fillMaxSize().padding(GuguDimens.menuPadding)) {
            MenuHeader(onParentGate = onParentGate)
            Spacer(Modifier.height(GuguDimens.menuTitleGap))
            GuguText(Strings.EXPLORE_TITLE, GuguType.categoryHeading, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(GuguDimens.menuGridTop))
            Column(verticalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
                LearningCategory.entries.chunked(2).forEach { categories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
                        categories.forEach { category ->
                            MenuCard(categoryColor(category), "category_${category.id}", Modifier.weight(1f), { onCategory(category) }) {
                                Row(Modifier.fillMaxSize().padding(GuguDimens.menuCardPadding), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(GuguDimens.menuHeroWidth).fillMaxHeight()) {
                                        category.heroIds.forEachIndexed { index, id ->
                                            Image(painterResource(Words.byId(id)!!.imageRes), null,
                                                Modifier.align(Alignment.CenterStart).offset(
                                                    x = GuguDimens.menuHeroOffset * index,
                                                    y = if (index == 0) -GuguDimens.menuHeroRise else GuguDimens.menuHeroRise,
                                                ).size(GuguDimens.menuHeroArt), contentScale = ContentScale.Fit)
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(GuguDimens.menuTextGap)) {
                                        GuguText(category.title, GuguType.categoryName)
                                        GuguText(category.english, GuguType.categoryEnglish)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            MenuAction(Strings.MY_STICKERS, "btn_sticker_book", Modifier.align(Alignment.CenterHorizontally).width(GuguDimens.menuAlbumWidth), onStickerBook)
        }
    }
}

@Composable
fun LessonSelectionScreen(category: LearningCategory, page: Int, onSelect: (String) -> Unit, onPage: (Int) -> Unit, onBack: () -> Unit, onParentGate: () -> Unit) {
    val pages = Words.inCategory(category).chunked(4)
    val visiblePacks = pages[page.coerceIn(pages.indices)]
    val spacious = visiblePacks.size <= 2
    GuguScreenContainer(GuguColor.background, "screen_lessons") {
        Column(Modifier.fillMaxSize().padding(GuguDimens.menuPadding)) {
            MenuHeader(onBack, onParentGate)
            Spacer(Modifier.height(GuguDimens.menuTitleGap))
            GuguText(category.heading, GuguType.categoryHeading, Modifier.align(Alignment.CenterHorizontally))
            GuguText(Strings.CHOOSE_LESSON, GuguType.stickerBookHint, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(GuguDimens.menuGridTop))
            if (spacious) Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
                visiblePacks.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
                        row.forEach { pack ->
                            MenuCard(
                                categoryColor(category), "lesson_${pack.id}", Modifier.weight(1f), { onSelect(pack.id) },
                                height = if (spacious) GuguDimens.menuFewCardHeight else GuguDimens.menuCardHeight,
                            ) {
                                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Row(
                                        modifier = if (spacious) Modifier.padding(horizontal = GuguDimens.menuCardPadding) else Modifier,
                                        horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuLessonArtGap),
                                    ) {
                                        pack.words.forEach { word ->
                                            Image(painterResource(word.imageRes), word.zh,
                                                if (spacious) Modifier.weight(1f).height(GuguDimens.menuFewLessonArt) else Modifier.size(GuguDimens.menuLessonArt),
                                                contentScale = ContentScale.Fit)
                                        }
                                    }
                                    GuguText(pack.title, GuguType.categoryLesson)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (pages.size > 1) {
                Box(Modifier.fillMaxWidth().height(GuguDimens.menuFooterHeight)) {
                    Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuDotGap)) {
                        pages.indices.forEach { index ->
                            Box(Modifier.size(GuguDimens.menuDot).background(if (index == page) GuguColor.primary else GuguColor.divider, CircleShape))
                        }
                    }
                    MenuAction(
                        if (page == pages.lastIndex) Strings.PREVIOUS_LESSONS else if (category == LearningCategory.ANIMALS) Strings.MORE_ANIMALS else Strings.MORE_LESSONS,
                        "btn_lesson_page", Modifier.align(Alignment.CenterEnd).width(GuguDimens.menuAlbumWidth),
                        { onPage(if (page == pages.lastIndex) page - 1 else page + 1) },
                    )
                }
            }
        }
    }
}

internal fun categoryColor(category: LearningCategory): Color = when (category) {
    LearningCategory.ANIMALS -> GuguColor.cream
    LearningCategory.FRUIT -> GuguColor.fruitCard
    LearningCategory.VEHICLES -> GuguColor.vehicleCard
    LearningCategory.EVERYDAY -> GuguColor.everydayCard
}

@Composable
private fun MenuHeader(onBack: (() -> Unit)? = null, onParentGate: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(GuguDimens.menuHeaderHeight), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuHeaderGap)) {
        if (onBack != null) MenuAction(Strings.HOME, "btn_back", Modifier.width(GuguDimens.menuBackWidth), onBack, GuguColor.surface)
        Image(painterResource(R.drawable.ic_gugu_mascot), null, Modifier.size(GuguDimens.menuMascot))
        GuguText(Strings.BRAND, GuguType.categoryBrand)
        Spacer(Modifier.weight(1f))
        ParentGateButton(GuguDimens.menuHeaderHeight, R.drawable.ic_parent_sm, GuguDimens.menuParentIcon, label = Strings.PARENT, onOpen = onParentGate)
    }
}

@Composable
private fun MenuCard(color: Color, tag: String, modifier: Modifier, onClick: () -> Unit, height: Dp = GuguDimens.menuCardHeight, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(GuguDimens.menuCardRadius)
    GuguShadowBox(GuguShadow.sticker, shape, modifier.height(height)) {
        Box(Modifier.fillMaxSize().background(color, shape).guguClick(tag = tag, onClick = onClick), content = content)
    }
}

@Composable
internal fun MenuAction(text: String, tag: String, modifier: Modifier, onClick: () -> Unit, color: Color = GuguColor.primary) {
    Box(modifier.height(GuguDimens.menuFooterHeight).background(color, RoundedCornerShape(GuguDimens.menuCardRadius)).guguClick(tag = tag, onClick = onClick), contentAlignment = Alignment.Center) {
        GuguText(text, GuguType.categoryAction)
    }
}
