package com.timeo.app.ui.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timeo.app.domain.StatsCalculator
import com.timeo.app.ui.theme.LocalTimeo
import com.timeo.app.ui.theme.TimeoType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

const val HEATMAP_WEEKS = 53

/** Fill colour for a heat level: empty cells use the rule colour, then four steps toward [color]. */
fun heatColor(level: Int, color: Color, empty: Color): Color = when (level) {
    0 -> empty
    1 -> lerp(empty, color, 0.3f)
    2 -> lerp(empty, color, 0.55f)
    3 -> lerp(empty, color, 0.8f)
    else -> color
}

/**
 * GitHub-style contribution grid: one column per week, one row per weekday,
 * the last [HEATMAP_WEEKS] weeks ending with the current one.
 */
@Composable
fun Heatmap(
    daily: Map<LocalDate, Long>,
    scaleMs: Long,
    color: Color,
    today: LocalDate,
    weekStart: DayOfWeek,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    val c = LocalTimeo.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = TimeoType.label.copy(fontSize = 10.sp, color = c.muted)

    val cell = 13.dp
    val gap = 3.dp
    val top = 18.dp
    val left = 28.dp
    val firstDay = StatsCalculator.weekStart(today, weekStart).minusWeeks((HEATMAP_WEEKS - 1).toLong())

    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(daily, color) {
        appear.snapTo(0f)
        appear.animateTo(1f, tween(500))
    }

    val width = left + (cell + gap) * HEATMAP_WEEKS
    val height = top + (cell + gap) * 7

    Row {
        // Weekday labels stay pinned while the grid scrolls.
        Canvas(Modifier.width(left).height(height)) {
            val step = (cell + gap).toPx()
            for (row in listOf(0, 2, 4)) {
                val day = weekStart.plus(row.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                val m = measurer.measure(day, labelStyle)
                drawText(m, topLeft = Offset(0f, top.toPx() + row * step + (cell.toPx() - m.size.height) / 2))
            }
        }
        Box(Modifier.horizontalScroll(scroll).padding(end = 20.dp)) {
            Canvas(
                Modifier
                    .width(width - left)
                    .height(height)
                    .pointerInput(firstDay, today) {
                        detectTapGestures { pos ->
                            val step = (cell + gap).toPx()
                            val col = (pos.x / step).toInt()
                            val row = ((pos.y - top.toPx()) / step).toInt()
                            if (col in 0 until HEATMAP_WEEKS && row in 0..6 && pos.y >= top.toPx()) {
                                val d = firstDay.plusDays(col * 7L + row)
                                if (!d.isAfter(today)) onSelect(d)
                            }
                        }
                    },
            ) {
                val step = (cell + gap).toPx()
                val cellPx = cell.toPx()
                val radius = CornerRadius(with(density) { 1.5.dp.toPx() })
                var lastMonth = -1
                for (col in 0 until HEATMAP_WEEKS) {
                    val colStart = firstDay.plusDays(col * 7L)
                    if (colStart.monthValue != lastMonth) {
                        if (lastMonth != -1 || colStart.dayOfMonth <= 7) {
                            val m = measurer.measure(
                                colStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                labelStyle,
                            )
                            if (col * step + m.size.width <= size.width) drawText(m, topLeft = Offset(col * step, 0f))
                        }
                        lastMonth = colStart.monthValue
                    }
                    for (row in 0..6) {
                        val d = colStart.plusDays(row.toLong())
                        if (d.isAfter(today)) continue
                        val level = StatsCalculator.heatLevel(daily[d] ?: 0, scaleMs)
                        // Cells fade in left to right.
                        val t = ((appear.value * (HEATMAP_WEEKS + 12) - col) / 12f).coerceIn(0f, 1f)
                        val fill = lerp(c.rule, heatColor(level, color, c.rule), t)
                        val o = Offset(col * step, top.toPx() + row * step)
                        drawRoundRect(fill, o, Size(cellPx, cellPx), radius)
                        if (d == selected || d == today) {
                            drawRoundRect(
                                if (d == selected) c.text else c.muted,
                                o - Offset(1.5f, 1.5f),
                                Size(cellPx + 3f, cellPx + 3f),
                                radius,
                                style = Stroke(width = if (d == selected) 2.5f else 1.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}
