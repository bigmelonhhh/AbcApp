package com.guguabc.kids.data

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.guguabc.kids.R

/**
 * design_v2.md §2 / §7：24 词 / 8 课。
 * 机器可读真值源为 app/tools/word_packs.json；本表由该 JSON 生成，
 * 禁止手改词序、id、group、packId —— verify_project.py 会逐项比对。
 */
data class Word(
    val id: String,
    val en: String,
    val zh: String,
    val packId: String,
    val level: String,
    val group: String,
    @DrawableRes val imageRes: Int,
    @RawRes val audioRes: Int,
)

data class WordPack(
    val id: String,
    val no: Int,
    val title: String,
    val level: String,
    val words: List<Word>,
)

object Words {

    /** 第 1 课是 V1 像素基准课，文案与流程保持 V1 原样。 */
    const val DEFAULT_PACK_ID = "p1_friends"

    const val P1 = "p1_friends"
    const val P2 = "p2_farm"
    const val P3 = "p3_meadow"
    const val P4 = "p4_pond"
    const val P5 = "p5_forest"
    const val P6 = "p6_zoo"
    const val P7 = "p7_big"
    const val P8 = "p8_special"

    val all: List<Word> = listOf(
        Word("cat", "Cat", "小猫", P1, "L1", "feline", R.drawable.img_cat, R.raw.word_cat),
        Word("dog", "Dog", "小狗", P1, "L1", "canine", R.drawable.img_dog, R.raw.word_dog),
        Word("bear", "Bear", "小熊", P1, "L1", "chunky", R.drawable.img_bear, R.raw.word_bear),
        Word("pig", "Pig", "小猪", P2, "L1", "chunky", R.drawable.img_pig, R.raw.word_pig),
        Word("cow", "Cow", "奶牛", P2, "L1", "chunky", R.drawable.img_cow, R.raw.word_cow),
        Word("duck", "Duck", "小鸭", P2, "L1", "bird", R.drawable.img_duck, R.raw.word_duck),
        Word("rabbit", "Rabbit", "兔子", P3, "L1", "tiny", R.drawable.img_rabbit, R.raw.word_rabbit),
        Word("sheep", "Sheep", "绵羊", P3, "L1", "chunky", R.drawable.img_sheep, R.raw.word_sheep),
        Word("horse", "Horse", "马", P3, "L1", "slender", R.drawable.img_horse, R.raw.word_horse),
        Word("fish", "Fish", "小鱼", P4, "L1", "aquatic", R.drawable.img_fish, R.raw.word_fish),
        Word("frog", "Frog", "青蛙", P4, "L1", "aquatic", R.drawable.img_frog, R.raw.word_frog),
        Word("bird", "Bird", "小鸟", P4, "L1", "bird", R.drawable.img_bird, R.raw.word_bird),
        Word("fox", "Fox", "狐狸", P5, "L1", "canine", R.drawable.img_fox, R.raw.word_fox),
        Word("monkey", "Monkey", "猴子", P5, "L1", "primate", R.drawable.img_monkey, R.raw.word_monkey),
        Word("deer", "Deer", "鹿", P5, "L1", "slender", R.drawable.img_deer, R.raw.word_deer),
        Word("lion", "Lion", "狮子", P6, "L1", "feline", R.drawable.img_lion, R.raw.word_lion),
        Word("tiger", "Tiger", "老虎", P6, "L1", "feline", R.drawable.img_tiger, R.raw.word_tiger),
        Word("panda", "Panda", "熊猫", P6, "L1", "chunky", R.drawable.img_panda, R.raw.word_panda),
        Word("elephant", "Elephant", "大象", P7, "L2", "giant", R.drawable.img_elephant, R.raw.word_elephant),
        Word("giraffe", "Giraffe", "长颈鹿", P7, "L2", "slender", R.drawable.img_giraffe, R.raw.word_giraffe),
        Word("zebra", "Zebra", "斑马", P7, "L2", "slender", R.drawable.img_zebra, R.raw.word_zebra),
        Word("owl", "Owl", "猫头鹰", P8, "L2", "bird", R.drawable.img_owl, R.raw.word_owl),
        Word("turtle", "Turtle", "乌龟", P8, "L2", "aquatic", R.drawable.img_turtle, R.raw.word_turtle),
        Word("butterfly", "Butterfly", "蝴蝶", P8, "L2", "tiny", R.drawable.img_butterfly, R.raw.word_butterfly),
    )

    val packs: List<WordPack> = listOf(
        WordPack(P1, 1, "身边的小伙伴", "L1", all.slice(0..2)),
        WordPack(P2, 2, "农场里", "L1", all.slice(3..5)),
        WordPack(P3, 3, "草地上", "L1", all.slice(6..8)),
        WordPack(P4, 4, "池塘边", "L1", all.slice(9..11)),
        WordPack(P5, 5, "树林里", "L1", all.slice(12..14)),
        WordPack(P6, 6, "动物园", "L1", all.slice(15..17)),
        WordPack(P7, 7, "大个子朋友", "L2", all.slice(18..20)),
        WordPack(P8, 8, "特别的朋友", "L2", all.slice(21..23)),
    )

    /** 未知 / 旧版 id 一律回落到基准课，绝不返回空词表。 */
    fun of(packId: String): WordPack = packs.firstOrNull { it.id == packId } ?: packs.first()

    fun byId(id: String): Word? = all.firstOrNull { it.id == id }
}
