package com.guguabc.kids.state

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** design.md §7.5：音效开关、todayDate / usedSeconds，跨天清零。 */
private val Context.guguDataStore: DataStore<Preferences> by preferencesDataStore(name = "gugu_prefs")

data class GuguPrefs(
    val soundEnabled: Boolean = true,
    val todayDate: String = "",
    val usedSeconds: Int = 0,
)

object Prefs {
    private val SOUND = booleanPreferencesKey("sound_enabled")
    private val DATE = stringPreferencesKey("today_date")
    private val USED = intPreferencesKey("used_seconds")

    suspend fun read(context: Context): GuguPrefs = context.guguDataStore.data.first().let { p ->
        GuguPrefs(
            soundEnabled = p[SOUND] ?: true,
            todayDate = p[DATE] ?: "",
            usedSeconds = p[USED] ?: 0,
        )
    }

    suspend fun write(context: Context, value: GuguPrefs) {
        context.guguDataStore.edit { p ->
            p[SOUND] = value.soundEnabled
            p[DATE] = value.todayDate
            p[USED] = value.usedSeconds
        }
    }
}
