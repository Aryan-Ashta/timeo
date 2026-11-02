package com.timeo.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

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
}
