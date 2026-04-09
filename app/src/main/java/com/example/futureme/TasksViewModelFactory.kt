package com.example.futureme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Factory for TasksViewModel
class TasksViewModelFactory(
    private val dao: TaskDao,
    private val tagDao: TagDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            return TasksViewModel(dao, tagDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
