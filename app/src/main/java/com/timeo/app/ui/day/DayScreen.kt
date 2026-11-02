package com.timeo.app.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timeo.app.data.Category
import com.timeo.app.data.Repository
import com.timeo.app.data.Session
import com.timeo.app.domain.Interval
import com.timeo.app.domain.StatsCalculator
import com.timeo.app.ui.components.SectionLabel
import com.timeo.app.ui.components.formatDuration
import com.timeo.app.ui.theme.LocalTimeo
import com.timeo.app.ui.theme.TimeoType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DayState(
    val categories: List<Category> = emptyList(),
    val sessions: List<Session> = emptyList(),
    /** Per top-level category rollup for the day, largest first. */
    val breakdown: List<Pair<Category, Long>> = emptyList(),
)

class DayViewModel(private val repo: Repository, val date: LocalDate) : ViewModel() {
    val zone: ZoneId = ZoneId.systemDefault()
    private val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    val state: StateFlow<DayState> =
        combine(repo.categories, repo.sessionsBetween(from, to)) { cats, sessions ->
            val now = System.currentTimeMillis()
            val breakdown = cats.filter { it.parentId == null }
                .map { it to (StatsCalculator.categoryDaily(it.id, cats, sessions, now, zone)[date] ?: 0L) }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
            DayState(cats, sessions, breakdown)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayState())

    fun save(session: Session) = viewModelScope.launch { repo.saveSession(session) }
    fun delete(session: Session) = viewModelScope.launch { repo.deleteSession(session) }

    fun dayStartMs() = from
}

