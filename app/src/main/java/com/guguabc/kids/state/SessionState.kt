package com.guguabc.kids.state

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.guguabc.kids.R
import com.guguabc.kids.audio.AudioPlayer
import com.guguabc.kids.data.Word
import com.guguabc.kids.data.WordPack
import com.guguabc.kids.data.Words
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * design.md §7 + design_v2.md §7：状态机、词表、无失败态、防连点（在 guguClick）、时长上限。
 * 用 remember 持有，不引入 ViewModel / Navigation 依赖。
 *
 * 2026-11-09 测试体系补齐：干扰项规则抽到 QuizPicker、时长口径抽到 TimeBudget，
 * 两者都是零 Android 依赖的纯逻辑，由 JVM 单元测试直接执行验证。本类只负责
 * 把纯逻辑结果同步到 Compose state / 路由 / 音频 / DataStore，行为与 UI 零改动。
 */
const val TIME_LIMIT_SECONDS = 5 * 60

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
    var currentPackId: String by mutableStateOf(Words.DEFAULT_PACK_ID)
        private set
    var learnedWordIds: Set<String> by mutableStateOf(emptySet())
        private set

    private val player = AudioPlayer(context)
    private val budget = TimeBudget()
    private val history = ArrayDeque<GuguRoute>()
    /** 每次开新一课 / 再玩一次时 +1，让干扰项确定性轮换，避免连续两次同题同干扰项。 */
    private var sessionSeed = 0

    /** 只在认一认 / 听一听 / 答对啦累计「学习态时长」。 */
    private val isLearning: Boolean
        get() = route is GuguRoute.Card || route is GuguRoute.Quiz || route is GuguRoute.Cheer

    val currentPack: WordPack get() = Words.of(currentPackId)
    val minutesUsed: Int get() = budget.minutesUsed
    val replayAllowed: Boolean get() = budget.replayAllowed
    val isTimeUp: Boolean get() = budget.isTimeUp

    init {
        scope.launch {
            val stored = Prefs.read(context)
            budget.restore(stored.usedSeconds, stored.todayDate, LocalDate.now().toString())
            usedSeconds = budget.seconds
            todayDate = budget.date
            soundEnabled = stored.soundEnabled
            currentPackId = Words.of(stored.currentPackId).id
            learnedWordIds = stored.learnedWordIds
            player.setEnabled(stored.soundEnabled)
            persist()
        }
    }

    // ---- 流程 ----
    fun start() {
        sessionSeed++
        navigate(GuguRoute.Card(0, currentPackId))
    }

    fun nextCard() {
        val r = route as? GuguRoute.Card ?: return
        val words = Words.of(r.packId).words
        navigate(
            if (r.index < words.lastIndex) GuguRoute.Card(r.index + 1, r.packId)
            else GuguRoute.Quiz(0, r.packId)
        )
    }

    /** design_v2.md §3：规则全部委托给纯函数 QuizPicker，保证基准课像素基准可复现。 */
    fun quizFor(route: GuguRoute.Quiz): QuizData =
        QuizPicker.pick(Words.of(route.packId), route.index, learnedWordIds, sessionSeed)

    /** 无失败态：选错只重播发音，不变色、不扣分、不锁按钮、不记录错误。 */
    fun pick(word: Word) {
        val r = route as? GuguRoute.Quiz ?: return
        val target = Words.of(r.packId).words[r.index]
        if (word.id == target.id) navigate(GuguRoute.Cheer(r.index, target, r.packId))
        else playWord(target)
    }

    fun continueFromCheer() {
        val r = route as? GuguRoute.Cheer ?: return
        val words = Words.of(r.packId).words
        if (r.index < words.lastIndex) {
            navigate(GuguRoute.Quiz(r.index + 1, r.packId))
        } else {
            markLearned(r.packId)
            navigate(GuguRoute.Done)
        }
    }

    /** 只由「完成某课 QUIZ」写入，点贴纸册不改变进度。 */
    private fun markLearned(packId: String) {
        learnedWordIds = learnedWordIds + Words.of(packId).words.map { it.id }
        persist()
    }

    fun replay() {
        // UI 层在禁用态把点击分流到「今天的 5 分钟用完啦」提示，不会走到这里；
        // 此处保留一道防御，避免任何路径绕过每日时长上限。
        if (!replayAllowed) return
        sessionSeed++
        history.clear()
        navigate(GuguRoute.Card(0, currentPackId))
    }

    fun openStickerBook() = navigate(GuguRoute.StickerBook)

    /** design_v2.md §4：选课权只在家长侧；选完直接回首页，不新增宝宝侧导航元素。 */
    fun selectPack(packId: String) {
        if (Words.packs.none { it.id == packId }) return
        currentPackId = packId
        sessionSeed++
        parentPanelVisible = false
        history.clear()
        route = GuguRoute.Home
        player.stop()
        persist()
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
    /**
     * design_v2.md 附录①②④：只在学习态计时，到 300s 停止累加并只跳一次 DONE。
     * 非学习页（Home / Done / StickerBook / 家长面板）既不累加也不被强制跳转，
     * 贴纸册因此可以停留、可以返回，家长选课后也能正常停在首页。
     */
    fun tick() {
        val result = budget.tick(isLearning, LocalDate.now().toString())
        usedSeconds = budget.seconds
        todayDate = budget.date
        if (result.shouldPersist) persist()
        if (result.timeUp) {
            history.clear()
            route = GuguRoute.Done
            player.stop()
        }
    }

    /**
     * 家长面板「重置今日时长」：只清时长，不动已学贴纸 / 当前课程 / 音效开关。
     * design_v2.md 附录⑤；跨天清零逻辑保持原样。
     */
    fun resetTodayTime() {
        budget.reset(LocalDate.now().toString())
        usedSeconds = budget.seconds
        todayDate = budget.date
        persist()
    }

    private fun persist() {
        scope.launch {
            Prefs.write(
                context,
                GuguPrefs(
                    soundEnabled = soundEnabled,
                    todayDate = budget.date,
                    usedSeconds = budget.seconds,
                    currentPackId = currentPackId,
                    learnedWordIds = learnedWordIds,
                ),
            )
        }
    }
}
