package com.timeo.app.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.timeo.app.data.Category
import com.timeo.app.ui.components.formatClock
import com.timeo.app.ui.components.formatDuration
import com.timeo.app.ui.components.formatGoal
import com.timeo.app.ui.components.iconFor
import com.timeo.app.ui.components.pressScale
import com.timeo.app.ui.theme.LocalTimeo
import com.timeo.app.ui.theme.TimeoType
import com.timeo.app.ui.theme.onPlate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    state: TodayState,
    onToggle: (Long) -> Unit,
    onEdit: (Category) -> Unit,
    onAdd: (parentId: Long?) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    contentPadding: PaddingValues,
) {
    val c = LocalTimeo.current
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(
                    state.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    style = TimeoType.body,
                    color = c.muted,
                    modifier = Modifier.clickable { onOpenDay(state.date) },
                )
                Spacer(Modifier.height(6.dp))
                Text(formatDuration(state.totalMs), style = TimeoType.display, color = c.text)
                Text("tracked today", style = TimeoType.label, color = c.muted)
            }
        }
        if (state.loaded && state.items.isEmpty()) {
            item(key = "empty") { EmptyState(onAdd = { onAdd(null) }) }
        }
        items(state.items, key = { it.category.id }) { item ->
            val open = expanded[item.category.id] == true
            Column(Modifier.animateItem()) {
                Plate(
                    item = item,
                    expanded = open,
                    onToggle = { onToggle(item.category.id) },
                    onEdit = { onEdit(item.category) },
                    onExpand = {
                        if (item.children.isEmpty()) onAdd(item.category.id)
                        else expanded[item.category.id] = !open
                    },
                )
                AnimatedVisibility(
                    visible = open && item.children.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SubtimerList(
                        parent = item,
                        onToggle = onToggle,
                        onEdit = onEdit,
                        onAdd = { onAdd(item.category.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Plate(
    item: TimerItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onExpand: () -> Unit,
) {
    val cat = item.category
    val plate = Color(cat.colorArgb)
    val fg = onPlate(plate)
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val progress by animateFloatAsState(item.dailyProgress, tween(600), label = "progress")

    Column(
        Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .background(plate)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
                onLongClick = onEdit,
            )
            .animateContentSize(),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.active) {
                    LiveDot(fg)
                    Spacer(Modifier.width(8.dp))
                    Text(if (item.runningSelf) "Running" else "Running · subtimer", style = TimeoType.labelStrong, color = fg)
                } else {
                    Text("No. %02d".format(item.number), style = TimeoType.labelStrong, color = fg)
                }
                Spacer(Modifier.weight(1f))
                Text(goalLine(cat), style = TimeoType.label, color = fg.copy(alpha = 0.8f))
            }

            if (item.active) {
                Spacer(Modifier.height(18.dp))
                Text(formatClock(item.liveMs), style = TimeoType.clock, color = fg)
            }
            Spacer(Modifier.height(if (item.active) 10.dp else 36.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Icon(iconFor(cat.iconKey), null, tint = fg, modifier = Modifier.padding(bottom = 4.dp).size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    cat.name,
                    style = TimeoType.plateTitle,
                    color = fg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                PlayButton(running = item.runningSelf, fg = fg, bg = plate)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        append("Today  ")
                        withStyle(SpanStyle(fontWeight = FontWeight(700))) { append(formatDuration(item.todayMs)) }
                        append("     Week  ")
                        withStyle(SpanStyle(fontWeight = FontWeight(700))) { append(formatDuration(item.weekMs)) }
                        cat.weeklyGoalMinutes?.let { append(" / ${formatGoal(it)}") }
                    },
                    style = TimeoType.label,
                    color = fg,
                    modifier = Modifier.weight(1f),
                )
                SubtimerToggle(item.children.size, expanded, fg, onExpand)
            }
        }
        // Daily goal rule along the bottom edge; weekly goal as a thinner rule beneath it.
        if (cat.dailyGoalMinutes != null) GoalRule(progress, fg, 4.dp)
        cat.weeklyGoalMinutes?.takeIf { it > 0 }?.let { goal ->
            val wp by animateFloatAsState((item.weekMs / (goal * 60_000f)).coerceIn(0f, 1f), tween(600), label = "week")
            GoalRule(wp, fg.copy(alpha = 0.55f), 2.dp)
        }
    }
}

@Composable
private fun GoalRule(progress: Float, color: Color, height: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxWidth().height(height).background(color.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(color))
    }
}

@Composable
private fun PlayButton(running: Boolean, fg: Color, bg: Color) {
    Box(
        Modifier
            .size(44.dp)
            .background(if (running) fg else Color.Transparent, CircleShape)
            .border(1.5.dp, fg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = if (running) "Stop" else "Start",
            tint = if (running) bg else fg,
        )
    }
}

@Composable
private fun SubtimerToggle(count: Int, expanded: Boolean, fg: Color, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (expanded) 180f else 0f, label = "chev")
    Row(
        Modifier
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .border(1.dp, fg.copy(alpha = 0.5f))
            .padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (count == 0) "+ sub" else "$count sub", style = TimeoType.labelStrong, color = fg)
        if (count > 0) {
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = fg, modifier = Modifier.size(16.dp).rotate(rot))
        } else {
            Spacer(Modifier.width(4.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubtimerList(
    parent: TimerItem,
    onToggle: (Long) -> Unit,
    onEdit: (Category) -> Unit,
    onAdd: () -> Unit,
) {
    val c = LocalTimeo.current
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().border(1.dp, c.rule)) {
        parent.children.forEach { child ->
            val color = Color(child.category.colorArgb)
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle(child.category.id)
                        },
                        onLongClick = { onEdit(child.category) },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).background(color))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(child.category.name, style = TimeoType.bodyStrong, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    child.category.dailyGoalMinutes?.let { goal ->
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(0.7f).height(2.dp).background(c.rule)) {
                            Box(Modifier.fillMaxWidth(child.dailyProgress).fillMaxHeight().background(color))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${formatGoal(goal)} / day", style = TimeoType.label, color = c.muted)
                    }
                }
                Text(
                    if (child.runningSelf) formatClock(child.liveMs) else formatDuration(child.todayMs),
                    style = TimeoType.monoSmall,
                    color = if (child.runningSelf) color else c.text,
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .background(if (child.runningSelf) color else Color.Transparent, CircleShape)
                        .border(1.dp, if (child.runningSelf) color else c.rule, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (child.runningSelf) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (child.runningSelf) onPlate(color) else c.text,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
        }
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = c.muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add subtimer", style = TimeoType.label, color = c.muted)
        }
    }
}

@Composable
private fun LiveDot(color: Color) {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(1f, 0.25f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(8.dp).alpha(a).background(color, CircleShape))
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = LocalTimeo.current
    Column(Modifier.fillMaxWidth().border(1.dp, c.rule).padding(24.dp)) {
        Text("No. 00", style = TimeoType.labelStrong, color = c.text)
        Spacer(Modifier.height(28.dp))
        Text("Start your first timer", style = TimeoType.title, color = c.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "Make a timer for anything you want to spend time on: reading, the gym, a side project. Add subtimers to break it down.",
            style = TimeoType.body,
            color = c.muted,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "New timer",
            style = TimeoType.bodyStrong,
            color = c.background,
            modifier = Modifier.background(c.text).clickable(onClick = onAdd).padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

private fun goalLine(cat: Category): String = when (val g = cat.dailyGoalMinutes) {
    null -> if (cat.weeklyGoalMinutes != null) "${formatGoal(cat.weeklyGoalMinutes)} / week" else "No goal"
    else -> "${formatGoal(g)} / day"
}
