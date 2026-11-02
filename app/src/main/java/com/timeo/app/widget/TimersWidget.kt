package com.timeo.app.widget

import android.content.Context
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.timeo.app.MainActivity
import com.timeo.app.R
import com.timeo.app.repository
import com.timeo.app.ui.components.formatDuration
import com.timeo.app.ui.theme.onPlate

/** A list of timers with start/stop buttons and live counters for whatever is running. */
class TimersWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.repository
        val colors = WidgetColors.of(context)
        provideContent {
            val flow = remember { WidgetData.timers(repo) }
            val snap by flow.collectAsState(initial = null)
            TimersContent(context, snap, colors)
        }
    }
}

class TimersWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimersWidget()
}

private val CategoryKey = ActionParameters.Key<Long>("categoryId")

class ToggleTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[CategoryKey] ?: return
        context.repository.toggle(id)
        WidgetData.refreshAll(context)
    }
}

@Composable
private fun TimersContent(context: Context, snap: TimersSnapshot?, c: WidgetColors) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(6.dp)
            .background(c.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            GlanceModifier.fillMaxWidth().padding(bottom = 8.dp).clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("timeo", style = TextStyle(color = ColorProvider(c.text), fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            if (snap != null) {
                if (snap.anyActive) {
                    Ticker(context, snap.nowMs, snap.totalMs, c.text, 14f)
                } else {
                    Text(
                        formatDuration(snap.totalMs),
                        style = TextStyle(color = ColorProvider(c.text), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    )
                }
                Text("  today", style = TextStyle(color = ColorProvider(c.muted), fontSize = 12.sp))
            }
        }
        when {
            snap == null -> Unit
            snap.rows.isEmpty() -> Text(
                "Open timeo to add your first timer",
                style = TextStyle(color = ColorProvider(c.muted), fontSize = 13.sp),
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            )
            else -> {
                // A plain column rather than a LazyColumn: Chronometers inside collection items don't tick.
                val fit = ((LocalSize.current.height.value - 50f) / 50f).toInt().coerceIn(1, 8)
                val shown = if (snap.rows.size > fit) snap.rows.take(fit - 1) else snap.rows
                shown.forEach { row ->
                    TimerRow(context, snap.nowMs, row, c)
                    Spacer(GlanceModifier.height(4.dp))
                }
                if (shown.size < snap.rows.size) {
                    Text(
                        "+${snap.rows.size - shown.size} more in timeo",
                        style = TextStyle(color = ColorProvider(c.muted), fontSize = 12.sp),
                        modifier = GlanceModifier.padding(top = 4.dp).clickable(actionStartActivity<MainActivity>()),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerRow(context: Context, nowMs: Long, row: WidgetRow, c: WidgetColors) {
    val plate = Color(row.colorArgb)
    // Top-level timers are solid colour plates; subtimers are indented rows with a colour tab.
    val bg = if (row.isChild) c.background else plate
    val fg = if (row.isChild) c.text else onPlate(plate)
    val toggle = actionRunCallback<ToggleTimerAction>(actionParametersOf(CategoryKey to row.id))
    Row(
        GlanceModifier
            .fillMaxWidth()
            .height(if (row.isChild) 40.dp else 48.dp)
            .padding(start = if (row.isChild) 14.dp else 0.dp)
            .clickable(toggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            GlanceModifier.defaultWeight().height(if (row.isChild) 40.dp else 48.dp).background(bg).cornerRadius(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (row.isChild) Box(GlanceModifier.width(4.dp).height(40.dp).background(plate)) {}
            Column(GlanceModifier.defaultWeight().padding(start = 10.dp)) {
                Text(
                    row.name,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(fg), fontSize = if (row.isChild) 13.sp else 15.sp, fontWeight = FontWeight.Bold),
                )
                row.goalProgress?.let {
                    Text("${it.coerceAtMost(999)}% of goal", style = TextStyle(color = ColorProvider(fg.copy(alpha = 0.75f)), fontSize = 11.sp))
                }
            }
            if (row.active) {
                Ticker(context, nowMs, row.todayMs, fg, 13f)
            } else {
                Text(
                    formatDuration(row.todayMs),
                    style = TextStyle(color = ColorProvider(fg), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Box(
                GlanceModifier
                    .size(32.dp)
                    .cornerRadius(16.dp)
                    .background(if (row.runningSelf) fg else fg.copy(alpha = 0.15f))
                    .clickable(toggle),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ImageProvider(if (row.runningSelf) R.drawable.ic_widget_stop else R.drawable.ic_widget_play),
                    contentDescription = if (row.runningSelf) "Stop ${row.name}" else "Start ${row.name}",
                    colorFilter = ColorFilter.tint(ColorProvider(if (row.runningSelf) bg else fg)),
                    modifier = GlanceModifier.size(18.dp),
                )
            }
            Spacer(GlanceModifier.width(8.dp))
        }
    }
}

/**
 * A live H:MM:SS counter. Glance can't tick by itself, so this embeds a platform Chronometer.
 * It needs a fixed width: left unsized, AndroidRemoteViews fills the row and squeezes out its siblings.
 */
@Composable
private fun Ticker(context: Context, snapshotMs: Long, elapsedMs: Long, color: Color, sizeSp: Float) {
    val views = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
        val sinceSnapshot = System.currentTimeMillis() - snapshotMs
        setChronometer(R.id.chrono, SystemClock.elapsedRealtime() - sinceSnapshot - elapsedMs, null, true)
        setTextColor(R.id.chrono, color.toArgb())
        setTextViewTextSize(R.id.chrono, TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }
    AndroidRemoteViews(views, GlanceModifier.width((sizeSp * 5.2f).dp))
}
