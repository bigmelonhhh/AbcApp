package com.guguabc.kids.state

import androidx.activity.ComponentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.guguabc.kids.data.Words
import com.guguabc.kids.screens.CheerScreen
import com.guguabc.kids.screens.DoneScreen
import com.guguabc.kids.screens.HomeScreen
import com.guguabc.kids.screens.LessonSelectionScreen
import com.guguabc.kids.screens.ListenQuizScreen
import com.guguabc.kids.screens.StickerBookScreen
import com.guguabc.kids.screens.WordCardScreen
import kotlinx.coroutines.delay

/** design.md §7.1 + design_v2.md §4：五屏闭环 + 贴纸册，200ms 淡入淡出，禁止滑动翻页手势。 */
@Composable
fun GuguNavHost() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val session = remember { GuguSession(context.applicationContext, scope) }
    GuguNavHost(session, activity?.lifecycle, onRest = { activity?.finish() })
}

/** 与生产入口共用页面和副作用，允许测试驱动真实生命周期及设备音频边界。 */
@Composable
internal fun GuguNavHost(session: GuguSession, lifecycle: Lifecycle?, onRest: () -> Unit) {

    // 每日时长计时（跨天清零由 SessionState.tick 处理）；后台不计时长
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true)
    }
    LaunchedEffect(session) {
        while (true) {
            delay(1_000L)
            if (foreground) session.tick()
        }
    }

    // 后台取消整段自动播报（含 10 秒重播）；回前台从当前页重新开始。
    LaunchedEffect(session, session.route, foreground, session.isReady) {
        if (!foreground || !session.isReady) return@LaunchedEffect
        when (val r = session.route) {
            is GuguRoute.Card -> session.playWord(Words.of(r.packId).words[r.index])
            is GuguRoute.Quiz -> {
                val word = Words.of(r.packId).words[r.index]
                session.playWord(word)
                delay(10_000L)
                if (foreground && session.route == r) session.playWord(word)
            }
            is GuguRoute.Cheer -> session.playCheer()
            else -> Unit
        }
    }

    // CHEER 的「再玩一个」额外延迟 1.2s 才可点（防宝宝连点误跳）
    var canContinue by remember { mutableStateOf(false) }
    LaunchedEffect(session.route) {
        canContinue = false
        if (session.route is GuguRoute.Cheer) {
            delay(1_200L)
            canContinue = true
        }
    }

    // App 退到后台立即停止播放；销毁时释放资源
    DisposableEffect(session, lifecycle) {
        foreground = lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
        if (foreground) session.onAppStart() else session.onAppStop()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    session.onAppStart()
                    foreground = true
                }
                Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    session.onAppStop()
                }
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            session.release()
        }
    }

    Box(Modifier) {
        Crossfade(
            targetState = session.route,
            animationSpec = tween(durationMillis = 200),
            label = "GuguRoute",
        ) { route ->
            when (route) {
                GuguRoute.Home -> HomeScreen(
                    onCategory = session::openCategory,
                    onStickerBook = session::openStickerBook,
                    onParentGate = session::openParentPanel,
                )

                is GuguRoute.Lessons -> LessonSelectionScreen(
                    category = route.category,
                    page = route.page,
                    onSelect = session::startPack,
                    onPage = session::pageLessons,
                    onBack = session::back,
                    onParentGate = session::openParentPanel,
                )

                is GuguRoute.Card -> {
                    val word = Words.of(route.packId).words[route.index]
                    WordCardScreen(
                        word = word,
                        cardIndex = route.index,
                        soundEnabled = session.soundEnabled,
                        onSound = { session.playWord(word) },
                        onNext = session::nextCard,
                        onBack = session::back,
                        onToggleSound = session::toggleSound,
                    )
                }

                is GuguRoute.Quiz -> {
                    val data = session.quizFor(route)
                    ListenQuizScreen(
                        targetWord = data.target,
                        optionA = data.optionA,
                        optionB = data.optionB,
                        quizIndex = route.index,
                        soundEnabled = session.soundEnabled,
                        onSound = { session.playWord(data.target) },
                        onPick = session::pick,
                        onBack = session::back,
                        onToggleSound = session::toggleSound,
                    )
                }

                is GuguRoute.Cheer -> CheerScreen(
                    word = route.word,
                    quizNumber = route.index + 1,
                    canContinue = canContinue,
                    onContinue = session::continueFromCheer,
                    onBack = session::back,
                )

                GuguRoute.Done -> DoneScreen(
                    learnedWords = session.currentPack.words,
                    minutesUsed = session.minutesUsed,
                    replayEnabled = session.replayAllowed,
                    onReplay = session::replay,
                    onRest = onRest,
                    onParentGate = session::openParentPanel,
                    onStickerBook = session::openStickerBook,
                    onChooseLesson = session::chooseAnotherLesson,
                )

                GuguRoute.StickerBook -> StickerBookScreen(
                    words = Words.all,
                    learnedIds = session.learnedWordIds,
                    initialCategory = session.currentPack.category,
                    onPlay = session::playWord,
                    onBack = session::back,
                )
            }
        }

        if (session.parentPanelVisible) {
            ParentPanel(
                session = session,
                onClose = session::closeParentPanel,
            )
        }
    }
}
