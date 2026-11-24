package com.guguabc.kids.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.guguabc.kids.R
import com.guguabc.kids.data.ColorActivity
import com.guguabc.kids.data.ColorGroup
import com.guguabc.kids.data.ColorId
import com.guguabc.kids.data.ColorObject
import com.guguabc.kids.data.Strings
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguText
import com.guguabc.kids.design.GuguType
import com.guguabc.kids.design.components.GuguScreenContainer
import com.guguabc.kids.design.guguClick
import com.guguabc.kids.state.ColorFeedback
import com.guguabc.kids.state.GuguRoute
import com.guguabc.kids.state.GuguSession

@Composable
internal fun ColorMenuScreen(route: GuguRoute.ColorMenu, session: GuguSession) {
    ColorFrame("screen_color_menu", session, if (route.choosingGame) Strings.COLOR_BACK else Strings.HOME) {
        GuguText(if (route.choosingGame) Strings.COLOR_PLAY else Strings.COLOR_WORLD, GuguType.categoryHeading)
        Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
            ColorGroup.entries.forEach { group ->
                val selected = group == route.group
                val shape = RoundedCornerShape(GuguDimens.menuCardRadius)
                Row(
                    Modifier.width(GuguDimens.colorGroupWidth).height(GuguDimens.menuFooterHeight)
                        .background(if (selected) GuguColor.colorWorld else GuguColor.surface, shape)
                        .then(if (selected) Modifier.border(GuguDimens.colorBorder, GuguColor.learningPurple, shape) else Modifier)
                        .guguClick(tag = "color_group_${group.name.lowercase()}") { session.selectColorGroup(route, group) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorBalloons(group.colors, Modifier.size(GuguDimens.menuFooterHeight))
                    GuguText(group.title, GuguType.categoryAction)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
            val activities = if (route.choosingGame) ColorActivity.entries.drop(1) else listOf(ColorActivity.LEARN, null)
            activities.forEach { activity ->
                val tag = activity?.name?.lowercase() ?: "play"
                MenuCard(
                    if (activity == ColorActivity.LEARN || activity == ColorActivity.LISTEN) GuguColor.colorWorld else GuguColor.cream,
                    "color_activity_$tag", Modifier.weight(1f), {
                        if (activity == null) session.chooseColorGame(route)
                        else session.startColorActivity(route, route.group, activity)
                    }, height = GuguDimens.colorMenuCard,
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (activity) {
                            ColorActivity.MATCH -> ColorArt(ColorObject.BLOCK, route.group.colors.first().swatch, Modifier.size(GuguDimens.colorMenuArt))
                            ColorActivity.PAINT -> ColorArt(ColorObject.BALLOON, route.group.colors.last().swatch, Modifier.size(GuguDimens.colorMenuArt))
                            ColorActivity.LISTEN -> Image(
                                painterResource(R.drawable.ic_sound_lg), null, Modifier.size(GuguDimens.colorMenuArt),
                                colorFilter = ColorFilter.tint(GuguColor.learningPurple),
                            )
                            ColorActivity.LEARN -> ColorBalloons(route.group.colors, Modifier.size(GuguDimens.colorMenuArt))
                            null -> Row {
                                ColorObject.entries.zip(route.group.colors).forEach { (kind, color) ->
                                    ColorArt(kind, color.swatch, Modifier.size(GuguDimens.colorExample))
                                }
                            }
                        }
                        GuguText(activity?.title ?: Strings.COLOR_PLAY, GuguType.categoryLesson)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun ColorLearnScreen(route: GuguRoute.ColorLearn, session: GuguSession) {
    val color = route.group.colors[route.index]
    val example = ColorObject.entries[(route.index + route.exampleOffset) % ColorObject.entries.size]
    ColorFrame("screen_color_learn", session) {
        GuguText(Strings.COLOR_LEARN, GuguType.categoryHeading)
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.guguClick(tag = "color_card") { session.playColorPrompt(route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GuguDimens.menuHeaderGap),
            ) {
                Swatch(color, Modifier.size(GuguDimens.colorArtLarge))
                ColorArt(example, color.swatch, Modifier.size(GuguDimens.colorExample))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GuguText(color.en, GuguType.wordEn)
                GuguText(color.zh, GuguType.categoryHeading)
                ColorAction(Strings.COLOR_HEAR, "color_hear") { session.playColorPrompt(route) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
            if (route.index > 0) ColorAction(Strings.COLOR_PREVIOUS, "color_previous") { session.moveColorCard(route, -1) }
            val label = if (route.index == route.group.colors.lastIndex) Strings.COLOR_FINISH else Strings.COLOR_NEXT
            ColorAction(label, "color_next") { session.moveColorCard(route, 1) }
        }
    }
}

@Composable
internal fun ColorGameScreen(route: GuguRoute.ColorGame, session: GuguSession) {
    val activity = route.round.activity
    val question = route.question
    val feedback = route.feedback
    ColorFrame("screen_color_game", session) {
        GuguText(activity.title, GuguType.categoryHeading)
        val instruction = when (feedback) {
            ColorFeedback.NONE -> activity.instruction
            ColorFeedback.DEMO -> Strings.COLOR_TRY_AGAIN
            ColorFeedback.CORRECT -> Strings.COLOR_PRAISE
        }
        GuguText(instruction, GuguType.categoryAction)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (activity) {
                ColorActivity.MATCH -> ColorArt(
                    question.example, question.target.swatch,
                    Modifier.fillMaxHeight().width(GuguDimens.colorArtLarge)
                        .semantics { contentDescription = Strings.COLOR_TARGET },
                )
                ColorActivity.PAINT -> BalloonPainting(
                    route.round.questions.mapIndexed { index, item ->
                        item.target.takeIf { index < route.index || (index == route.index && feedback == ColorFeedback.CORRECT) }
                    },
                    Modifier.fillMaxHeight(),
                    activeIndex = route.index.takeUnless { feedback == ColorFeedback.CORRECT },
                )
                ColorActivity.LISTEN -> {
                    if (feedback == ColorFeedback.CORRECT) {
                        Image(
                            painterResource(R.drawable.ic_gugu_mascot), null,
                            Modifier.size(GuguDimens.colorExample),
                        )
                    } else {
                        Image(
                            painterResource(R.drawable.ic_sound_lg), Strings.COLOR_HEAR,
                            Modifier.size(GuguDimens.colorMenuArt)
                                .guguClick(tag = "color_sound_icon") { session.playColorPrompt(route) },
                            colorFilter = ColorFilter.tint(GuguColor.learningPurple),
                        )
                    }
                }
                ColorActivity.LEARN -> Unit
            }
        }
        if (activity == ColorActivity.PAINT && feedback == ColorFeedback.NONE) {
            GuguText(if (session.colorChineseHelp || !session.soundEnabled) question.target.zh else question.target.en, GuguType.categoryAction)
        }
        if (activity != ColorActivity.MATCH && !session.soundEnabled && feedback == ColorFeedback.NONE) {
            Swatch(question.target, Modifier.size(GuguDimens.colorSwatch).testTag("color_silent_prompt"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
            question.options.forEachIndexed { index, color ->
                val highlight = feedback != ColorFeedback.NONE && color == question.target
                val shape = RoundedCornerShape(GuguDimens.menuCardRadius)
                Box(
                    Modifier.size(GuguDimens.colorChoiceWidth, GuguDimens.colorChoiceHeight)
                        .background(GuguColor.surface, shape)
                        .then(if (highlight) Modifier.border(GuguDimens.colorBorder, GuguColor.learningPurple, shape) else Modifier)
                        .semantics { contentDescription = color.zh }
                        .guguClick(tag = "color_option_$index", enabled = feedback == ColorFeedback.NONE) {
                            session.pickColor(route, color)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (activity == ColorActivity.MATCH) {
                        ColorArt(question.choices, color.swatch, Modifier.size(GuguDimens.colorChoiceArt))
                    } else {
                        Swatch(color, Modifier.size(GuguDimens.colorChoiceArt))
                    }
                }
            }
        }
        Spacer(Modifier.height(GuguDimens.menuHeaderGap))
        when (feedback) {
            ColorFeedback.CORRECT -> {
                val label = if (route.index == route.round.questions.lastIndex) Strings.COLOR_FINISH else Strings.COLOR_NEXT
                ColorAction(label, "color_continue") { session.continueColorGame(route) }
            }
            ColorFeedback.NONE -> ColorAction(Strings.COLOR_HEAR, "color_hear") { session.playColorPrompt(route) }
            ColorFeedback.DEMO -> Spacer(Modifier.height(GuguDimens.menuFooterHeight))
        }
    }
}

@Composable
internal fun ColorResultScreen(
    group: ColorGroup,
    activity: ColorActivity,
    painting: List<ColorId>?,
    timeUp: Boolean,
    session: GuguSession,
    onReplay: () -> Unit,
    onChoose: () -> Unit,
    onRest: () -> Unit,
) {
    ColorFrame("screen_color_result", session) {
        val title = when {
            timeUp -> Strings.COLOR_TIMES_UP
            activity == ColorActivity.LEARN -> Strings.COLOR_LEARN_DONE
            else -> Strings.COLOR_DONE
        }
        GuguText(title, GuguType.categoryHeading)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (painting != null) {
                BalloonPainting(painting, Modifier.height(GuguDimens.colorArtLarge).testTag("color_finished_painting"))
            } else {
                ColorBalloons(group.colors, Modifier.size(GuguDimens.colorHomeHero))
            }
        }
        GuguText(Strings.COLOR_AT_HOME.format(group.colors.first().zh), GuguType.categoryAction, textAlign = TextAlign.Center)
        Spacer(Modifier.height(GuguDimens.menuHeaderGap))
        Row(horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuGridGap)) {
            if (!timeUp || !session.isTimeUp) ColorAction(Strings.COLOR_REPLAY, "color_replay", onReplay)
            ColorAction(Strings.COLOR_SWITCH, "color_switch", onChoose)
            ColorAction(Strings.COLOR_REST, "color_rest", onRest)
        }
    }
}

@Composable
private fun ColorFrame(
    tag: String,
    session: GuguSession,
    backLabel: String = Strings.COLOR_BACK,
    content: @Composable ColumnScope.() -> Unit,
) {
    GuguScreenContainer(GuguColor.background, tag) {
        Column(
            Modifier.fillMaxSize().padding(GuguDimens.menuPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MenuHeader(onBack = session::back, backLabel = backLabel, onParentGate = session::openParentPanel)
            Spacer(Modifier.height(GuguDimens.menuTitleGap))
            content()
        }
    }
}

@Composable
private fun Swatch(color: ColorId, modifier: Modifier) {
    Box(modifier.background(color.swatch, CircleShape).border(GuguDimens.colorBorder, GuguColor.artOutline, CircleShape))
}

@Composable
private fun ColorAction(label: String, tag: String, onClick: () -> Unit) {
    MenuAction(label, tag, Modifier.width(GuguDimens.colorActionWidth), onClick)
}
