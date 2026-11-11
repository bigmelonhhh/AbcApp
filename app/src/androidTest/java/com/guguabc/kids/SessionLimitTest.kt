package com.guguabc.kids

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guguabc.kids.data.Words
import com.guguabc.kids.state.GuguPrefs
import com.guguabc.kids.state.GuguRoute
import com.guguabc.kids.state.GuguSession
import com.guguabc.kids.state.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** 使用真实 DataStore 恢复额度，再从公开入口尝试开始学习。 */
@RunWith(AndroidJUnit4::class)
class SessionLimitTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var saved: GuguPrefs
    private lateinit var session: GuguSession

    @Before
    fun savePreferences() = runBlocking {
        saved = Prefs.read(context)
    }

    @After
    fun restorePreferences() = runBlocking {
        withContext(Dispatchers.Main) {
            if (::session.isInitialized) session.release()
            scope.cancel()
        }
        Prefs.write(context, saved)
    }

    @Test
    fun coldStartAtDailyLimitCannotEnterLesson() = runBlocking {
        loadSession(usedSeconds = 300)
        withContext(Dispatchers.Main) {
            session.start()
            assertEquals(GuguRoute.Done, session.route)
            assertEquals(300, session.usedSeconds)
        }
    }

    @Test
    fun selectingAnotherPackAtLimitDoesNotGrantMoreTime() = runBlocking {
        loadSession(usedSeconds = 300)
        withContext(Dispatchers.Main) {
            session.selectPack(Words.P2)
            assertEquals(GuguRoute.Home, session.route)
            session.start()
            assertEquals(GuguRoute.Done, session.route)
            assertEquals(Words.P2, session.currentPackId)
            session.replay()
            assertEquals(GuguRoute.Done, session.route)
            assertEquals(300, session.usedSeconds)
            assertFalse(session.replayAllowed)
        }
    }

    @Test
    fun parentResetAllowsLearningWithoutClearingLearnedWordsOrCourse() = runBlocking {
        loadSession(usedSeconds = 300, learned = setOf("cat", "dog", "bear"))
        withContext(Dispatchers.Main) {
            session.selectPack(Words.P2)
            session.start()
            session.resetTodayTime()
            assertTrue(session.replayAllowed)
            session.replay()
            assertEquals(GuguRoute.Card(0, Words.P2), session.route)
            assertEquals(0, session.usedSeconds)
            assertEquals(setOf("cat", "dog", "bear"), session.learnedWordIds)
        }
    }

    @Test
    fun lastSecondEndsLearningButStillAllowsStickerBook() = runBlocking {
        loadSession(usedSeconds = 299)
        withContext(Dispatchers.Main) {
            session.start()
            assertEquals(GuguRoute.Card(0, Words.P1), session.route)
            session.tick()
            assertEquals(GuguRoute.Done, session.route)
            assertEquals(300, session.usedSeconds)
            session.openStickerBook()
            repeat(12) { session.tick() }
            assertEquals(GuguRoute.StickerBook, session.route)
            session.back()
            assertEquals(GuguRoute.Done, session.route)
            session.start()
            assertEquals(GuguRoute.Done, session.route)
        }
    }

    @Test
    fun yesterdayLimitDoesNotBlockTodaysLesson() = runBlocking {
        loadSession(usedSeconds = 300, date = LocalDate.now().minusDays(1).toString())
        withContext(Dispatchers.Main) {
            session.start()
            assertEquals(GuguRoute.Card(0, Words.P1), session.route)
            assertEquals(0, session.usedSeconds)
        }
    }

    @Test
    fun actionsBeforeRestoreCannotOverwriteOrBypassStoredLimit() = runBlocking {
        Prefs.write(context, GuguPrefs(
            todayDate = LocalDate.now().toString(), usedSeconds = 300,
            currentPackId = Words.P2, soundEnabled = false, learnedWordIds = setOf("cat"),
        ))
        // 同一个主线程任务内点击，保证 DataStore 恢复协程尚未执行。
        withContext(Dispatchers.Main) {
            session = GuguSession(context, scope)
            assertFalse(session.isReady)
            session.start()
            session.replay()
            session.startPack(Words.P9)
            session.tick()
            session.selectPack(Words.P3)
            session.toggleSound()
            session.resetTodayTime()
            assertEquals(GuguRoute.Home, session.route)
        }
        awaitReady()
        withContext(Dispatchers.Main) {
            assertEquals(300, session.usedSeconds)
            assertEquals(Words.P2, session.currentPackId)
            assertEquals(setOf("cat"), session.learnedWordIds)
            assertFalse(session.soundEnabled)
            session.start()
            assertEquals(GuguRoute.Done, session.route)
        }
    }

    @Test
    fun newCategoryEntryCannotBypassDailyLimit() = runBlocking {
        loadSession(usedSeconds = 300)
        withContext(Dispatchers.Main) {
            com.guguabc.kids.data.LearningCategory.entries.forEach { category ->
                session.openCategory(category)
                repeat(3) { session.tick() }
                assertTrue(session.route is GuguRoute.Lessons)
                session.startPack(Words.inCategory(category).first().id)
                assertEquals(GuguRoute.Done, session.route)
                assertEquals(300, session.usedSeconds)
            }
        }
    }

    @Test
    fun browsingDoesNotConsumeTimeAndBackRestoresLessonPage() = runBlocking {
        loadSession(usedSeconds = 20)
        withContext(Dispatchers.Main) {
            session.openCategory(com.guguabc.kids.data.LearningCategory.ANIMALS)
            session.pageLessons(1)
            repeat(10) { session.tick() }
            assertEquals(20, session.usedSeconds)
            session.startPack(Words.P8)
            session.back()
            assertEquals(GuguRoute.Lessons(com.guguabc.kids.data.LearningCategory.ANIMALS, 1), session.route)
            session.back()
            assertEquals(GuguRoute.Home, session.route)
        }
    }

    @Test
    fun allNewLessonsUnlockTheirWordsWithoutErasingAnimals() = runBlocking {
        loadSession(usedSeconds = 0, learned = setOf("cat"))
        withContext(Dispatchers.Main) {
            val newPacks = Words.packs.drop(8)
            newPacks.forEach { pack ->
                session.startPack(pack.id)
                repeat(3) { session.nextCard() }
                pack.words.forEach { word ->
                    val quiz = session.route as GuguRoute.Quiz
                    assertEquals(word, session.quizFor(quiz).target)
                    session.pick(word)
                    session.continueFromCheer()
                }
                assertEquals(GuguRoute.Done, session.route)
                session.chooseAnotherLesson()
                assertEquals(GuguRoute.Lessons(pack.category), session.route)
            }
            assertEquals(setOf("cat") + newPacks.flatMap { it.words }.map { it.id }, session.learnedWordIds)
        }
    }

    private suspend fun loadSession(
        usedSeconds: Int,
        date: String = LocalDate.now().toString(),
        learned: Set<String> = emptySet(),
    ) {
        Prefs.write(context, GuguPrefs(todayDate = date, usedSeconds = usedSeconds, learnedWordIds = learned))
        withContext(Dispatchers.Main) { session = GuguSession(context, scope) }
        awaitReady()
    }

    private suspend fun awaitReady() = withTimeout(5_000) {
        while (withContext(Dispatchers.Main) { !session.isReady }) delay(10)
    }
}
