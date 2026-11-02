package com.timeo.app.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timeo.app.data.Category
import com.timeo.app.ui.components.SectionLabel
import com.timeo.app.ui.components.formatDuration
import com.timeo.app.ui.theme.LocalTimeo
import com.timeo.app.ui.theme.TimeoType
import com.timeo.app.ui.theme.onPlate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(
    state: InsightsState,
    onSelectCategory: (Long?) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onWeekStart: (DayOfWeek) -> Unit,
    contentPadding: PaddingValues,
) {
    val c = LocalTimeo.current
    val accent by animateColorAsState(state.selected?.let { Color(it.colorArgb) } ?: c.text, label = "accent")
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val day = selectedDay ?: state.today

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        Filters(state, onSelectCategory)

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(state.selected?.name ?: "Everything", style = TimeoType.display, color = c.text)
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatDuration(state.allTimeMs)} across ${state.activeDays} days",
                style = TimeoType.body, color = c.muted,
            )
            Spacer(Modifier.height(28.dp))
            SectionLabel(1, "Last 12 months")
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.padding(start = 20.dp)) {
            Heatmap(
                daily = state.daily,
                scaleMs = state.heatScaleMs,
                color = accent,
                today = state.today,
                weekStart = state.weekStart,
                selected = selectedDay,
                onSelect = { selectedDay = it },
            )
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))
            Legend(accent, state.dailyGoalMs != null)
            Spacer(Modifier.height(14.dp))
            DayRow(day, state.daily[day] ?: 0, state.dailyGoalMs, onOpen = { onOpenDay(day) })

            Spacer(Modifier.height(32.dp))
            SectionLabel(2, "Goals and streaks")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(
                    "Current streak", "${state.streaks.current}d",
                    if (state.dailyGoalMs != null) "days meeting goal" else "days tracked",
                    Modifier.weight(1f),
                )
                Tile("Best streak", "${state.streaks.best}d", "all time", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            WeekTile(state.weekMs, state.weeklyGoalMs, accent)

            Spacer(Modifier.height(32.dp))
            SectionLabel(3, "Last 7 days")
            Spacer(Modifier.height(16.dp))
            WeekBars(state.daily, state.today, state.dailyGoalMs, accent)

            Spacer(Modifier.height(36.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Week starts on", style = TimeoType.label, color = c.muted, modifier = Modifier.weight(1f))
                listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY).forEach { d ->
                    val on = d == state.weekStart
                    Text(
                        d.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = TimeoType.labelStrong,
                        color = if (on) c.background else c.text,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(if (on) c.text else Color.Transparent)
                            .border(1.dp, if (on) c.text else c.rule)
                            .clickable { onWeekStart(d) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Filters(state: InsightsState, onSelect: (Long?) -> Unit) {
    val tops = state.categories.filter { it.parentId == null }
    val selectedTop = state.selected?.let { s -> if (s.parentId == null) s else tops.firstOrNull { it.id == s.parentId } }
    val children = selectedTop?.let { t -> state.categories.filter { it.parentId == t.id } }.orEmpty()
    Column {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("All", null, state.selected == null) { onSelect(null) }
            tops.forEach { cat -> FilterChip(cat.name, cat, selectedTop?.id == cat.id) { onSelect(cat.id) } }
        }
        AnimatedVisibility(children.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("↳", style = TimeoType.body, color = LocalTimeo.current.muted)
                children.forEach { child ->
                    FilterChip(child.name, child, state.selected?.id == child.id, small = true) {
                        onSelect(if (state.selected?.id == child.id) selectedTop?.id else child.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, cat: Category?, selected: Boolean, small: Boolean = false, onClick: () -> Unit) {
    val c = LocalTimeo.current
    val swatch = cat?.let { Color(it.colorArgb) } ?: c.text
    val bg by animateColorAsState(if (selected) swatch else Color.Transparent, label = "chip")
    val fg = when {
        !selected -> c.text
        cat != null -> onPlate(swatch)
        else -> c.background
    }
    Row(
        Modifier
            .background(bg)
            .border(1.dp, if (selected) swatch else c.rule)
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 14.dp, vertical = if (small) 6.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cat != null && !selected) {
            Box(Modifier.size(8.dp).background(swatch))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = if (small) TimeoType.label else TimeoType.labelStrong.copy(fontSize = TimeoType.nav.fontSize * 0.9f), color = fg)
    }
}

@Composable
private fun Legend(color: Color, byGoal: Boolean) {
    val c = LocalTimeo.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (byGoal) "Shaded by share of daily goal" else "Relative to a typical day", style = TimeoType.label, color = c.muted, modifier = Modifier.weight(1f))
        Text("Less", style = TimeoType.label, color = c.muted)
        Spacer(Modifier.width(6.dp))
        (0..4).forEach { l ->
            Box(Modifier.padding(horizontal = 1.5.dp).size(11.dp).background(heatColor(l, color, c.rule)))
        }
        Spacer(Modifier.width(6.dp))
        Text("More", style = TimeoType.label, color = c.muted)
    }
}

@Composable
private fun DayRow(day: LocalDate, ms: Long, goalMs: Long?, onOpen: () -> Unit) {
    val c = LocalTimeo.current
    Row(
        Modifier.fillMaxWidth().border(1.dp, c.rule).clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(day.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy")), style = TimeoType.bodyStrong, color = c.text)
            Text(
                buildString {
                    append(formatDuration(ms))
                    if (goalMs != null) append("  ·  ${(ms * 100 / goalMs)}% of goal")
                },
                style = TimeoType.label, color = c.muted,
            )
        }
        Text("Sessions", style = TimeoType.label, color = c.muted)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = c.text, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Tile(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    val c = LocalTimeo.current
    Column(modifier.border(1.dp, c.rule).padding(14.dp)) {
        Text(label, style = TimeoType.label, color = c.muted)
        Spacer(Modifier.height(14.dp))
        Text(value, style = TimeoType.title.copy(fontSize = TimeoType.display.fontSize * 0.7f), color = c.text)
        Text(caption, style = TimeoType.label, color = c.muted)
    }
}

@Composable
private fun WeekTile(weekMs: Long, goalMs: Long?, color: Color) {
    val c = LocalTimeo.current
    val p by animateFloatAsState(goalMs?.let { (weekMs.toFloat() / it).coerceIn(0f, 1f) } ?: 0f, tween(700), label = "wk")
    Column(Modifier.fillMaxWidth().border(1.dp, c.rule).padding(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("This week", style = TimeoType.label, color = c.muted, modifier = Modifier.weight(1f))
            if (goalMs != null) Text("${(weekMs * 100 / goalMs)}%", style = TimeoType.monoSmall, color = c.text)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatDuration(weekMs), style = TimeoType.title.copy(fontSize = TimeoType.display.fontSize * 0.7f), color = c.text)
            if (goalMs != null) {
                Spacer(Modifier.width(8.dp))
                Text("of ${formatDuration(goalMs)} goal", style = TimeoType.label, color = c.muted, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        if (goalMs != null) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).background(c.rule)) {
                Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(color))
            }
        } else {
            Text("Set a weekly goal on a timer to track it here", style = TimeoType.label, color = c.muted)
        }
    }
}

@Composable
private fun WeekBars(daily: Map<LocalDate, Long>, today: LocalDate, goalMs: Long?, color: Color) {
    val c = LocalTimeo.current
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { daily[it] ?: 0L }
    val max = maxOf(values.maxOrNull() ?: 0L, goalMs ?: 0L, 60 * 60_000L).toFloat()
    val grow by animateFloatAsState(1f, tween(700), label = "bars")
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { v ->
                Text(
                    if (v > 0) formatDuration(v) else "",
                    style = TimeoType.label.copy(fontSize = TimeoType.label.fontSize * 0.85f),
                    color = c.muted, textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val gap = 8.dp.toPx()
            val w = (size.width - gap * 6) / 7
            values.forEachIndexed { i, v ->
                val h = (v / max) * size.height * grow
                val x = i * (w + gap)
                drawRect(c.rule, Offset(x, 0f), Size(w, size.height))
                drawRect(
                    if (goalMs != null && v >= goalMs) color else color.copy(alpha = 0.6f),
                    Offset(x, size.height - h), Size(w, h),
                )
            }
            if (goalMs != null) {
                val y = size.height - goalMs / max * size.height
                drawLine(c.text, Offset(0f, y), Offset(size.width, y), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { d ->
                Text(
                    d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = if (d == today) TimeoType.labelStrong else TimeoType.label,
                    color = if (d == today) c.text else c.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (goalMs != null) {
            Spacer(Modifier.height(8.dp))
            Text("Dashed line: daily goal of ${formatDuration(goalMs)}", style = TimeoType.label, color = c.muted)
        }
    }
}
