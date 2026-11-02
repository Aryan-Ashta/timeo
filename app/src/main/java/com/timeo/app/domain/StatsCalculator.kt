package com.timeo.app.domain

import com.timeo.app.data.Category
import com.timeo.app.data.Session
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class Interval(val start: Long, val end: Long) {
    val length: Long get() = end - start
}

data class Streaks(val current: Int, val best: Int)

/** Pure time math: rollups, midnight splitting, streaks and heatmap levels. */
object StatsCalculator {

    /** The category itself plus its subtimers, so parents roll up their children's time. */
    fun rollupIds(categoryId: Long, categories: List<Category>): Set<Long> =
        buildSet {
            add(categoryId)
            categories.filter { it.parentId == categoryId }.forEach { add(it.id) }
        }

    /** Sessions for [ids] as closed intervals; running sessions end at [nowMs]. */
    fun intervals(sessions: List<Session>, ids: Set<Long>, nowMs: Long): List<Interval> =
        sessions.asSequence()
            .filter { it.categoryId in ids }
            .map { Interval(it.startEpochMs, it.endEpochMs ?: nowMs) }
            .filter { it.end > it.start }
            .toList()

    /** Merges overlapping intervals so simultaneous parent + child time is counted once. */
    fun merge(intervals: List<Interval>): List<Interval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.start }
        val out = ArrayList<Interval>()
        var cur = sorted.first()
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            cur = if (next.start <= cur.end) {
                Interval(cur.start, maxOf(cur.end, next.end))
            } else {
                out += cur
                next
            }
        }
        out += cur
        return out
    }

    /** Splits intervals at local midnights and sums milliseconds per day. */
    fun dailyTotals(intervals: List<Interval>, zone: ZoneId): Map<LocalDate, Long> {
        val totals = HashMap<LocalDate, Long>()
        for (iv in intervals) {
            var start = iv.start
            while (start < iv.end) {
                val day = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
                val nextMidnight = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = minOf(iv.end, nextMidnight)
                totals.merge(day, end - start, Long::plus)
                start = end
            }
        }
        return totals
    }

    /** Daily totals for one category including its subtimers, with overlaps merged. */
    fun categoryDaily(
        categoryId: Long,
        categories: List<Category>,
        sessions: List<Session>,
        nowMs: Long,
        zone: ZoneId,
    ): Map<LocalDate, Long> =
        dailyTotals(merge(intervals(sessions, rollupIds(categoryId, categories), nowMs)), zone)

    /** Daily totals across everything: the sum of each top-level category's rollup. */
    fun overallDaily(
        categories: List<Category>,
        sessions: List<Session>,
        nowMs: Long,
        zone: ZoneId,
    ): Map<LocalDate, Long> {
        val totals = HashMap<LocalDate, Long>()
        categories.filter { it.parentId == null }.forEach { top ->
            categoryDaily(top.id, categories, sessions, nowMs, zone).forEach { (d, ms) ->
                totals.merge(d, ms, Long::plus)
            }
        }
        return totals
    }

    fun sumBetween(daily: Map<LocalDate, Long>, from: LocalDate, toInclusive: LocalDate): Long =
        daily.entries.sumOf { (d, ms) -> if (d in from..toInclusive) ms else 0L }

    fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDay))

    /**
     * A day counts when it meets [goalMs] (or has any time when there is no goal).
     * Today not being met yet does not break the current streak.
     */
    fun streaks(daily: Map<LocalDate, Long>, goalMs: Long?, today: LocalDate): Streaks {
        fun met(d: LocalDate): Boolean {
            val ms = daily[d] ?: 0L
            return if (goalMs != null && goalMs > 0) ms >= goalMs else ms > 0
        }
        var current = 0
        var d = if (met(today)) today else today.minusDays(1)
        while (met(d)) {
            current++
            d = d.minusDays(1)
        }
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (day in daily.keys.filter { it <= today && met(it) }.sorted()) {
            run = if (prev != null && prev.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            prev = day
        }
        return Streaks(current, maxOf(best, current))
    }

    /** 90th percentile of active days, so heatmap shading adapts to how much time is usually logged. */
    fun typicalDay(values: Collection<Long>): Long {
        val active = values.filter { it > 0 }.sorted()
        if (active.isEmpty()) return 60 * 60_000L
        return maxOf(active[((active.size - 1) * 0.9).toInt()], 30 * 60_000L)
    }

    /** Heatmap intensity 0..4: by share of the daily goal, or by time buckets without one. */
    fun heatLevel(ms: Long, goalMs: Long?): Int {
        if (ms <= 0) return 0
        if (goalMs != null && goalMs > 0) {
            val r = ms.toDouble() / goalMs
            return when {
                r >= 1.0 -> 4
                r >= 0.66 -> 3
                r >= 0.33 -> 2
                else -> 1
            }
        }
        val min = ms / 60_000
        return when {
            min >= 120 -> 4
            min >= 60 -> 3
            min >= 30 -> 2
            else -> 1
        }
    }
}
