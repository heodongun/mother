package com.smartpet.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.smartpet.todo.ui.TaskListScreen
import com.smartpet.todo.ui.SmartPetTodoTheme
import com.smartpet.todo.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            SmartPetTodoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    
                    TaskListScreen(
                        uiState = uiState,
                        onAddTask = viewModel::addTask,
                        onUpdateTask = viewModel::updateTask,
                        onToggleComplete = { taskId ->
                            viewModel.toggleTaskCompletion(taskId)
                        },
                        onDeleteTask = { taskId ->
                            viewModel.deleteTask(taskId)
                        },
                        onRestoreTask = viewModel::restoreTask,
                        onRefresh = viewModel::refresh
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
