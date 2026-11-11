package com.guguabc.kids.state

import com.guguabc.kids.data.Word
import com.guguabc.kids.data.WordPack
import com.guguabc.kids.data.Words
import kotlin.math.abs

/** design_v2.md §3：二选一题面。两图都不带任何文字。 */
data class QuizData(val target: Word, val optionA: Word, val optionB: Word)

/**
 * design_v2.md §3 · 干扰项规则（纯 Kotlin，零 Android 依赖）。
 *
 * 从 GuguSession 抽出的原因：GuguSession 构造依赖 Context / MediaPlayer / DataStore，
 * 无法在 JVM 单元测试里跑；而干扰项规则是 V2 最核心、组合最多的业务逻辑，
 * 必须能被 8 课 × 3 词 × 多 seed 的用例锁死。GuguSession.quizFor() 只做参数转发，
 * UI 与 V1 五张像素基准图零改动。
 */
object QuizPicker {

    /** 体型 / 姿态差异序：数字间隔越大，轮廓差异越大（design_v2.md §3.2）。 */
    val GROUP_RANK: Map<String, Int> = mapOf(
        "aquatic" to 0, "tiny" to 1, "bird" to 2, "primate" to 3,
        "feline" to 4, "canine" to 5, "slender" to 6, "chunky" to 7, "giant" to 8,
        "round_fruit" to 0, "long_fruit" to 5, "cluster_fruit" to 2,
        "berry" to 3, "melon" to 7,
        "small_vehicle" to 0, "long_vehicle" to 3, "cycle" to 7,
        "rail" to 1, "aircraft" to 5, "watercraft" to 8,
        "drinkware" to 0, "cutlery" to 7, "tableware" to 3,
        "brush" to 8, "cloth" to 2, "footwear" to 5,
    )

    /** design_v2.md §3.4 / word_packs.json distractors.forbiddenPairs。 */
    val FORBIDDEN_PAIRS = setOf(
        setOf("lion", "tiger"), setOf("cat", "lion"), setOf("dog", "fox"),
        setOf("duck", "bird"), setOf("horse", "zebra"), setOf("turtle", "frog"),
    )

    /**
     * 候选池：本课三词 + 历史已学词，去重后排除同 group 与禁用组合，
     * 按 |groupRank 差| 降序 + id 升序排列（体型/姿态差异最大者优先）。
     * 纯函数，不在重组中写任何状态。
     */
    fun candidates(pack: WordPack, target: Word, learnedIds: Set<String>): List<Word> {
        val learned = learnedIds.mapNotNull(Words::byId)
            .filter { Words.of(it.packId).category == pack.category }
        val pool = (pack.words + learned).distinctBy { it.id }
        val rank = GROUP_RANK[target.group] ?: 0
        return pool.filter { w ->
            w.id != target.id
                && w.group != target.group
                && FORBIDDEN_PAIRS.none { it == setOf(w.id, target.id) }
        }.sortedWith(
            compareByDescending<Word> { abs((GROUP_RANK[it.group] ?: 0) - rank) }
                .thenBy { it.id }
        )
    }

    /**
     * 基准课（第 1 课）保持 V1 的课内轮换结果不变，确保 5 张像素基准可复现；
     * 其余 7 课走跨 group 规则，用 (index + pack.no + seed) 确定性轮换。
     * 目标词按 index 奇偶左右轮换，避免宝宝记住固定位置。
     */
    fun pick(pack: WordPack, index: Int, learnedIds: Set<String>, seed: Int): QuizData {
        val words = pack.words
        val target = words[index]
        val other: Word = if (pack.id == Words.DEFAULT_PACK_ID) {
            words[(index + 1) % words.size]
        } else {
            val pool = candidates(pack, target, learnedIds)
            if (pool.isEmpty()) words[(index + 1) % words.size]
            else pool[Math.floorMod(index + pack.no + seed, pool.size)]
        }
        return if (index % 2 == 0) QuizData(target, target, other)
        else QuizData(target, other, target)
    }
}
