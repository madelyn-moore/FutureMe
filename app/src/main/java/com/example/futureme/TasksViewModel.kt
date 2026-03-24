package com.example.futureme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasksViewModel(val dao: TaskDao) : ViewModel() {

    val newTaskName = MutableLiveData("")
    val newTags = MutableLiveData("")
    val newDueDate = MutableLiveData("")
    val newDatePostponed = MutableLiveData("")
    val newDescription = MutableLiveData("")
    val newWhyTaskPushedOff = MutableLiveData("")
    val newPenalties = MutableLiveData("")
    val newNumDaysDelayed = MutableLiveData("0")
    val newOverdueStatus = MutableLiveData(false)
    val newDateCompleted = MutableLiveData("")

    val tasks = dao.getAllTasks()

    private var currentTaskId: Long? = null

    // Date formatter for MM/dd/yyyy
    // setLenient(false) makes invalid dates like 02/30/2026 fail
    private val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.US).apply {
        isLenient = false
    }

    val totalPostponedCount: LiveData<String> = tasks.map { list ->
        list.count { it.datePostponed.isNotEmpty() }.toString()
    }

    val averageDelay: LiveData<String> = tasks.map { list ->
        val delayedTasks = list.filter { it.numDaysDelayed.isNotEmpty() }
        if (delayedTasks.isEmpty()) {
            "0.0"
        } else {
            val totalDays = delayedTasks.sumOf { it.numDaysDelayed.toDoubleOrNull() ?: 0.0 }
            String.format("%.2f", totalDays / delayedTasks.size)
        }
    }

    val overdueCount: LiveData<String> = tasks.map { list ->
        list.count { it.overdueStatus }.toString()
    }

    val mostDeferredTaskName: LiveData<String> = tasks.map { list ->
        list.filter { it.numDaysDelayed.isNotEmpty() }
            .maxByOrNull { it.numDaysDelayed.toDoubleOrNull() ?: 0.0 }
            ?.name ?: "None"
    }

    val tasksString: LiveData<String> = tasks.map { taskList ->
        formatTasks(taskList)
    }

    // Safely parse a date string in MM/dd/yyyy format
    private fun parseDate(dateString: String): Date? {
        return try {
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    // Calculate delayed days using due date and postponed date
    private fun calculateDelayedDays(dueDate: String, postponedDate: String): String {
        if (dueDate.isBlank() || postponedDate.isBlank()) return "0"

        val due = parseDate(dueDate) ?: return "0"
        val postponed = parseDate(postponedDate) ?: return "0"

        val diffInMillis = postponed.time - due.time
        val daysBetween = diffInMillis / (1000 * 60 * 60 * 24)

        return if (daysBetween < 0) "0" else daysBetween.toString()
    }

    // Calculate overdue status using today's date
    private fun calculateOverdueStatus(dueDate: String): Boolean {
        if (dueDate.isBlank()) return false

        val due = parseDate(dueDate) ?: return false
        val today = Date()

        return today.after(due)
    }

    fun recalculateTaskStats() {
        val dueDateValue = newDueDate.value ?: ""
        val postponedDateValue = newDatePostponed.value ?: ""

        newNumDaysDelayed.value = calculateDelayedDays(dueDateValue, postponedDateValue)
        newOverdueStatus.value = calculateOverdueStatus(dueDateValue)
    }

    fun addTask() {
        viewModelScope.launch {
            val dueDateValue = newDueDate.value ?: ""
            val postponedDateValue = newDatePostponed.value ?: ""

            val calculatedDaysDelayed = calculateDelayedDays(dueDateValue, postponedDateValue)
            val calculatedOverdue = calculateOverdueStatus(dueDateValue)

            val task = Task(
                name = newTaskName.value ?: "",
                tags = newTags.value ?: "",
                dueDate = dueDateValue,
                datePostponed = postponedDateValue,
                description = newDescription.value ?: "",
                whyTaskPushedOff = newWhyTaskPushedOff.value ?: "",
                penalties = newPenalties.value ?: "",
                numDaysDelayed = calculatedDaysDelayed,
                overdueStatus = calculatedOverdue,
                dateCompleted = newDateCompleted.value ?: ""
            )

            dao.insert(task)
            clearFields()
        }
    }

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            val task = dao.getTaskById(taskId)
            task?.let {
                currentTaskId = it.taskId
                newTaskName.postValue(it.name)
                newTags.postValue(it.tags)
                newDueDate.postValue(it.dueDate)
                newDatePostponed.postValue(it.datePostponed)
                newDescription.postValue(it.description)
                newWhyTaskPushedOff.postValue(it.whyTaskPushedOff)
                newPenalties.postValue(it.penalties)
                newNumDaysDelayed.postValue(it.numDaysDelayed)
                newOverdueStatus.postValue(it.overdueStatus)
                newDateCompleted.postValue(it.dateCompleted)
            }
        }
    }

    fun updateTask() {
        val id = currentTaskId ?: return

        viewModelScope.launch {
            val dueDateValue = newDueDate.value ?: ""
            val postponedDateValue = newDatePostponed.value ?: ""

            val calculatedDaysDelayed = calculateDelayedDays(dueDateValue, postponedDateValue)
            val calculatedOverdue = calculateOverdueStatus(dueDateValue)

            val updatedTask = Task(
                taskId = id,
                name = newTaskName.value ?: "",
                tags = newTags.value ?: "",
                dueDate = dueDateValue,
                datePostponed = postponedDateValue,
                description = newDescription.value ?: "",
                whyTaskPushedOff = newWhyTaskPushedOff.value ?: "",
                penalties = newPenalties.value ?: "",
                numDaysDelayed = calculatedDaysDelayed,
                overdueStatus = calculatedOverdue,
                dateCompleted = newDateCompleted.value ?: ""
            )

            dao.update(updatedTask)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.delete(task)
        }
    }

    fun clearFields() {
        currentTaskId = null
        newTaskName.value = ""
        newTags.value = ""
        newDueDate.value = ""
        newDatePostponed.value = ""
        newDescription.value = ""
        newWhyTaskPushedOff.value = ""
        newPenalties.value = ""
        newNumDaysDelayed.value = "0"
        newOverdueStatus.value = false
        newDateCompleted.value = ""
    }

    fun formatTasks(tasks: List<Task>): String {
        return tasks.fold("") { str, item ->
            str + '\n' + formatTask(item)
        }
    }

    fun formatTask(task: Task): String {
        var str = "ID: ${task.taskId}"
        str += '\n' + "Name: ${task.name}"
        str += '\n' + "Tags: ${task.tags}"
        str += '\n' + "Due Date: ${task.dueDate}"
        str += '\n' + "Date Postponed: ${task.datePostponed}"
        str += '\n' + "Days Delayed: ${task.numDaysDelayed}"
        str += '\n' + "Overdue: ${if (task.overdueStatus) "Yes" else "No"}\n"
        return str
    }
}