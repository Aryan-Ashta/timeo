package com.timeo.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

private val Context.settingsStore by preferencesDataStore("settings")

class Repository(private val dao: TimeoDao, private val context: Context) {

    val categories: Flow<List<Category>> = dao.observeCategories()
    val running: Flow<List<Session>> = dao.observeRunning()
    val allSessions: Flow<List<Session>> = dao.observeAllSessions()

    fun sessionsBetween(fromMs: Long, toMs: Long): Flow<List<Session>> =
        dao.observeSessionsBetween(fromMs, toMs)

    suspend fun getCategory(id: Long): Category? = dao.getCategory(id)

    suspend fun saveCategory(category: Category): Long =
        if (category.id == 0L) {
            dao.insertCategory(category.copy(sortOrder = dao.nextSortOrder()))
        } else {
            dao.updateCategory(category)
            category.id
        }

    suspend fun deleteCategory(category: Category, deleteChildren: Boolean) =
        dao.deleteCategoryWithChildren(category, deleteChildren)

    /** Starts the stopwatch for [categoryId], or stops it if it is already running. */
    suspend fun toggle(categoryId: Long, nowMs: Long = System.currentTimeMillis()) {
        val open = dao.runningFor(categoryId)
        if (open != null) {
            dao.updateSession(open.copy(endEpochMs = nowMs))
        } else {
            dao.insertSession(Session(categoryId = categoryId, startEpochMs = nowMs))
        }
    }

    suspend fun saveSession(session: Session) {
        if (session.id == 0L) dao.insertSession(session) else dao.updateSession(session)
    }

    suspend fun deleteSession(session: Session) = dao.deleteSession(session)

    private val weekStartKey = stringPreferencesKey("week_start")

    val weekStart: Flow<DayOfWeek> = context.settingsStore.data.map { prefs ->
        prefs[weekStartKey]?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() } ?: DayOfWeek.MONDAY
    }

    suspend fun setWeekStart(day: DayOfWeek) {
        context.settingsStore.edit { it[weekStartKey] = day.name }
    }

    /** Debug helper: a few categories with ~6 months of plausible history. */
    suspend fun seedSampleData(palette: List<Int>) {
        val zone = ZoneId.systemDefault()
        val coding = saveCategory(Category(name = "Coding", colorArgb = palette[1], iconKey = "code", dailyGoalMinutes = 120, weeklyGoalMinutes = 600))
        val timeo = saveCategory(Category(parentId = coding, name = "Timeo", colorArgb = palette[1], iconKey = "code", dailyGoalMinutes = 45))
        val school = saveCategory(Category(parentId = coding, name = "Coursework", colorArgb = palette[1], iconKey = "school"))
        val reading = saveCategory(Category(name = "Reading", colorArgb = palette[0], iconKey = "book", dailyGoalMinutes = 30, weeklyGoalMinutes = 180))
        val gym = saveCategory(Category(name = "Gym", colorArgb = palette[2], iconKey = "fitness", dailyGoalMinutes = 60, weeklyGoalMinutes = 240))
        val music = saveCategory(Category(name = "Guitar", colorArgb = palette[3], iconKey = "music", dailyGoalMinutes = 20))
        val rnd = Random(7)
        val today = LocalDate.now(zone)
        val sessions = mutableListOf<Session>()
        fun add(cat: Long, day: LocalDate, hour: Int, minutes: Int) {
            val start = day.atTime(hour, rnd.nextInt(0, 50)).atZone(zone).toInstant().toEpochMilli()
            sessions += Session(categoryId = cat, startEpochMs = start, endEpochMs = start + minutes * 60_000L)
        }
        for (back in 180 downTo 1) {
            val day = today.minusDays(back.toLong())
            val weekend = day.dayOfWeek.value >= 6
            if (rnd.nextFloat() < 0.8f) add(listOf(coding, timeo, school).random(rnd), day, 9, rnd.nextInt(20, 150))
            if (!weekend && rnd.nextFloat() < 0.6f) add(school, day, 14, rnd.nextInt(30, 90))
            if (rnd.nextFloat() < 0.5f) add(timeo, day, 20, rnd.nextInt(15, 70))
            if (rnd.nextFloat() < 0.65f) add(reading, day, 22, rnd.nextInt(10, 50))
            if (rnd.nextFloat() < (if (weekend) 0.3f else 0.55f)) add(gym, day, 7, rnd.nextInt(40, 80))
            if (rnd.nextFloat() < 0.35f) add(music, day, 18, rnd.nextInt(10, 35))
        }
        add(reading, today, 7, 20)
        dao.insertSessions(sessions)
    }
}
