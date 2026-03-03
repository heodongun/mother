package com.smartpet.todo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.smartpet.todo.pet.PetBehavior
import com.smartpet.todo.ui.TossColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Task item card with Toss-style design
 */
@Composable
fun TaskCard(
    task: Task,
    nowMillis: Long,
    onToggleComplete: () -> Unit,
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
            // Checkbox
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
                        .background(
                            if (task.isCompleted) TossColors.Blue else TossColors.Gray100
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = TossColors.White,
                        modifier = Modifier
                            .size(18.dp)
                            .scale(checkScale)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Task content
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
                
                // Due date and status
                if (task.dueDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dueDateText = formatDueDate(task.dueDate, nowMillis)
                        val urgencyText = if (!task.isCompleted) PetBehavior.getTaskMessage(task, nowMillis) else ""
                        
                        Text(
                            text = dueDateText,
                            fontSize = 12.sp,
                            color = TossColors.Gray500
                        )
                        
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
                    Text(
                        text = "예상 ${task.estimatedMinutes}분",
                        fontSize = 12.sp,
                        color = TossColors.Gray500
                    )
                }
            }
            
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.graphicsLayer(alpha = 0.6f)
            ) {
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
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

private fun formatDueDate(timestamp: Long, nowMillis: Long): String {
    val diff = timestamp - nowMillis
    
    return when {
        diff < 0 -> {
            val format = SimpleDateFormat("M월 d일", Locale.KOREA)
            format.format(Date(timestamp)) + " (지남)"
        }
        diff < 60 * 60 * 1000 -> {
            val minutes = (diff / (60 * 1000)).toInt()
            "${minutes}분 후"
        }
        diff < 24 * 60 * 60 * 1000 -> {
            val hours = (diff / (60 * 60 * 1000)).toInt()
            "${hours}시간 후"
        }
        else -> {
            val format = SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA)
            format.format(Date(timestamp))
        }
    }
}
