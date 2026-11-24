package com.guguabc.kids.data

import androidx.annotation.RawRes
import com.guguabc.kids.R
import kotlin.random.Random

enum class ColorId(val id: String, val zh: String, val en: String, @RawRes val english: Int, @RawRes val bilingual: Int) {
    RED("red", "红色", "Red", R.raw.color_red_en, R.raw.color_red_bilingual),
    YELLOW("yellow", "黄色", "Yellow", R.raw.color_yellow_en, R.raw.color_yellow_bilingual),
    BLUE("blue", "蓝色", "Blue", R.raw.color_blue_en, R.raw.color_blue_bilingual),
    GREEN("green", "绿色", "Green", R.raw.color_green_en, R.raw.color_green_bilingual),
    ORANGE("orange", "橙色", "Orange", R.raw.color_orange_en, R.raw.color_orange_bilingual),
    PURPLE("purple", "紫色", "Purple", R.raw.color_purple_en, R.raw.color_purple_bilingual),
}

enum class ColorGroup(val title: String, val colors: List<ColorId>) {
    PRIMARY(Strings.COLOR_PRIMARY, listOf(ColorId.RED, ColorId.YELLOW, ColorId.BLUE)),
    SECONDARY(Strings.COLOR_SECONDARY, listOf(ColorId.GREEN, ColorId.ORANGE, ColorId.PURPLE)),
}

enum class ColorActivity(val title: String, val instruction: String) {
    LEARN(Strings.COLOR_LEARN, Strings.COLOR_LEARN_HINT),
    MATCH(Strings.COLOR_MATCH, Strings.COLOR_MATCH_HINT),
    LISTEN(Strings.COLOR_LISTEN, Strings.COLOR_LISTEN_HINT),
    PAINT(Strings.COLOR_PAINT, Strings.COLOR_PAINT_HINT),
}

enum class ColorObject { BALLOON, BLOCK, CAR }

data class ColorQuestion(
    val target: ColorId,
    val options: List<ColorId>,
    val example: ColorObject,
    val choices: ColorObject,
)

data class ColorRound(val group: ColorGroup, val activity: ColorActivity, val questions: List<ColorQuestion>) {
    companion object {
        fun create(group: ColorGroup, activity: ColorActivity, seed: Int): ColorRound {
            require(activity != ColorActivity.LEARN)
            val random = Random(seed)
            val targets = group.colors.shuffled(random)
            val leftFirst = random.nextBoolean()
            // Select a mixed pattern, including runs of two, so positions cannot be predicted by alternating.
            val differingIndex = random.nextInt(targets.size)
            val offset = random.nextInt(1, targets.size)
            val objects = ColorObject.entries.shuffled(random)
            val questions = targets.mapIndexed { index, target ->
                val distractor = targets[(index + offset) % targets.size]
                val onLeft = if (index == differingIndex) !leftFirst else leftFirst
                ColorQuestion(
                    target,
                    if (onLeft) listOf(target, distractor) else listOf(distractor, target),
                    objects[index],
                    if (index == 0) objects[index] else objects[(index + 1) % objects.size],
                )
            }
            return ColorRound(group, activity, questions)
        }
    }
}

internal fun colorRewards(existing: Set<String>, group: ColorGroup): Set<String> =
    existing + group.colors.map(ColorId::id)
