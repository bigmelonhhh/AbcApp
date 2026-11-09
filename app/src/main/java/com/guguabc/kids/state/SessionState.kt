package com.guguabc.kids.state

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.guguabc.kids.R
import com.guguabc.kids.audio.AudioPlayer
import com.guguabc.kids.data.Word
import com.guguabc.kids.data.Words
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

const val TIME_LIMIT_SECONDS = 5 * 60   // design.md §7.5 每日 5 分钟

data class QuizData(val target: Word, val optionA: Word, val optionB: Word)

/**
 * design.md §7：状态机、词表、无失败态、防连点（在 guguClick）、时长上限。
 * 用 remember 持有，不引入 ViewModel / Navigation 依赖。
 */
class GuguSession(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    var route: GuguRoute by mutableStateOf(GuguRoute.Home)
        private set
    var soundEnabled: Boolean by mutableStateOf(true)
        private set
    var usedSeconds: Int by mutableStateOf(0)
        private set
    var todayDate: String by mutableStateOf("")
        private set
    var parentPanelVisible: Boolean by mutableStateOf(false)
        private set

    private val player = AudioPlayer(context)
    private val history = ArrayDeque<GuguRoute>()
    private var persistTick = 0

    val minutesUsed: Int get() = ceil(usedSeconds / 60f).toInt()
    val replayAllowed: Boolean get() = usedSeconds < TIME_LIMIT_SECONDS

    init {
        scope.launch {
            val stored = Prefs.read(context)
            val today = LocalDate.now().toString()
            if (stored.todayDate != today) {
                todayDate = today
                usedSeconds = 0
            } else {
                todayDate = stored.todayDate
                usedSeconds = stored.usedSeconds
            }
            soundEnabled = stored.soundEnabled
            player.setEnabled(stored.soundEnabled)
            persist()
        }
    }

    // ---- 流程 ----
    fun start() = navigate(GuguRoute.Card(0))

    fun nextCard() {
        val r = route as? GuguRoute.Card ?: return
        navigate(
            if (r.index < Words.all.lastIndex) GuguRoute.Card(r.index + 1)
            else GuguRoute.Quiz(0)
        )
    }

    fun quizFor(index: Int): QuizData {
        val target = Words.all[index]
        val other = Words.all[(index + 1) % Words.all.size]
        // 目标词左右轮换，避免宝宝记住固定位置；两图都不带任何文字
        return if (index % 2 == 0) QuizData(target, target, other)
        else QuizData(target, other, target)
    }

    /** 无失败态：选错只重播发音，不变色、不扣分、不锁按钮、不记录错误。 */
    fun pick(word: Word) {
        val r = route as? GuguRoute.Quiz ?: return
        val target = Words.all[r.index]
        if (word.id == target.id) navigate(GuguRoute.Cheer(r.index, target))
        else playWord(target)
    }

    fun continueFromCheer() {
        val r = route as? GuguRoute.Cheer ?: return
        navigate(
            if (r.index < Words.all.lastIndex) GuguRoute.Quiz(r.index + 1)
            else GuguRoute.Done
        )
    }

    fun replay() {
        if (!replayAllowed) return
        history.clear()
        navigate(GuguRoute.Card(0))
    }

    fun back() {
        when (route) {
            // CHEER 的返回不回退到答题中途，避免「再来一次骗时长」
            is GuguRoute.Cheer -> {
                history.clear()
                route = GuguRoute.Home
            }
            else -> route = if (history.isNotEmpty()) history.removeLast() else GuguRoute.Home
        }
        player.stop()
    }

    private fun navigate(next: GuguRoute) {
        history.addLast(route)
        route = next
        player.stop()   // 切页释放资源
    }

    // ---- 音频 ----
    fun playWord(word: Word) = player.play(word.audioRes)
    fun playCheer() = player.play(R.raw.cheer)
    fun onAppStop() = player.stop()
    fun release() = player.release()

    // ---- 音效开关 / 家长门 ----
    fun toggleSound() {
        soundEnabled = !soundEnabled
        player.setEnabled(soundEnabled)
        persist()
    }

    fun openParentPanel() {
        parentPanelVisible = true
        player.play(R.raw.tap)
    }

    fun closeParentPanel() { parentPanelVisible = false }

    // ---- 时长 ----
    fun tick() {
        val today = LocalDate.now().toString()
        if (today != todayDate) {
            todayDate = today
            usedSeconds = 0
        }
        usedSeconds++
        persistTick++
        if (persistTick >= 5) {
            persistTick = 0
            persist()
        }
        if (usedSeconds >= TIME_LIMIT_SECONDS && route !is GuguRoute.Done) {
            history.clear()
            route = GuguRoute.Done
            player.stop()
        }
    }

    private fun persist() {
        scope.launch {
            Prefs.write(context, GuguPrefs(soundEnabled, todayDate, usedSeconds))
        }
    }
}
