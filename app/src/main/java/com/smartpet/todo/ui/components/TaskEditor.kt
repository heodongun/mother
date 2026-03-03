package com.smartpet.todo.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartpet.todo.data.Task
import com.smartpet.todo.data.TaskPriority
import com.smartpet.todo.ui.TossColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TaskEditorDialog(
    initialTask: Task?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        dueDate: Long?,
        priority: TaskPriority,
        maxEnforcementLevel: Int,
        estimatedMinutes: Int?
    ) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(16.dp)
                .testTag("task_editor_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            TaskEditorContent(
                initialTask = initialTask,
                onDismiss = onDismiss,
                onSave = onSave,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Composable
fun TaskEditorContent(
    initialTask: Task?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        dueDate: Long?,
        priority: TaskPriority,
        maxEnforcementLevel: Int,
        estimatedMinutes: Int?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title ?: "") }
    var description by remember(initialTask?.id) { mutableStateOf(initialTask?.description ?: "") }
    var dueDate by remember(initialTask?.id) { mutableStateOf<Long?>(initialTask?.dueDate) }
    var priority by remember(initialTask?.id) { mutableStateOf(initialTask?.priority ?: TaskPriority.NORMAL) }
    var maxEnforcementLevel by remember(initialTask?.id) { mutableIntStateOf(initialTask?.maxEnforcementLevel ?: 3) }
    var estimatedMinutesRaw by remember(initialTask?.id) {
        mutableStateOf(initialTask?.estimatedMinutes?.toString().orEmpty())
    }

    var showDatePicker by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .imePadding()
    ) {
        Text(
            text = if (initialTask == null) "새 할 일" else "할 일 수정",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "미루지 않게, 환경이 같이 도와줘요.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("제목") },
            placeholder = { Text("예: 운동 20분") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_editor_title"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("설명 (선택)") },
            placeholder = { Text("예: 스트레칭, 러닝, 샤워") },
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_editor_description"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showDatePicker = true }
                .padding(16.dp)
                .testTag("task_editor_due_date"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "마감일",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dueDate?.let { formatDate(it) } ?: "미설정",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            if (dueDate != null) {
                TextButton(onClick = { dueDate = null }) {
                    Text("지우기")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "예상 소요 시간 (분)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = estimatedMinutesRaw,
                    onValueChange = { v ->
                        // Keep it numeric-ish; allow empty.
                        estimatedMinutesRaw = v.filter { it.isDigit() }.take(4)
                    },
                    placeholder = { Text("예: 25") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag("task_editor_estimated_minutes"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "우선순위",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityChip(
                label = "낮음",
                selected = priority == TaskPriority.LOW,
                onClick = { priority = TaskPriority.LOW }
            )
            PriorityChip(
                label = "보통",
                selected = priority == TaskPriority.NORMAL,
                onClick = { priority = TaskPriority.NORMAL }
            )
            PriorityChip(
                label = "높음",
                selected = priority == TaskPriority.HIGH,
                onClick = { priority = TaskPriority.HIGH }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "강제 단계 (최대)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "필요하면 더 강하게, 싫으면 여기서 멈춰요.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnforcementChip(
                label = "알림",
                selected = maxEnforcementLevel == 1,
                onClick = { maxEnforcementLevel = 1 }
            )
            EnforcementChip(
                label = "조명",
                selected = maxEnforcementLevel == 2,
                onClick = { maxEnforcementLevel = 2 }
            )
            EnforcementChip(
                label = "전원/로봇",
                selected = maxEnforcementLevel == 3,
                onClick = { maxEnforcementLevel = 3 }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("task_editor_cancel")
            ) {
                Text("취소")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isBlank()) return@Button

                    val parsedMinutes = estimatedMinutesRaw.toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(24 * 60)
                    onSave(
                        trimmedTitle,
                        description.trim(),
                        dueDate,
                        priority,
                        maxEnforcementLevel.coerceIn(1, 3),
                        parsedMinutes
                    )
                },
                enabled = title.trim().isNotBlank(),
                modifier = Modifier.testTag("task_editor_save"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = TossColors.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTask == null) "추가" else "저장",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showDatePicker) {
        DueDatePickerDialog(
            initialDateMillis = dueDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { dateMillis ->
                dueDate = dateMillis
                showDatePicker = false
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PriorityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EnforcementChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun DueDatePickerDialog(
    initialDateMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val onDismissState = rememberUpdatedState(onDismiss)
    val onDateSelectedState = rememberUpdatedState(onDateSelected)

    androidx.compose.runtime.LaunchedEffect(initialDateMillis) {
        val now = System.currentTimeMillis()
        val initialCalendar = Calendar.getInstance().apply {
            timeInMillis = initialDateMillis ?: now
        }

        val dateDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val timeDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        selected.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selected.set(Calendar.MINUTE, minute)
                        onDateSelectedState.value(selected.timeInMillis)
                    },
                    initialCalendar.get(Calendar.HOUR_OF_DAY),
                    initialCalendar.get(Calendar.MINUTE),
                    true
                )
                timeDialog.setOnCancelListener { onDismissState.value() }
                timeDialog.show()
            },
            initialCalendar.get(Calendar.YEAR),
            initialCalendar.get(Calendar.MONTH),
            initialCalendar.get(Calendar.DAY_OF_MONTH)
        )
        dateDialog.setOnCancelListener { onDismissState.value() }
        dateDialog.show()
    }
}

private fun formatDate(timestamp: Long): String {
    val format = SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREA)
    return format.format(Date(timestamp))
}