@Composable
fun DayScreen(vm: DayViewModel, state: DayState, onBack: () -> Unit) {
    val c = LocalTimeo.current
    var editing by remember { mutableStateOf<Session?>(null) }
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    fun time(ms: Long) = Instant.ofEpochMilli(ms).atZone(vm.zone).toLocalTime().format(timeFmt)

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.text) }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .background(c.text)
                    .clickable {
                        val start = minOf(System.currentTimeMillis() - 30 * 60_000, vm.dayStartMs() + 12 * 3_600_000)
                        editing = Session(
                            categoryId = state.categories.firstOrNull()?.id ?: return@clickable,
                            startEpochMs = start,
                            endEpochMs = start + 30 * 60_000,
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, tint = c.background, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add session", style = TimeoType.labelStrong, color = c.background)
            }
            Spacer(Modifier.width(12.dp))
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Text(vm.date.format(DateTimeFormatter.ofPattern("EEEE")), style = TimeoType.body, color = c.muted)
                Text(vm.date.format(DateTimeFormatter.ofPattern("d MMMM")), style = TimeoType.display, color = c.text)
                Spacer(Modifier.height(24.dp))
                if (state.breakdown.isNotEmpty()) {
                    SectionLabel(1, "Breakdown")
                    Spacer(Modifier.height(12.dp))
                    val max = state.breakdown.first().second.toFloat()
                    state.breakdown.forEach { (cat, ms) ->
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.name, style = TimeoType.label, color = c.text, modifier = Modifier.width(96.dp), maxLines = 1)
                            Box(Modifier.weight(1f).height(14.dp)) {
                                Box(Modifier.fillMaxWidth(ms / max).fillMaxHeight().background(Color(cat.colorArgb)))
                            }
                            Text(formatDuration(ms), style = TimeoType.monoSmall, color = c.text, modifier = Modifier.padding(start = 10.dp).width(64.dp))
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
                SectionLabel(2, "Sessions")
                Spacer(Modifier.height(8.dp))
                if (state.sessions.isEmpty()) {
                    Text("Nothing tracked on this day.", style = TimeoType.body, color = c.muted, modifier = Modifier.padding(vertical = 16.dp))
                }
            }
            items(state.sessions, key = { it.id }) { s ->
                val cat = state.categories.firstOrNull { it.id == s.categoryId }
                val parent = cat?.parentId?.let { pid -> state.categories.firstOrNull { it.id == pid } }
                Column(Modifier.clickable { editing = s }) {
                    Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(Color(cat?.colorArgb ?: 0)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                listOfNotNull(parent?.name, cat?.name).joinToString("  ›  "),
                                style = TimeoType.bodyStrong, color = c.text,
                            )
                            Text(
                                "${time(s.startEpochMs)} – ${s.endEpochMs?.let { time(it) } ?: "running"}",
                                style = TimeoType.label, color = c.muted,
                            )
                        }
                        val len = Interval(s.startEpochMs, s.endEpochMs ?: System.currentTimeMillis()).length
                        Text(formatDuration(len), style = TimeoType.monoSmall, color = c.text)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
                }
            }
        }
    }

    editing?.let { s ->
        SessionDialog(
            session = s,
            categories = state.categories,
            zone = vm.zone,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
            onDelete = { vm.delete(it); editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SessionDialog(
    session: Session,
    categories: List<Category>,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onSave: (Session) -> Unit,
    onDelete: (Session) -> Unit,
) {
    val c = LocalTimeo.current
    var categoryId by remember { mutableLongStateOf(session.categoryId) }
    var start by remember { mutableLongStateOf(session.startEpochMs) }
    var end by remember { mutableStateOf(session.endEpochMs) }
    var picking by remember { mutableStateOf<String?>(null) }
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    fun local(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone)
    val valid = end == null || end!! > start

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        containerColor = c.background,
        title = { Text(if (session.id == 0L) "Add session" else "Edit session", style = TimeoType.title, color = c.text) },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        val on = cat.id == categoryId
                        Text(
                            (if (cat.parentId != null) "↳ " else "") + cat.name,
                            style = TimeoType.label,
                            color = if (on) c.background else c.text,
                            modifier = Modifier
                                .background(if (on) Color(cat.colorArgb) else Color.Transparent)
                                .border(1.dp, if (on) Color(cat.colorArgb) else c.rule)
                                .clickable { categoryId = cat.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeField("Start", local(start).format(fmt), Modifier.weight(1f)) { picking = "start" }
                    TimeField("End", end?.let { local(it).format(fmt) } ?: "running", Modifier.weight(1f)) { picking = "end" }
                }
                if (!valid) {
                    Spacer(Modifier.height(8.dp))
                    Text("End must be after start", style = TimeoType.label, color = c.danger)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(session.copy(categoryId = categoryId, startEpochMs = start, endEpochMs = end)) }) {
                Text("Save", style = TimeoType.bodyStrong, color = if (valid) c.text else c.muted)
            }
        },
        dismissButton = {
            Row {
                if (session.id != 0L) {
                    TextButton(onClick = { onDelete(session) }) { Text("Delete", style = TimeoType.bodyStrong, color = c.danger) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", style = TimeoType.bodyStrong, color = c.muted) }
            }
        },
    )

    picking?.let { which ->
        val base = local(if (which == "start") start else end ?: System.currentTimeMillis())
        val tp = rememberTimePickerState(base.hour, base.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = null },
            shape = RoundedCornerShape(2.dp),
            containerColor = c.background,
            text = {
                TimePicker(
                    state = tp,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = c.rule, selectorColor = c.text,
                        timeSelectorSelectedContainerColor = c.text, timeSelectorSelectedContentColor = c.background,
                        timeSelectorUnselectedContainerColor = c.rule,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Keep the session's date; only the clock time changes.
                    val ms = base.toLocalDate().atTime(LocalTime.of(tp.hour, tp.minute)).atZone(zone).toInstant().toEpochMilli()
                    if (which == "start") start = ms else end = ms
                    picking = null
                }) { Text("OK", style = TimeoType.bodyStrong, color = c.text) }
            },
            dismissButton = {
                TextButton(onClick = { picking = null }) { Text("Cancel", style = TimeoType.bodyStrong, color = c.muted) }
            },
        )
    }
}

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalTimeo.current
    Column(modifier.border(1.dp, c.rule).clickable(onClick = onClick).padding(12.dp)) {
        Text(label, style = TimeoType.label, color = c.muted)
        Text(value, style = TimeoType.monoSmall.copy(fontSize = TimeoType.title.fontSize), color = c.text)
    }
}
