package com.guguabc.kids.state

import com.guguabc.kids.data.WordPack
import com.guguabc.kids.data.Words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * design_v2.md §3 · 干扰项规则的可执行断言。
 * 覆盖 8 课 × 3 词 × 多 seed，以及空候选回退、确定性、历史词混入、禁用组合。
 */
class QuizPickerTest {

    private val allLearned: Set<String> = Words.all.map { it.id }.toSet()

    private fun distractorOf(q: QuizData) = if (q.optionA.id == q.target.id) q.optionB else q.optionA

    // ---- V1 基准课：像素基准必须可复现 ----
    @Test
    fun baselinePackKeepsV1Rotation() {
        val pack = Words.of(Words.DEFAULT_PACK_ID)
        val expected = listOf("dog", "bear", "cat")
        repeat(3) { index ->
            val q = QuizPicker.pick(pack, index, emptySet(), 0)
            assertEquals(pack.words[index], q.target)
            assertEquals(expected[index], distractorOf(q).id)
            // 目标词左右轮换：偶数 index 在 A，奇数在 B
            assertEquals(q.target.id, if (index % 2 == 0) q.optionA.id else q.optionB.id)
        }
    }

    @Test
    fun baselinePackIgnoresLearnedHistoryAndSeed() {
        val pack = Words.of(Words.DEFAULT_PACK_ID)
        val a = QuizPicker.pick(pack, 1, allLearned, 0)
        val b = QuizPicker.pick(pack, 1, emptySet(), 99)
        assertEquals(a, b)
    }

    // ---- 全组合：跨 group / 禁用对 / 目标词位置 ----
    @Test
    fun allPacksAllIndicesAllSeedsObeyRules() {
        Words.packs.forEach { pack ->
            repeat(3) { index ->
                repeat(8) { seed ->
                    val q = QuizPicker.pick(pack, index, allLearned, seed)
                    val target = q.target
                    val other = distractorOf(q)
                    assertEquals("干扰项不能跨分类", pack.category, Words.of(other.packId).category)

                    assertEquals(pack.words[index].id, target.id)
                    assertNotEquals("${pack.id}/$index/seed=$seed 干扰项等于目标词", target.id, other.id)
                    assertEquals(
                        "${pack.id}/$index/seed=$seed 两选项不是一目标一干扰",
                        setOf(target.id, other.id),
                        setOf(q.optionA.id, q.optionB.id),
                    )
                    if (pack.id != Words.DEFAULT_PACK_ID) {
                        assertNotEquals(
                            "${pack.id}/$index/seed=$seed 干扰项同 group：${target.id} vs ${other.id}",
                            target.group, other.group,
                        )
                        assertFalse(
                            "${pack.id}/$index/seed=$seed 命中禁用组合：${target.id}↔${other.id}",
                            QuizPicker.FORBIDDEN_PAIRS.contains(setOf(target.id, other.id)),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun targetAlternatesLeftAndRightForEveryPack() {
        Words.packs.drop(1).forEach { pack ->
            repeat(3) { index ->
                val q = QuizPicker.pick(pack, index, allLearned, 3)
                assertEquals(
                    "${pack.id}/$index 目标词位置错误",
                    q.target.id,
                    if (index % 2 == 0) q.optionA.id else q.optionB.id,
                )
            }
        }
    }

    // ---- 候选池 / 排序 / 兜底 ----
    @Test
    fun forbiddenPairsAreLockedToDesignSpec() {
        val expected = listOf(
            setOf("lion", "tiger"), setOf("cat", "lion"), setOf("dog", "fox"),
            setOf("duck", "bird"), setOf("horse", "zebra"), setOf("turtle", "frog"),
        )
        assertEquals(6, QuizPicker.FORBIDDEN_PAIRS.size)
        expected.forEach { assertTrue("缺少禁用组合 $it", QuizPicker.FORBIDDEN_PAIRS.contains(it)) }
    }

    @Test
    fun everyGroupUsedByCatalogHasARank() {
        Words.all.forEach { w ->
            assertTrue("${w.id} 的 group=${w.group} 未配 GROUP_RANK",
                QuizPicker.GROUP_RANK.containsKey(w.group))
        }
    }

    @Test
    fun learnedHistoryEntersCandidatePoolForSpacedRepetition() {
        val pack = Words.of(Words.P6)
        val target = pack.words[0]   // Lion
        val without = QuizPicker.candidates(pack, target, emptySet())
        val withHistory = QuizPicker.candidates(pack, target, setOf("cat", "dog", "bear"))
        assertTrue("历史词未进入候选池", withHistory.size > without.size)
        assertTrue("历史词 dog 应可作为干扰项", withHistory.any { it.id == "dog" })
        assertTrue("候选池规则被破坏", withHistory.none { it.group == target.group })
    }

    @Test
    fun emptyCandidatePoolFallsBackToInLessonRotation() {
        val template = Words.all.first()
        val sameGroup = (0..2).map { i ->
            template.copy(id = "fake$i", en = "Fake$i", zh = "假$i", packId = "p_fake", group = "feline")
        }
        val pack = WordPack("p_fake", 9, "全同组", "L1", sameGroup)
        assertEquals(emptyList<com.guguabc.kids.data.Word>(),
            QuizPicker.candidates(pack, sameGroup[0], emptySet()))
        val q = QuizPicker.pick(pack, 0, emptySet(), 0)
        assertEquals("fake1", distractorOf(q).id)
    }

    // ---- 确定性轮换 ----
    @Test
    fun sameInputsAreReproducible() {
        val pack = Words.of(Words.P6)
        val a = QuizPicker.pick(pack, 0, allLearned, 4)
        val b = QuizPicker.pick(pack, 0, allLearned, 4)
        assertEquals(a, b)
    }

    @Test
    fun seedRotationActuallyChangesDistractor() {
        val pack = Words.of(Words.P6)
        val distinct = (0..10).map { seed ->
            distractorOf(QuizPicker.pick(pack, 0, allLearned, seed)).id
        }.toSet()
        assertTrue("seed 变化未带来干扰项轮换：$distinct", distinct.size > 1)
    }

    @Test
    fun candidatesAreSortedByShapeGapThenId() {
        val pack = Words.of(Words.P6)
        val target = pack.words[0]
        val list = QuizPicker.candidates(pack, target, allLearned)
        val rank = QuizPicker.GROUP_RANK.getValue(target.group)
        val gaps = list.map { kotlin.math.abs(QuizPicker.GROUP_RANK.getValue(it.group) - rank) }
        assertEquals("候选未按体型差降序", gaps.sortedDescending(), gaps)
        for (i in 0 until list.size - 1) {
            if (gaps[i] == gaps[i + 1]) {
                assertTrue("同体型差时应按 id 升序", list[i].id <= list[i + 1].id)
            }
        }
    }
}
