package com.timeo.app.domain

import com.timeo.app.data.Category
import com.timeo.app.data.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsCalculatorTest {
    private val zone = ZoneId.of("America/Chicago")
    private val min = 60_000L
    private val hour = 60 * min

    private fun at(y: Int, m: Int, d: Int, h: Int, mi: Int = 0) =
        LocalDateTime.of(y, m, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun splitsSessionAcrossMidnight() {
        val totals = StatsCalculator.dailyTotals(
            listOf(Interval(at(2026, 3, 1, 23), at(2026, 3, 2, 1, 30))),
            zone,
        )
        assertEquals(hour, totals[LocalDate.of(2026, 3, 1)])
        assertEquals(hour + 30 * min, totals[LocalDate.of(2026, 3, 2)])
    }

    @Test
    fun runningSessionCountsUntilNow() {
        val sessions = listOf(Session(categoryId = 1, startEpochMs = at(2026, 3, 1, 9)))
        val ivs = StatsCalculator.intervals(sessions, setOf(1L), at(2026, 3, 1, 9, 45))
        assertEquals(45 * min, ivs.single().length)
    }

    @Test
    fun parentRollsUpChildrenWithoutDoubleCounting() {
        val cats = listOf(
            Category(id = 1, name = "Coding", colorArgb = 0),
            Category(id = 2, parentId = 1, name = "Timeo", colorArgb = 0),
            Category(id = 3, parentId = 1, name = "School", colorArgb = 0),
            Category(id = 4, name = "Gym", colorArgb = 0),
        )
        val sessions = listOf(
            Session(categoryId = 1, startEpochMs = at(2026, 3, 1, 9), endEpochMs = at(2026, 3, 1, 11)),
            // Overlaps the parent by 1h.
            Session(categoryId = 2, startEpochMs = at(2026, 3, 1, 10), endEpochMs = at(2026, 3, 1, 12)),
            Session(categoryId = 3, startEpochMs = at(2026, 3, 1, 14), endEpochMs = at(2026, 3, 1, 15)),
            Session(categoryId = 4, startEpochMs = at(2026, 3, 1, 9), endEpochMs = at(2026, 3, 1, 10)),
        )
        val day = LocalDate.of(2026, 3, 1)
        val now = at(2026, 3, 2, 0)
        assertEquals(4 * hour, StatsCalculator.categoryDaily(1, cats, sessions, now, zone)[day])
        assertEquals(2 * hour, StatsCalculator.categoryDaily(2, cats, sessions, now, zone)[day])
        assertEquals(5 * hour, StatsCalculator.overallDaily(cats, sessions, now, zone)[day])
    }

    @Test
    fun streaksIgnoreUnfinishedToday() {
        val today = LocalDate.of(2026, 3, 10)
        val goal = 30 * min
        val daily = mapOf(
            today.minusDays(1) to goal,
            today.minusDays(2) to goal + 1,
            today.minusDays(3) to 10 * min, // missed
            today.minusDays(4) to goal,
            today.minusDays(5) to goal,
            today.minusDays(6) to goal,
            today to 5 * min,
        )
        val s = StatsCalculator.streaks(daily, goal, today)
        assertEquals(2, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun heatLevels() {
        assertEquals(0, StatsCalculator.heatLevel(0, 30 * min))
        assertEquals(1, StatsCalculator.heatLevel(5 * min, 30 * min))
        assertEquals(3, StatsCalculator.heatLevel(25 * min, 30 * min))
        assertEquals(4, StatsCalculator.heatLevel(45 * min, 30 * min))
        assertEquals(2, StatsCalculator.heatLevel(40 * min, null))
        assertEquals(4, StatsCalculator.heatLevel(3 * hour, null))
    }

    @Test
    fun weekStartRespectsFirstDay() {
        val wed = LocalDate.of(2026, 11, 4)
        assertEquals(LocalDate.of(2026, 11, 2), StatsCalculator.weekStart(wed, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 11, 1), StatsCalculator.weekStart(wed, DayOfWeek.SUNDAY))
    }
}
