package com.guguabc.kids.state

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.guguabc.kids.data.Words
import kotlinx.coroutines.flow.first

/**
 * design_v2.md §7：V1 的 soundEnabled / todayDate / usedSeconds 三个 key 完全保留，
 * 新增 key 缺省时回落到基准课 + 空已学集合，旧数据不得被清空。
 * 学习进度只存本地，无账号、无上传、无埋点（§7.5）。
 */
private val Context.guguDataStore: DataStore<Preferences> by preferencesDataStore(name = "gugu_prefs")

data class GuguPrefs(
    val soundEnabled: Boolean = true,
    val todayDate: String = "",
    val usedSeconds: Int = 0,
    val currentPackId: String = Words.DEFAULT_PACK_ID,
    val learnedWordIds: Set<String> = emptySet(),
)

object Prefs {
    private val SOUND = booleanPreferencesKey("sound_enabled")
    private val DATE = stringPreferencesKey("today_date")
    private val USED = intPreferencesKey("used_seconds")
    private val PACK = stringPreferencesKey("current_pack_id")
    private val LEARNED = stringSetPreferencesKey("learned_word_ids")

    suspend fun read(context: Context): GuguPrefs = context.guguDataStore.data.first().let { p ->
        GuguPrefs(
            soundEnabled = p[SOUND] ?: true,
            todayDate = p[DATE] ?: "",
            usedSeconds = p[USED] ?: 0,
            currentPackId = p[PACK] ?: Words.DEFAULT_PACK_ID,
            learnedWordIds = p[LEARNED]?.toSet() ?: emptySet(),
        )
    }

    suspend fun write(context: Context, value: GuguPrefs) {
        context.guguDataStore.edit { p ->
            p[SOUND] = value.soundEnabled
            p[DATE] = value.todayDate
            p[USED] = value.usedSeconds
            p[PACK] = value.currentPackId
            p[LEARNED] = value.learnedWordIds.toSet()
        }
    }
}
