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
fun HomeScreen(
    onCategory: (LearningCategory) -> Unit,
    onStickerBook: () -> Unit,
    onParentGate: () -> Unit,
    onColors: () -> Unit,
) {
    GuguScreenContainer(GuguColor.background, "screen_home") {
        Column(Modifier.fillMaxSize().padding(GuguDimens.menuPadding)) {
            MenuHeader(onParentGate = onParentGate)
            Spacer(Modifier.height(GuguDimens.menuTitleGap))
            GuguText(Strings.EXPLORE_TITLE, GuguType.categoryHeading, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(GuguDimens.menuGridTop))
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap),
            ) {
                MenuCard(
                    GuguColor.colorWorld, "category_colors", Modifier.weight(1f), onColors,
                    height = GuguDimens.colorHomeMiniCard * 2 + GuguDimens.menuGridGap,
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ColorBalloons(showcaseColors, Modifier.size(GuguDimens.colorHomeHero))
                        GuguText(Strings.COLOR_WORLD, GuguType.categoryHeading)
                        GuguText(Strings.COLOR_WORLD_EN, GuguType.categoryEnglish)
                    }
                }
                Column(
                    Modifier.weight(2f),
                    verticalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap),
                ) {
                    LearningCategory.entries.chunked(2).forEach { categories ->
                        Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
                            categories.forEach { category ->
                                MenuCard(
                                    categoryColor(category), "category_${category.id}", Modifier.weight(1f),
                                    { onCategory(category) }, height = GuguDimens.colorHomeMiniCard,
                                ) {
                                    Column(
                                        Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Row {
                                            category.heroIds.forEach { id ->
                                                Image(
                                                    painterResource(Words.byId(id)!!.imageRes), null,
                                                    Modifier.size(GuguDimens.colorHomeMiniArt),
                                                    contentScale = ContentScale.Fit,
                                                )
                                            }
                                        }
                                        GuguText(category.title, GuguType.categoryLesson)
                                        GuguText(category.english, GuguType.categoryEnglish)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(GuguDimens.menuHeaderGap))
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
            MenuHeader(onBack = onBack, onParentGate = onParentGate)
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
internal fun MenuHeader(
    onBack: (() -> Unit)? = null,
    backLabel: String = Strings.HOME,
    onParentGate: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(GuguDimens.menuHeaderHeight), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuHeaderGap)) {
        if (onBack != null) MenuAction(backLabel, "btn_back", Modifier.width(GuguDimens.menuBackWidth), onBack, GuguColor.surface)
        Image(painterResource(R.drawable.ic_gugu_mascot), null, Modifier.size(GuguDimens.menuMascot))
        GuguText(Strings.BRAND, GuguType.categoryBrand)
        Spacer(Modifier.weight(1f))
        ParentGateButton(GuguDimens.menuHeaderHeight, R.drawable.ic_parent_sm, GuguDimens.menuParentIcon, label = Strings.PARENT, onOpen = onParentGate)
    }
}

@Composable
internal fun MenuCard(color: Color, tag: String, modifier: Modifier, onClick: () -> Unit, height: Dp = GuguDimens.menuCardHeight, content: @Composable BoxScope.() -> Unit) {
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
