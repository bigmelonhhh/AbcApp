package com.guguabc.kids.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorRoundTest {
    @Test
    fun bothGroupsCoverSixDifferentColorsWithDedicatedAudio() {
        val colors = ColorGroup.entries.flatMap { it.colors }
        assertEquals(ColorId.entries, colors)
        assertEquals(6, colors.map { it.id }.toSet().size)
        assertEquals(6, colors.map { it.zh }.toSet().size)
        assertEquals(6, colors.map { it.en }.toSet().size)
    }

    @Test
    fun everyRoundUsesEachColorOnceAsTargetAndOnceAsDistractor() {
        for (group in ColorGroup.entries) {
            val orders = mutableSetOf<List<ColorId>>()
            val positions = mutableSetOf<List<Int>>()
            for (seed in 0..40) {
                val round = ColorRound.create(group, ColorActivity.MATCH, seed)
                assertEquals(round, ColorRound.create(group, ColorActivity.MATCH, seed))
                val targets = round.questions.map { it.target }
                val distractors = round.questions.map { question -> question.options.single { it != question.target } }
                assertEquals(group.colors.toSet(), targets.toSet())
                assertEquals(group.colors.toSet(), distractors.toSet())
                assertTrue(round.questions.all { it.options.size == 2 })
                val sides = round.questions.map { it.options.indexOf(it.target) }
                assertEquals(setOf(0, 1), sides.toSet())
                assertEquals(round.questions[0].example, round.questions[0].choices)
                assertTrue(round.questions.drop(1).all { it.example != it.choices })
                orders.add(targets)
                positions.add(sides)
            }
            assertTrue(orders.size > 1)
            assertEquals(6, positions.size)
        }
    }

    @Test
    fun repeatCompletionKeepsBothGroupsWithoutAddingDuplicates() {
        val first = colorRewards(emptySet(), ColorGroup.PRIMARY)
        val both = colorRewards(first, ColorGroup.SECONDARY)
        assertEquals(3, first.size)
        assertEquals(6, both.size)
        assertEquals(both, colorRewards(both, ColorGroup.PRIMARY))
    }
}
