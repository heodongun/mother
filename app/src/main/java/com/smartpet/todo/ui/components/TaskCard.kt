package com.smartpet.todo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.smartpet.todo.data.Task
import com.smartpet.todo.data.TaskPriority
import com.smartpet.todo.penalty.TaskPenaltyPresentation
import com.smartpet.todo.pet.PetBehavior
import com.smartpet.todo.ui.TossColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskCard(
    task: Task,
    nowMillis: Long,
    penaltyPresentation: TaskPenaltyPresentation,
    onToggleComplete: () -> Unit,
    onVerify: (() -> Unit)?,
    isVerifying: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interventionLevel = task.getInterventionLevel(nowMillis)

    val borderColor by animateColorAsState(
        targetValue = when {
            task.isCompleted -> TossColors.Gray100
            interventionLevel == 3 -> TossColors.Red
            interventionLevel == 2 -> TossColors.Orange
            interventionLevel == 1 -> TossColors.Yellow
            else -> TossColors.Gray100
        },
        label = "borderColor"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(200),
        label = "checkScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) TossColors.Gray50 else TossColors.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (interventionLevel >= 2 && !task.isCompleted) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .toggleable(
                        value = task.isCompleted,
                        role = Role.Checkbox,
                        onValueChange = { onToggleComplete() }
                    )
                    .semantics {
                        contentDescription = "할 일 완료"
                        stateDescription = if (task.isCompleted) "완료됨" else "미완료"
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompleted) TossColors.Blue else TossColors.Gray100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = TossColors.White,
                        modifier = Modifier.size(18.dp).scale(checkScale)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isCompleted) TossColors.Gray500 else TossColors.Black,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    PriorityBadge(priority = task.priority)
                }

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 14.sp,
                        color = TossColors.Gray500,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.dueDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dueDateText = formatDueDate(task.dueDate, nowMillis)
                        val urgencyText = if (!task.isCompleted) PetBehavior.getTaskMessage(task, nowMillis) else ""

                        Text(text = dueDateText, fontSize = 12.sp, color = TossColors.Gray500)

                        if (urgencyText.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = urgencyText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (interventionLevel) {
                                    3 -> TossColors.Red
                                    2 -> TossColors.Orange
                                    1 -> TossColors.Yellow
                                    else -> TossColors.Gray500
                                }
                            )
                        }
                    }
                }

                if (task.estimatedMinutes != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "예상 ${task.estimatedMinutes}분", fontSize = 12.sp, color = TossColors.Gray500)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = penaltyPresentation.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TossColors.Blue
                )
                Text(
                    text = penaltyPresentation.detail,
                    fontSize = 12.sp,
                    color = TossColors.Gray500,
                    lineHeight = 16.sp
                )
                penaltyPresentation.warning?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 16.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!task.isCompleted && onVerify != null) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = TossColors.Blue
                        )
                    } else {
                        TextButton(
                            onClick = onVerify,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(text = "인증하기", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.graphicsLayer(alpha = 0.6f)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "삭제",
                        tint = TossColors.Gray500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: TaskPriority) {
    val (bg, fg, label) = when (priority) {
        TaskPriority.LOW -> Triple(TossColors.Gray100, TossColors.Gray700, "낮음")
        TaskPriority.NORMAL -> Triple(TossColors.Gray50, TossColors.Gray700, "보통")
        TaskPriority.HIGH -> Triple(TossColors.Red.copy(alpha = 0.12f), TossColors.Red, "높음")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

private fun formatDueDate(timestamp: Long, nowMillis: Long): String {
    val diff = timestamp - nowMillis
    return when {
        diff < 0 -> SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(timestamp)) + " (지남)"
        diff < 60 * 60 * 1000 -> "${(diff / (60 * 1000)).toInt()}분 후"
        diff < 24 * 60 * 60 * 1000 -> "${(diff / (60 * 60 * 1000)).toInt()}시간 후"
        else -> SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(timestamp))
    }
}
