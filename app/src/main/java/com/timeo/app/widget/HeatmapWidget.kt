package com.timeo.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.timeo.app.MainActivity
import com.timeo.app.domain.StatsCalculator
import com.timeo.app.repository
import com.timeo.app.ui.components.formatDuration
import com.timeo.app.ui.insights.heatColor

/** The recent weeks of the Insights heatmap (everything combined), sized to fit the widget. */
class HeatmapWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.repository
        val colors = WidgetColors.of(context)
        provideContent {
            val flow = remember { WidgetData.heatmap(repo) }
            val snap by flow.collectAsState(initial = null)
            HeatmapContent(context, snap, colors)
        }
    }
}

class HeatmapWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HeatmapWidget()
}

private val PAD = 12.dp
private val HEADER = 28.dp

@Composable
private fun HeatmapContent(context: Context, snap: HeatmapSnapshot?, c: WidgetColors) {
    val size = LocalSize.current
    Column(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(6.dp)
            .background(c.background)
            .padding(PAD)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("timeo", style = TextStyle(color = ColorProvider(c.text), fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            if (snap != null) {
                Text(
                    "${snap.streak}d streak  ·  ${formatDuration(snap.todayMs)} today",
                    style = TextStyle(color = ColorProvider(c.muted), fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                )
            }
        }
        if (snap != null) {
            Spacer(GlanceModifier.defaultWeight())
            Image(
                ImageProvider(renderGrid(context, snap, c, size)),
                contentDescription = "Activity heatmap",
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.defaultWeight())
        }
    }
}

/** Draws as many full weeks as fit, ending with the current one. */
private fun renderGrid(context: Context, snap: HeatmapSnapshot, c: WidgetColors, size: DpSize): Bitmap {
    val density = context.resources.displayMetrics.density
    val widthDp = (size.width - PAD * 2).value.coerceAtLeast(100f)
    val heightDp = (size.height - PAD * 2 - HEADER).value.coerceAtLeast(40f)
    val stepDp = minOf(heightDp / 7f, 18f)
    val cellDp = stepDp * 0.82f
    val weeks = (widthDp / stepDp).toInt().coerceIn(4, 53)

    val step = stepDp * density
    val cell = cellDp * density
    val w = (weeks * step - (step - cell)).toInt().coerceAtLeast(1)
    val h = (7 * step - (step - cell)).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = 1.5f * density
    val first = StatsCalculator.weekStart(snap.today, snap.weekStart).minusWeeks((weeks - 1).toLong())
    for (col in 0 until weeks) {
        for (row in 0..6) {
            val d = first.plusDays(col * 7L + row)
            if (d.isAfter(snap.today)) continue
            val level = StatsCalculator.heatLevel(snap.daily[d] ?: 0, snap.scaleMs)
            paint.style = Paint.Style.FILL
            paint.color = heatColor(level, c.text, c.rule).toArgb()
            val r = RectF(col * step, row * step, col * step + cell, row * step + cell)
            canvas.drawRoundRect(r, radius, radius, paint)
            if (d == snap.today) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = density
                paint.color = c.muted.toArgb()
                canvas.drawRoundRect(r, radius, radius, paint)
            }
        }
    }
    return bmp
}
