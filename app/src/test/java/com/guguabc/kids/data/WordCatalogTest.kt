package com.guguabc.kids.data

import com.guguabc.kids.state.GuguRoute
import com.guguabc.kids.state.QuizPicker
import com.guguabc.kids.state.TIME_LIMIT_SECONDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 词库 / 课包 / 路由的运行时契约。
 * verify_project.py 用正则比对 Kotlin 源码与 word_packs.json，属于静态检查；
 * 这里从真实对象出发再锁一遍结构不变量，防止「源码文本对、运行时结构错」。
 */
class WordCatalogTest {
    @Test
    fun fourCategoriesHaveRealLessonsAndMatchingHeroImages() {
        assertEquals(listOf(8, 2, 2, 2), LearningCategory.entries.map { Words.inCategory(it).size })
        LearningCategory.entries.forEach { category ->
            val ids = Words.inCategory(category).flatMap { it.words }.map { it.id }.toSet()
            assertTrue(ids.containsAll(category.heroIds))
        }
    }

    @Test
    fun catalogIs42WordsIn14PacksOf3() {
        assertEquals(42, Words.all.size)
        assertEquals(14, Words.packs.size)
        Words.packs.forEach { assertEquals("${it.id} 词数不是 3", 3, it.words.size) }
        assertEquals(Words.all, Words.packs.flatMap { it.words })
    }

    @Test
    fun packsAreNumberedAndLeveled() {
        Words.packs.forEachIndexed { i, pack ->
            assertEquals("第 ${i + 1} 课课号", i + 1, pack.no)
            assertEquals(if (i in 6..7) "L2" else "L1", pack.level)
            assertTrue(pack.title.isNotBlank())
        }
    }

    @Test
    fun everyWordBelongsToItsOwnPack() {
        Words.packs.forEach { pack ->
            pack.words.forEach { w ->
                assertEquals("${w.id} 的 packId", pack.id, w.packId)
                assertEquals("${w.id} 的 level", pack.level, w.level)
            }
        }
    }

    @Test
    fun baselinePackIsFrozenToV1CatDogBear() {
        assertEquals(Words.P1, Words.DEFAULT_PACK_ID)
        assertEquals(listOf("cat", "dog", "bear"), Words.of(Words.DEFAULT_PACK_ID).words.map { it.id })
        assertEquals("身边的小伙伴", Words.of(Words.P1).title)
    }

    @Test
    fun wordsHaveUniqueIdsNamesMediaAndKnownGroups() {
        assertEquals(Words.all.size, Words.all.map { it.id }.toSet().size)
        assertEquals(Words.all.size, Words.all.map { it.en }.toSet().size)
        Words.all.forEach { w ->
            assertTrue("${w.id} 英文首字母应大写", w.en.first().isUpperCase())
            assertEquals("${w.id} 英文名应全小写（除首字母）", w.en.lowercase().replaceFirstChar { it.uppercase() }, w.en)
            assertTrue("${w.id} 中文名为空", w.zh.isNotBlank())
            assertTrue("${w.id} 未配 groupRank", QuizPicker.GROUP_RANK.containsKey(w.group))
            assertTrue("${w.id} 缺插画资源", w.imageRes != 0)
            assertTrue("${w.id} 缺发音资源", w.audioRes != 0)
        }
    }

    @Test
    fun everyLessonHasAtLeastTwoShapeGroups() {
        // 若某课三词同 group，二选一干扰项会退化成课内轮换
        Words.packs.forEach { pack ->
            assertTrue(
                "${pack.id} 三词 group 全相同，干扰项规则会退化",
                pack.words.map { it.group }.toSet().size >= 2,
            )
        }
    }

    @Test
    fun lookupsAreSafeForUnknownIds() {
        assertEquals(Words.packs.first(), Words.of("not_exist"))
        assertEquals(Words.packs.first(), Words.of(""))
        assertEquals("cat", Words.byId("cat")?.id)
        assertNull(Words.byId("not_exist"))
    }

    @Test
    fun routesCarryPackIdToPreventCrossLessonMixing() {
        val word = Words.of(Words.P6).words[0]
        val a = GuguRoute.Card(0, Words.P1)
        val b = GuguRoute.Card(0, Words.P6)
        assertNotEquals("Card 缺 packId 会串词", a, b)
        assertNotEquals(GuguRoute.Quiz(0, Words.P1), GuguRoute.Quiz(0, Words.P6))
        assertNotEquals(
            GuguRoute.Cheer(0, word, Words.P1),
            GuguRoute.Cheer(0, word, Words.P6),
        )
        assertEquals(Words.P6, (b as GuguRoute.Card).packId)
    }

    @Test
    fun timeLimitIsFiveMinutes() {
        assertEquals(300, TIME_LIMIT_SECONDS)
        assertFalse(299 >= TIME_LIMIT_SECONDS)
        assertTrue(300 >= TIME_LIMIT_SECONDS)
    }
}
