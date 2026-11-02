package com.timeo.app.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll
import com.timeo.app.data.Category
import com.timeo.app.data.Repository
import com.timeo.app.data.Session
import com.timeo.app.domain.StatsCalculator
import com.timeo.app.ui.theme.Ink
import com.timeo.app.ui.theme.Paper
import com.timeo.app.ui.theme.Rule
import com.timeo.app.ui.theme.Slate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class WidgetRow(
    val id: Long,
    val name: String,
    val colorArgb: Int,
    val isChild: Boolean,
    val todayMs: Long,
    val runningSelf: Boolean,
    /** Running itself or through a subtimer, so its total is ticking. */
    val active: Boolean,
    val goalProgress: Int?,
)

data class TimersSnapshot(val nowMs: Long, val totalMs: Long, val anyActive: Boolean, val rows: List<WidgetRow>)

data class HeatmapSnapshot(
    val today: LocalDate,
    val weekStart: DayOfWeek,
    val daily: Map<LocalDate, Long>,
    val scaleMs: Long,
    val streak: Int,
    val todayMs: Long,
)

object WidgetData {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun timers(repo: Repository): Flow<TimersSnapshot> {
        val from = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        return combine(repo.categories, repo.sessionsBetween(from, Long.MAX_VALUE)) { cats, sessions ->
            buildTimers(cats, sessions, System.currentTimeMillis())
        }
    }

    fun heatmap(repo: Repository): Flow<HeatmapSnapshot> =
        combine(repo.categories, repo.allSessions, repo.weekStart) { cats, sessions, weekStart ->
            val now = System.currentTimeMillis()
            val today = LocalDate.now(zone)
            val daily = StatsCalculator.overallDaily(cats, sessions, now, zone)
            HeatmapSnapshot(
                today = today,
                weekStart = weekStart,
                daily = daily,
                scaleMs = StatsCalculator.typicalDay(daily.values),
                streak = StatsCalculator.streaks(daily, null, today).current,
                todayMs = daily[today] ?: 0,
            )
        }

    private fun buildTimers(cats: List<Category>, sessions: List<Session>, now: Long): TimersSnapshot {
        val today = LocalDate.now(zone)
        val running = sessions.filter { it.isRunning }.map { it.categoryId }.toSet()
        fun row(c: Category): WidgetRow {
            val ids = StatsCalculator.rollupIds(c.id, cats)
            val ms = StatsCalculator.categoryDaily(c.id, cats, sessions, now, zone)[today] ?: 0
            return WidgetRow(
                id = c.id,
                name = c.name,
                colorArgb = c.colorArgb,
                isChild = c.parentId != null,
                todayMs = ms,
                runningSelf = c.id in running,
                active = ids.any { it in running },
                goalProgress = c.dailyGoalMinutes?.takeIf { it > 0 }?.let { (ms * 100 / (it * 60_000L)).toInt() },
            )
        }
        val rows = cats.filter { it.parentId == null }.flatMap { top ->
            listOf(row(top)) + cats.filter { it.parentId == top.id }.map(::row)
        }
        return TimersSnapshot(
            nowMs = now,
            totalMs = StatsCalculator.overallDaily(cats, sessions, now, zone)[today] ?: 0,
            anyActive = running.isNotEmpty(),
            rows = rows,
        )
    }

    suspend fun refreshAll(context: Context) {
        TimersWidget().updateAll(context)
        HeatmapWidget().updateAll(context)
    }
}

/** The app palette resolved for the launcher's light or dark mode. */
data class WidgetColors(val background: Color, val text: Color, val muted: Color, val rule: Color) {
    companion object {
        fun of(context: Context): WidgetColors {
            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            return if (night) {
                WidgetColors(Ink, Paper, Color(0xFF9AA1AE), Color(0xFF323A4C))
            } else {
                WidgetColors(Paper, Ink, Slate, Rule)
            }
        }
    }
}
