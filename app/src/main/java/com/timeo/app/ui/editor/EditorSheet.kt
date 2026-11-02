package com.timeo.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.timeo.app.data.Category
import com.timeo.app.ui.components.CategoryIcons
import com.timeo.app.ui.components.formatGoal
import com.timeo.app.ui.theme.LocalTimeo
import com.timeo.app.ui.theme.PlateArgb
import com.timeo.app.ui.theme.TimeoType
import com.timeo.app.ui.theme.onPlate

/** What the editor is opened for: a new timer (optionally under a parent) or an existing one. */
data class EditorRequest(val category: Category? = null, val parentId: Long? = null)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorSheet(
    request: EditorRequest,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (Category, deleteChildren: Boolean) -> Unit,
) {
    val c = LocalTimeo.current
    val existing = request.category
    val hasChildren = existing != null && allCategories.any { it.parentId == existing.id }
    // A category with subtimers stays top-level (one level of nesting only).
    val parentOptions = allCategories.filter { it.parentId == null && it.id != existing?.id }
    val initialParent = existing?.parentId ?: request.parentId
    val parentColor = allCategories.firstOrNull { it.id == initialParent }?.colorArgb

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var parentId by rememberSaveable { mutableStateOf(initialParent) }
    var color by rememberSaveable {
        mutableIntStateOf(existing?.colorArgb ?: parentColor ?: PlateArgb[allCategories.count { it.parentId == null } % PlateArgb.size])
    }
    var icon by rememberSaveable { mutableStateOf(existing?.iconKey ?: "timer") }
    var daily by rememberSaveable { mutableIntStateOf(existing?.dailyGoalMinutes ?: 0) }
    var weekly by rememberSaveable { mutableIntStateOf(existing?.weeklyGoalMinutes ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }

    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
        containerColor = c.background,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(36.dp, 3.dp).background(c.rule)) },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            val title = when {
                existing != null -> "Edit timer"
                parentId != null -> "New subtimer"
                else -> "New timer"
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = TimeoType.title, color = c.text)
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                placeholder = { Text("Name, e.g. Reading", style = TimeoType.body) },
                singleLine = true,
                textStyle = TimeoType.body,
                shape = RectangleShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.text, unfocusedBorderColor = c.rule, cursorColor = c.text,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!hasChildren && parentOptions.isNotEmpty()) {
                FieldLabel("Subtimer of")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("None", parentId == null) { parentId = null }
                    parentOptions.forEach { p ->
                        Chip(p.name, parentId == p.id, swatch = Color(p.colorArgb)) {
                            parentId = p.id
                            if (existing == null) color = p.colorArgb
                        }
                    }
                }
            }

            FieldLabel("Color")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlateArgb.forEach { argb ->
                    val plate = Color(argb)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(plate)
                            .clickable { color = argb },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (argb == color) Icon(Icons.Rounded.Check, null, tint = onPlate(plate), modifier = Modifier.size(18.dp))
                    }
                }
            }

            FieldLabel("Icon")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIcons.forEach { (key, vector) ->
                    val selected = key == icon
                    Box(
                        Modifier
                            .size(42.dp)
                            .background(if (selected) c.text else Color.Transparent)
                            .border(1.dp, if (selected) c.text else c.rule)
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(vector, key, tint = if (selected) c.background else c.text, modifier = Modifier.size(20.dp))
                    }
                }
            }

            FieldLabel("Daily goal")
            Stepper(daily, step = if (daily >= 60) 15 else 5, onChange = { daily = it })
            FieldLabel("Weekly goal")
            Stepper(weekly, step = 30, onChange = { weekly = it })

            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (existing != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete", style = TimeoType.bodyStrong, color = c.danger)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", style = TimeoType.bodyStrong, color = c.muted) }
                Spacer(Modifier.width(8.dp))
                val valid = name.isNotBlank()
                Text(
                    "Save",
                    style = TimeoType.bodyStrong,
                    color = c.background,
                    modifier = Modifier
                        .background(if (valid) c.text else c.muted)
                        .clickable(enabled = valid) {
                            onSave(
                                (existing ?: Category(name = "", colorArgb = color)).copy(
                                    name = name.trim(),
                                    parentId = parentId,
                                    colorArgb = color,
                                    iconKey = icon,
                                    dailyGoalMinutes = daily.takeIf { it > 0 },
                                    weeklyGoalMinutes = weekly.takeIf { it > 0 },
                                ),
                            )
                        }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                )
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(2.dp),
            containerColor = c.background,
            title = { Text("Delete ${existing.name}?", style = TimeoType.title, color = c.text) },
            text = {
                Text(
                    if (hasChildren) "Its history will be removed. What should happen to its subtimers?"
                    else "This removes the timer and all of its history.",
                    style = TimeoType.body, color = c.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(existing, true) }) {
                    Text(if (hasChildren) "Delete all" else "Delete", style = TimeoType.bodyStrong, color = c.danger)
                }
            },
            dismissButton = {
                Row {
                    if (hasChildren) {
                        TextButton(onClick = { onDelete(existing, false) }) {
                            Text("Keep subtimers", style = TimeoType.bodyStrong, color = c.text)
                        }
                    }
                    TextButton(onClick = { confirmDelete = false }) {
                        Text("Cancel", style = TimeoType.bodyStrong, color = c.muted)
                    }
                }
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Spacer(Modifier.height(22.dp))
    Text(text, style = TimeoType.labelStrong, color = LocalTimeo.current.muted)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Chip(label: String, selected: Boolean, swatch: Color? = null, onClick: () -> Unit) {
    val c = LocalTimeo.current
    Row(
        Modifier
            .background(if (selected) c.text else Color.Transparent)
            .border(1.dp, if (selected) c.text else c.rule)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (swatch != null) {
            Box(Modifier.size(8.dp).background(swatch))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = TimeoType.label.copy(fontSize = TimeoType.body.fontSize * 0.85f), color = if (selected) c.background else c.text)
    }
}

@Composable
private fun Stepper(minutes: Int, step: Int, onChange: (Int) -> Unit) {
    val c = LocalTimeo.current
    Row(Modifier.fillMaxWidth().border(1.dp, c.rule), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clickable(enabled = minutes > 0) { onChange((minutes - step).coerceAtLeast(0)) },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Remove, "Less", tint = if (minutes > 0) c.text else c.rule) }
        Text(
            if (minutes == 0) "No goal" else formatGoal(minutes),
            style = TimeoType.monoSmall.copy(fontSize = TimeoType.body.fontSize),
            color = if (minutes == 0) c.muted else c.text,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(
            Modifier.size(48.dp).clickable { onChange((minutes + step).coerceAtMost(24 * 60 * 7)) },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "More", tint = c.text) }
    }
}
