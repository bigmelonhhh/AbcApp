package com.guguabc.kids

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.guguabc.kids.audio.AudioOutput
import com.guguabc.kids.data.Words
import com.guguabc.kids.state.GuguNavHost
import com.guguabc.kids.state.GuguPrefs
import com.guguabc.kids.state.GuguSession
import com.guguabc.kids.state.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** 运行真实 NavHost 副作用与生命周期，仅替换 Android 音频设备输出。 */
class SessionAudioLifecycleTest {
    @get:Rule val rule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audio = RecordingAudio()
    private lateinit var saved: GuguPrefs
    private lateinit var session: GuguSession
    private lateinit var owner: TestOwner

    private class TestOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }

    private class RecordingAudio : AudioOutput {
        val played = mutableListOf<Int>()
        var current: Int? = null
        private var enabled = true
        override fun setEnabled(value: Boolean) { enabled = value; if (!value) stop() }
        override fun play(resId: Int) {
            if (enabled) { played.add(resId); current = resId }
        }
        override fun stop() { current = null }
        override fun release() = stop()
    }

    @Before
    fun setUp() {
        runBlocking {
            saved = Prefs.read(context)
            Prefs.write(context, GuguPrefs())
        }
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread {
            owner = TestOwner()
            session = GuguSession(context, scope, audio)
        }
        rule.waitUntil(5_000) { session.isReady }
    }

    private fun mount(state: Lifecycle.State = Lifecycle.State.RESUMED) {
        rule.runOnUiThread { owner.lifecycle.currentState = state }
        rule.setGuguScreen { GuguNavHost(session, owner.lifecycle, onRest = {}) }
        advance(100)
    }

    @After
    fun tearDown() {
        rule.runOnUiThread {
            if (::owner.isInitialized) owner.lifecycle.currentState = Lifecycle.State.DESTROYED
            if (::session.isInitialized) session.release()
            scope.cancel()
        }
        runBlocking { Prefs.write(context, saved) }
    }

    private fun advance(milliseconds: Long) {
        rule.mainClock.advanceTimeBy(milliseconds)
        rule.waitForIdle()
    }

    private fun enterQuiz() {
        mount()
        rule.runOnIdle {
            session.start()
            repeat(3) { session.nextCard() }
        }
        advance(300)
        rule.runOnIdle { assertEquals(listOf(R.raw.word_cat), audio.played) }
    }

    @Test
    fun delayedQuizReplayIsCancelledWhileStopped() {
        enterQuiz()
        advance(5_000)
        rule.runOnIdle {
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            assertNull(audio.current)
        }
        advance(11_000)
        rule.runOnIdle {
            assertEquals(listOf(R.raw.word_cat), audio.played)
            assertNull(audio.current)
        }
    }

    @Test
    fun resumePlaysCurrentQuizAndStartsAFreshReminder() {
        enterQuiz()
        advance(5_000)
        rule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        advance(11_000)
        rule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        advance(300)
        rule.runOnIdle { assertEquals(listOf(R.raw.word_cat, R.raw.word_cat), audio.played) }
        advance(9_000)
        rule.runOnIdle { assertEquals(2, audio.played.size) }
        advance(1_500)
        rule.runOnIdle { assertEquals(listOf(R.raw.word_cat, R.raw.word_cat, R.raw.word_cat), audio.played) }
        advance(12_000)
        rule.runOnIdle { assertEquals(3, audio.played.size) }
    }

    @Test
    fun stoppedSessionRejectsPlaybackBeforeCompositionCanCancelEffects() {
        enterQuiz()
        rule.runOnIdle {
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            session.playWord(Words.all[1])
            session.playCheer()
            session.openParentPanel()
            assertEquals(listOf(R.raw.word_cat), audio.played)
            assertNull(audio.current)
        }
    }

    @Test
    fun initiallyStoppedHostWaitsForStartBeforePlayingOrCountingTime() {
        rule.runOnUiThread {
            session.start()
            repeat(3) { session.nextCard() }
        }
        mount(Lifecycle.State.CREATED)
        advance(11_000)
        rule.runOnIdle {
            assertEquals(emptyList<Int>(), audio.played)
            assertEquals(0, session.usedSeconds)
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        advance(300)
        rule.runOnIdle { assertEquals(listOf(R.raw.word_cat), audio.played) }
    }

    @Test
    fun changingRouteCancelsThePreviousQuizReminder() {
        enterQuiz()
        advance(5_000)
        rule.runOnIdle { session.back() }
        advance(300)
        advance(11_000)
        rule.runOnIdle { assertEquals(listOf(R.raw.word_cat, R.raw.word_bear), audio.played) }
    }

    @Test
    fun mutedAudioStaysSilentWhenResuming() {
        enterQuiz()
        rule.runOnIdle {
            session.toggleSound()
            owner.lifecycle.currentState = Lifecycle.State.CREATED
        }
        advance(11_000)
        rule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        advance(11_000)
        rule.runOnIdle {
            assertEquals(listOf(R.raw.word_cat), audio.played)
            assertNull(audio.current)
        }
    }

    @Test
    fun newCategoriesPlayTheirOwnAudioAndRemainSilentInBackground() {
        mount()
        listOf(Words.P9, Words.P11, Words.P13).forEach { packId ->
            rule.runOnIdle { session.startPack(packId) }
            advance(300)
            val expected = Words.of(packId).words.first().audioRes
            rule.runOnIdle {
                assertEquals(expected, audio.current)
                owner.lifecycle.currentState = Lifecycle.State.CREATED
            }
            val count = audio.played.size
            advance(11_000)
            rule.runOnIdle {
                session.playWord(Words.of(packId).words.first())
                assertEquals(count, audio.played.size)
                assertNull(audio.current)
                owner.lifecycle.currentState = Lifecycle.State.RESUMED
            }
            advance(300)
            rule.runOnIdle { assertEquals(expected, audio.current) }
        }
    }
}
