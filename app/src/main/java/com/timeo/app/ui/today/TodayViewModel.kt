package com.timeo.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timeo.app.data.Category
import com.timeo.app.data.Repository
import com.timeo.app.data.Session
import com.timeo.app.domain.StatsCalculator
import com.timeo.app.ui.theme.PlateArgb
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class TimerItem(
    val category: Category,
    val number: Int,
    val todayMs: Long,
    val weekMs: Long,
    val runningSelf: Boolean,
    /** Running itself or through one of its subtimers. */
    val active: Boolean,
    /** Elapsed time of the newest running session (own or a subtimer's). */
    val liveMs: Long,
    val children: List<TimerItem> = emptyList(),
) {
    val dailyProgress: Float
        get() = category.dailyGoalMinutes?.takeIf { it > 0 }
            ?.let { (todayMs / (it * 60_000f)).coerceIn(0f, 1f) } ?: 0f
}

data class TodayState(
    val loaded: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val totalMs: Long = 0,
    val items: List<TimerItem> = emptyList(),
    val parents: List<Category> = emptyList(),
)

class TodayViewModel(private val repo: Repository) : ViewModel() {
    private val zone = ZoneId.systemDefault()

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000 - System.currentTimeMillis() % 1000)
        }
    }

    // A week back always covers the current week whatever day it starts on.
    private val sessions = repo.sessionsBetween(
        LocalDate.now(zone).minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli(),
        Long.MAX_VALUE,
    )

    val state: StateFlow<TodayState> =
        combine(repo.categories, sessions, repo.weekStart, ticker) { cats, sess, weekStart, now ->
            build(cats, sess, weekStart, now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    private fun build(cats: List<Category>, sess: List<Session>, weekStart: DayOfWeek, now: Long): TodayState {
        val today = LocalDate.now(zone)
        val from = StatsCalculator.weekStart(today, weekStart)
        val running = sess.filter { it.isRunning }

        fun item(cat: Category, number: Int, children: List<TimerItem>): TimerItem {
            val daily = StatsCalculator.categoryDaily(cat.id, cats, sess, now, zone)
            val ids = StatsCalculator.rollupIds(cat.id, cats)
            val live = running.filter { it.categoryId in ids }.maxByOrNull { it.startEpochMs }
            return TimerItem(
                category = cat,
                number = number,
                todayMs = daily[today] ?: 0,
                weekMs = StatsCalculator.sumBetween(daily, from, today),
                runningSelf = running.any { it.categoryId == cat.id },
                active = live != null,
                liveMs = live?.let { now - it.startEpochMs } ?: 0,
                children = children,
            )
        }

        val tops = cats.filter { it.parentId == null }
        val items = tops.mapIndexed { i, top ->
            val kids = cats.filter { it.parentId == top.id }.mapIndexed { j, c -> item(c, j + 1, emptyList()) }
            item(top, i + 1, kids)
        }
        val total = StatsCalculator.overallDaily(cats, sess, now, zone)[today] ?: 0
        return TodayState(true, today, total, items, tops)
    }

    fun toggle(categoryId: Long) = viewModelScope.launch { repo.toggle(categoryId) }

    fun seed() = viewModelScope.launch { repo.seedSampleData(PlateArgb) }
}
