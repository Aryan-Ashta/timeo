package com.timeo.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.timeo.app.ui.theme.LocalTimeo
import com.timeo.app.ui.theme.TimeoType

/** "1h 05m", "42m", "0m" */
fun formatDuration(ms: Long): String {
    val totalMin = (ms / 60_000).coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m.toString().padStart(2, '0')}m"
    }
}

/** "01:12:05" */
fun formatClock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

fun formatGoal(minutes: Int): String = formatDuration(minutes * 60_000L)

val CategoryIcons: Map<String, ImageVector> = linkedMapOf(
    "timer" to Icons.Rounded.Timer,
    "code" to Icons.Rounded.Code,
    "book" to Icons.AutoMirrored.Rounded.MenuBook,
    "school" to Icons.Rounded.School,
    "fitness" to Icons.Rounded.FitnessCenter,
    "run" to Icons.Rounded.DirectionsRun,
    "music" to Icons.Rounded.MusicNote,
    "art" to Icons.Rounded.Brush,
    "work" to Icons.Rounded.Work,
    "mind" to Icons.Rounded.SelfImprovement,
    "language" to Icons.Rounded.Language,
    "people" to Icons.Rounded.People,
    "food" to Icons.Rounded.Restaurant,
    "games" to Icons.Rounded.SportsEsports,
)

fun iconFor(key: String): ImageVector = CategoryIcons[key] ?: Icons.Rounded.Timer

/** A gentle press-in scale, used on plates and buttons. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(stiffness = 600f), label = "press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** The site-style nav: wordmark left, text tabs with an underline on the active one. */
@Composable
fun TopBar(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalTimeo.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("timeo", style = TimeoType.wordmark, color = c.text)
        Spacer(Modifier.weight(1f))
        tabs.forEachIndexed { i, label ->
            val active = i == selected
            Column(
                Modifier
                    .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                    .padding(start = 20.dp, top = 6.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, style = TimeoType.nav, color = if (active) c.text else c.muted)
                Spacer(Modifier.height(4.dp))
                val w by animateFloatAsState(if (active) 1f else 0f, label = "tab")
                Box(
                    Modifier
                        .height(1.5.dp)
                        .width((label.length * 8).dp)
                        .graphicsLayer { scaleX = w }
                        .background(c.text),
                )
            }
        }
    }
}

/** Small numbered section label like "No. 02 — Last 12 months". */
@Composable
fun SectionLabel(number: Int, text: String, modifier: Modifier = Modifier) {
    val c = LocalTimeo.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("No. %02d".format(number), style = TimeoType.labelStrong, color = c.text)
        Text(text, style = TimeoType.label, color = c.muted)
    }
}
