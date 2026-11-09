package com.guguabc.kids.data

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.guguabc.kids.R

/** design.md §7.2：V1 只有 3 个词，不新增词、不新增主题包。 */
data class Word(
    val id: String,
    val en: String,
    val zh: String,
    @DrawableRes val imageRes: Int,
    @RawRes val audioRes: Int,
)

object Words {
    val all: List<Word> = listOf(
        Word("cat", "Cat", "小猫", R.drawable.img_cat, R.raw.word_cat),
        Word("dog", "Dog", "小狗", R.drawable.img_dog, R.raw.word_dog),
        Word("bear", "Bear", "小熊", R.drawable.img_bear, R.raw.word_bear),
    )
}
