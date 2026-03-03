package com.smartpet.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.smartpet.todo.data.Task
import com.smartpet.todo.data.TaskPriority
import com.smartpet.todo.pet.PetBehavior
import com.smartpet.todo.ui.components.PetStatusCard
import com.smartpet.todo.ui.components.TaskCard
import com.smartpet.todo.ui.components.TaskEditorDialog
import com.smartpet.todo.viewmodel.TaskUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main task list screen with pet status and task items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    uiState: TaskUiState,
    onAddTask: (String, String, Long?, TaskPriority, Int, Int?) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onToggleComplete: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRestoreTask: (Task) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Recompose periodically so due/urgency updates without user interaction.
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }

    val petState = remember(uiState.tasks, nowMillis) {
        PetBehavior.calculatePetState(uiState.tasks, nowMillis)
    }

    var filter by rememberSaveable { mutableStateOf(TaskFilter.ACTIVE) }
    var editorTask by remember { mutableStateOf<Task?>(null) }
    var isEditorOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Task?>(null) }

    val activeTasks = remember(uiState.tasks, filter) {
        uiState.tasks.filter { !it.isCompleted }
            .sortedWith(
                compareBy<Task> { it.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.priority.ordinal }
                    .thenByDescending { it.createdAt }
            )
    }
    val completedTasks = remember(uiState.tasks, filter) {
        uiState.tasks.filter { it.isCompleted }
            .sortedByDescending { it.completedAt ?: 0L }
    }

    val totalCount = uiState.tasks.size
    val completedCount = uiState.tasks.count { it.isCompleted }
    val overdueCount = remember(uiState.tasks, nowMillis) { PetBehavior.getOverdueCount(uiState.tasks, nowMillis) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "마더",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "미루지 않게, 환경이 도와줘요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "새로고침")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editorTask = null
                    isEditorOpen = true
                },
                modifier = Modifier
                    .testTag("add_fab")
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "할 일 추가",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val listState = rememberLazyListState()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentPadding = when {
                maxWidth < 600.dp -> PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                maxWidth < 840.dp -> PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                else -> PaddingValues(horizontal = 32.dp, vertical = 20.dp)
            }
            val isExpanded = maxWidth >= 840.dp

            if (uiState.isLoading && uiState.tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@BoxWithConstraints
            }

            if (isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.width(360.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PetStatusCard(petState = petState)
                        StatsCard(
                            totalCount = totalCount,
                            completedCount = completedCount,
                            overdueCount = overdueCount
                        )
                        TaskFilterRow(
                            filter = filter,
                            onFilterChange = { filter = it }
                        )
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        TasksList(
                            filter = filter,
                            activeTasks = activeTasks,
                            completedTasks = completedTasks,
                            nowMillis = nowMillis,
                            listState = listState,
                            onTaskClick = { task ->
                                editorTask = task
                                isEditorOpen = true
                            },
                            onToggleComplete = onToggleComplete,
                            onRequestDelete = { task -> pendingDelete = task },
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 720.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PetStatusCard(petState = petState)
                    StatsCard(
                        totalCount = totalCount,
                        completedCount = completedCount,
                        overdueCount = overdueCount
                    )
                    TaskFilterRow(
                        filter = filter,
                        onFilterChange = { filter = it }
                    )

                    TasksList(
                        filter = filter,
                        activeTasks = activeTasks,
                        completedTasks = completedTasks,
                        nowMillis = nowMillis,
                        listState = listState,
                        onTaskClick = { task ->
                            editorTask = task
                            isEditorOpen = true
                        },
                        onToggleComplete = onToggleComplete,
                        onRequestDelete = { task -> pendingDelete = task },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (isEditorOpen) {
        TaskEditorDialog(
            initialTask = editorTask,
            onDismiss = {
                isEditorOpen = false
                editorTask = null
            },
            onSave = { title, description, dueDate, priority, maxEnforcementLevel, estimatedMinutes ->
                val editing = editorTask
                if (editing == null) {
                    onAddTask(title, description, dueDate, priority, maxEnforcementLevel, estimatedMinutes)
                } else {
                    onUpdateTask(
                        editing.copy(
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            priority = priority,
                            maxEnforcementLevel = maxEnforcementLevel,
                            estimatedMinutes = estimatedMinutes
                        )
                    )
                }
                isEditorOpen = false
                editorTask = null
            }
        )
    }

    if (pendingDelete != null) {
        val task = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("삭제할까요?") },
            text = { Text("“${task.title}”을(를) 삭제합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteTask(task.id)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "삭제했어요",
                                actionLabel = "되돌리기",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                onRestoreTask(task)
                            }
                        }
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

private enum class TaskFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

@Composable
private fun StatsCard(
    totalCount: Int,
    completedCount: Int,
    overdueCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "진행 상황",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (totalCount > 0) {
                    Text(
                        text = "$completedCount / $totalCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )

            if (overdueCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "마감 지난 할 일 ${overdueCount}개",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TaskFilterRow(
    filter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filter == TaskFilter.ACTIVE,
            onClick = { onFilterChange(TaskFilter.ACTIVE) },
            label = { Text("진행중") }
        )
        FilterChip(
            selected = filter == TaskFilter.ALL,
            onClick = { onFilterChange(TaskFilter.ALL) },
            label = { Text("전체") }
        )
        FilterChip(
            selected = filter == TaskFilter.COMPLETED,
            onClick = { onFilterChange(TaskFilter.COMPLETED) },
            label = { Text("완료") }
        )
    }
}

@Composable
private fun TasksList(
    filter: TaskFilter,
    activeTasks: List<Task>,
    completedTasks: List<Task>,
    nowMillis: Long,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTaskClick: (Task) -> Unit,
    onToggleComplete: (String) -> Unit,
    onRequestDelete: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("task_list"),
        state = listState,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val showEmpty = when (filter) {
            TaskFilter.ALL -> activeTasks.isEmpty() && completedTasks.isEmpty()
            TaskFilter.ACTIVE -> activeTasks.isEmpty()
            TaskFilter.COMPLETED -> completedTasks.isEmpty()
        }

        if (showEmpty) {
            item(key = "empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📝", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = when (filter) {
                                TaskFilter.COMPLETED -> "완료된 할 일이 아직 없어요."
                                else -> "할 일이 없어요!\n새로운 할 일을 추가해보세요."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
            return@LazyColumn
        }

        if (filter == TaskFilter.ALL || filter == TaskFilter.ACTIVE) {
            item(key = "active_header") {
                Text(
                    text = "할 일",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(activeTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    nowMillis = nowMillis,
                    onToggleComplete = { onToggleComplete(task.id) },
                    onDelete = { onRequestDelete(task) },
                    onClick = { onTaskClick(task) }
                )
            }
        }

        if ((filter == TaskFilter.ALL && completedTasks.isNotEmpty()) || filter == TaskFilter.COMPLETED) {
            item(key = "completed_header") {
                Text(
                    text = "완료됨",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            items(completedTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    nowMillis = nowMillis,
                    onToggleComplete = { onToggleComplete(task.id) },
                    onDelete = { onRequestDelete(task) },
                    onClick = { onTaskClick(task) }
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
