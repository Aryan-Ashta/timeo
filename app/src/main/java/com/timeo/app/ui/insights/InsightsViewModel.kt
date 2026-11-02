package com.timeo.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timeo.app.data.Category
import com.timeo.app.data.Repository
import com.timeo.app.domain.StatsCalculator
import com.timeo.app.domain.Streaks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class InsightsState(
    val loaded: Boolean = false,
    val categories: List<Category> = emptyList(),
    /** null means everything. */
    val selected: Category? = null,
    val today: LocalDate = LocalDate.now(),
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val daily: Map<LocalDate, Long> = emptyMap(),
    val dailyGoalMs: Long? = null,
    /** What a full heatmap cell means: the daily goal, or a typical busy day without one. */
    val heatScaleMs: Long = 60 * 60_000L,
    val weeklyGoalMs: Long? = null,
    val streaks: Streaks = Streaks(0, 0),
    val weekMs: Long = 0,
    val allTimeMs: Long = 0,
    val activeDays: Int = 0,
)

class InsightsViewModel(private val repo: Repository) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val selectedId = MutableStateFlow<Long?>(null)

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    val state: StateFlow<InsightsState> =
        combine(repo.categories, repo.allSessions, selectedId, repo.weekStart, ticker) { cats, sessions, selId, weekStart, now ->
            val today = LocalDate.now(zone)
            val selected = cats.firstOrNull { it.id == selId }
            val daily = if (selected == null) {
                StatsCalculator.overallDaily(cats, sessions, now, zone)
            } else {
                StatsCalculator.categoryDaily(selected.id, cats, sessions, now, zone)
            }
            val dailyGoal = selected?.dailyGoalMinutes?.let { it * 60_000L }
            InsightsState(
                loaded = true,
                categories = cats,
                selected = selected,
                today = today,
                weekStart = weekStart,
                daily = daily,
                dailyGoalMs = dailyGoal,
                heatScaleMs = dailyGoal ?: StatsCalculator.typicalDay(daily.values),
                weeklyGoalMs = selected?.weeklyGoalMinutes?.let { it * 60_000L },
                streaks = StatsCalculator.streaks(daily, dailyGoal, today),
                weekMs = StatsCalculator.sumBetween(daily, StatsCalculator.weekStart(today, weekStart), today),
                allTimeMs = daily.values.sum(),
                activeDays = daily.count { it.value > 0 },
            )
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())

    fun select(id: Long?) {
        selectedId.value = id
    }

    fun setWeekStart(day: DayOfWeek) = viewModelScope.launch { repo.setWeekStart(day) }
}
