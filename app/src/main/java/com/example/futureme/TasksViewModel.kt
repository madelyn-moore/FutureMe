package com.example.futureme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TasksViewModel(val dao : TaskDao) : ViewModel() {

    // Mutable live data for these because we want to update and change them as we go
    val newTaskName = MutableLiveData("")
    val newTags = MutableLiveData("")
    val newDueDate = MutableLiveData("")
    val newDatePostponed = MutableLiveData("")
    val newDescription = MutableLiveData("")
    val newWhyTaskPushedOff = MutableLiveData("")
    val newPenalties = MutableLiveData("")
    val newNumDaysDelayed = MutableLiveData("")
    val newOverdueStatus = MutableLiveData(false)
    val newDateCompleted = MutableLiveData("")

    val tasks = dao.getAllTasks()

    // Calculations for analytics page i had to look up how to do these because i wasn't entirely sure
    // what to do but i wanted to include them definitely change them if needed


    // this is juts counting if the date postponed is empty will need to change
    val totalPostponedCount: LiveData<String> = tasks.map { list ->
        list.count { it.datePostponed.isNotEmpty() }.toString()
    }

    val averageDelay: LiveData<String> = tasks.map { list ->
        val delayedTasks = list.filter { it.numDaysDelayed.isNotEmpty() }
        if (delayedTasks.isEmpty()) "0.0"
        else {
            val totalDays = delayedTasks.sumOf { it.numDaysDelayed.toDoubleOrNull() ?: 0.0 }
            String.format("%.2f", totalDays / delayedTasks.size)
        }
    }

    val overdueCount: LiveData<String> = tasks.map { list ->
        list.count { it.overdueStatus }.toString()
    }

    val mostDeferredTaskName: LiveData<String> = tasks.map { list ->
        list.filter { it.numDaysDelayed.isNotEmpty() }
            .maxByOrNull { it.numDaysDelayed.toDoubleOrNull() ?: 0.0 }?.name ?: "None"
    }

    val tasksString: LiveData<String> = tasks.map { tasks ->
        formatTasks(tasks)
    }


    // add task function
    fun addTask() {
        viewModelScope.launch {
            val task = Task(
                name = newTaskName.value ?: "",
                tags = newTags.value ?: "",
                dueDate = newDueDate.value ?: "",
                datePostponed = newDatePostponed.value ?: "",
                description = newDescription.value ?: "",
                whyTaskPushedOff = newWhyTaskPushedOff.value ?: "",
                penalties = newPenalties.value ?: "",
                numDaysDelayed = newNumDaysDelayed.value ?: "",
                overdueStatus = newOverdueStatus.value ?: false,
                dateCompleted = newDateCompleted.value ?: ""
            )
            dao.insert(task)
        }
    }

    // format tasks I put this in from the lecture def will need to change
    fun formatTasks(tasks: List<Task>): String {
        return tasks.fold("") { str, item ->
            str + '\n' + formatTask(item)
        }
    }

    fun formatTask(task: Task): String {
        var str = "ID: ${task.taskId}"
        str += '\n' + "Name: ${task.name}"
        str += '\n' + "Tags: ${task.tags}"
        str += '\n' + "Due Date: ${task.dueDate}\n"
        return str
    }
}
