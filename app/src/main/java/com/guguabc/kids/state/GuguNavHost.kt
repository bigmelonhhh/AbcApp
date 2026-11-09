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
import com.guguabc.kids.screens.ListenQuizScreen
import com.guguabc.kids.screens.StickerBookScreen
import com.guguabc.kids.screens.WordCardScreen
import kotlinx.coroutines.delay

/** design.md §7.1 + design_v2.md §4：五屏闭环 + 贴纸册，200ms 淡入淡出，禁止滑动翻页手势。 */
@Composable
fun GuguNavHost() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycle = activity?.lifecycle
    val scope = rememberCoroutineScope()
    val session = remember { GuguSession(context.applicationContext, scope) }

    // 每日时长计时（跨天清零由 SessionState.tick 处理）；后台不计时长
    var foreground by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            if (foreground) session.tick()
        }
    }

    // 进 CARD / QUIZ 自动播报一次；QUIZ 10 秒无操作重播一次；进 CHEER 播放庆祝音
    LaunchedEffect(session.route) {
        when (val r = session.route) {
            is GuguRoute.Card -> session.playWord(Words.of(r.packId).words[r.index])
            is GuguRoute.Quiz -> {
                val word = Words.of(r.packId).words[r.index]
                session.playWord(word)
                delay(10_000L)
                if (session.route == r) session.playWord(word)
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
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> foreground = true
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
                    pack = session.currentPack,
                    onStart = session::start,
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
                    onRest = { activity?.finish() },
                    onParentGate = session::openParentPanel,
                    onStickerBook = session::openStickerBook,
                )

                GuguRoute.StickerBook -> StickerBookScreen(
                    words = Words.all,
                    learnedIds = session.learnedWordIds,
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
