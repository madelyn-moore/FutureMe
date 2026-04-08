package com.example.futureme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TasksViewModel(private val dao: TaskDao, private val tagDao: TagDAO) : ViewModel() {

    // =========================================
    // DATABASE
    // =========================================
    val allTasks: LiveData<List<Task>> = dao.getAllTasks()
    val allTags: LiveData<List<Tag>> = tagDao.getAllTags()

    private var currentTaskId: Long = 0L

    // =========================================
    // ADD / EDIT TASK FIELDS
    // =========================================
    val newTaskName = MutableLiveData("")
    val newTaskTags = MutableLiveData("")
    val newTaskDueDate = MutableLiveData("")
    val newTaskDatePostponed = MutableLiveData("")
    val newTaskDescription = MutableLiveData("")
    val newTaskWhyPushedOff = MutableLiveData("")
    val newTaskPenalties = MutableLiveData("n/a")
    val newTaskDateCompleted = MutableLiveData("")
    val newTaskNumDaysDelayed = MutableLiveData("0")
    val newTaskOverdueStatus = MutableLiveData(false)

    // Tag Selection
    val selectedTag = MutableLiveData<Tag?>(null)
    val tagNameInput = MutableLiveData("")

    // =========================================
    // XML BINDING ALIASES
    // =========================================
    val newName = newTaskName
    val newTags = newTaskTags
    val newDueDate = newTaskDueDate
    val newDatePostponed = newTaskDatePostponed
    val newDescription = newTaskDescription
    val newWhyTaskPushedOff = newTaskWhyPushedOff
    val newWhyPushedOff = newTaskWhyPushedOff
    val newPenalties = newTaskPenalties
    val newDateCompleted = newTaskDateCompleted
    val newNumDaysDelayed = newTaskNumDaysDelayed
    val newOverdueStatus = newTaskOverdueStatus

    // =========================================
    // SEARCH + FILTERS
    // =========================================
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _statusFilter = MutableLiveData(TaskStatusFilter.ALL)
    val statusFilter: LiveData<TaskStatusFilter> = _statusFilter

    private val _sortOption = MutableLiveData(TaskSortOption.DUE_DATE_ASC)
    val sortOption: LiveData<TaskSortOption> = _sortOption

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query.trim()
    }

    fun updateStatusFilter(filter: TaskStatusFilter) {
        _statusFilter.value = filter
    }

    fun updateSortOption(sort: TaskSortOption) {
        _sortOption.value = sort
    }

    // =========================================
    // FILTERED TASK LIST
    // =========================================
    val filteredTasks = MediatorLiveData<List<Task>>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            val query = _searchQuery.value ?: ""
            val status = _statusFilter.value ?: TaskStatusFilter.ALL
            val sort = _sortOption.value ?: TaskSortOption.DUE_DATE_ASC
            value = applyFilters(tasks, query, status, sort)
        }

        addSource(allTasks) { update() }
        addSource(_searchQuery) { update() }
        addSource(_statusFilter) { update() }
        addSource(_sortOption) { update() }
    }

    private fun applyFilters(
        tasks: List<Task>,
        query: String,
        status: TaskStatusFilter,
        sort: TaskSortOption
    ): List<Task> {
        val filtered = tasks.filter { task ->
            val matchesSearch =
                task.name.contains(query, ignoreCase = true) ||
                        task.tags.contains(query, ignoreCase = true) ||
                        task.description.contains(query, ignoreCase = true) ||
                        task.dueDate.contains(query, ignoreCase = true) ||
                        task.datePostponed.contains(query, ignoreCase = true) ||
                        task.whyTaskPushedOff.contains(query, ignoreCase = true) ||
                        task.penalties.contains(query, ignoreCase = true) ||
                        task.dateCompleted.contains(query, ignoreCase = true) ||
                        task.numDaysDelayed.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                TaskStatusFilter.ALL -> true
                TaskStatusFilter.OVERDUE -> calculateOverdue(task) && !task.isCompleted
                TaskStatusFilter.COMPLETED -> task.isCompleted
                TaskStatusFilter.INCOMPLETE -> !task.isCompleted
            }

            matchesSearch && matchesStatus
        }

        return when (sort) {
            TaskSortOption.DUE_DATE_ASC ->
                filtered.sortedBy { parseDate(it.dueDate)?.time ?: Long.MAX_VALUE }

            TaskSortOption.DUE_DATE_DESC ->
                filtered.sortedByDescending { parseDate(it.dueDate)?.time ?: Long.MIN_VALUE }

            TaskSortOption.NAME_AZ ->
                filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }

            TaskSortOption.TAG_AZ ->
                filtered.sortedBy { it.tags.lowercase(Locale.getDefault()) }
        }
    }

    // =========================================
    // ADD / EDIT SUPPORT
    // =========================================
    fun loadTask(task: Task) {
        currentTaskId = task.taskId
        newTaskName.value = task.name
        newTaskTags.value = task.tags
        newTaskDueDate.value = task.dueDate
        newTaskDatePostponed.value = task.datePostponed
        newTaskDescription.value = task.description
        newTaskWhyPushedOff.value = task.whyTaskPushedOff
        newTaskPenalties.value = task.penalties
        newTaskDateCompleted.value = task.dateCompleted
        newTaskNumDaysDelayed.value = task.numDaysDelayed
        newTaskOverdueStatus.value = task.overdueStatus
        
        // Try to match tag in spinner
        selectedTag.value = allTags.value?.find { it.tagName == task.tags }
    }

    fun recalculateTaskStats() {
        val dueDate = newTaskDueDate.value?.trim() ?: ""
        val completedDate = newTaskDateCompleted.value?.trim() ?: ""

        val overdue = if (completedDate.isNotBlank()) false else calculateOverdueFromString(dueDate)
        val delayedDays = calculateDelayedDaysString(dueDate)

        newTaskOverdueStatus.value = overdue
        newTaskNumDaysDelayed.value = delayedDays
    }

    fun addTask() {
        val name = newTaskName.value?.trim() ?: ""
        // Crucial fix: Pull tag name from selectedTag
        val tags = selectedTag.value?.tagName ?: ""
        val dueDate = newTaskDueDate.value?.trim() ?: ""
        val datePostponed = newTaskDatePostponed.value?.trim() ?: ""
        val description = newTaskDescription.value?.trim() ?: ""
        val whyPushedOff = newTaskWhyPushedOff.value?.trim() ?: ""
        val penalties = newTaskPenalties.value?.trim()?.ifBlank { "n/a" } ?: "n/a"
        val dateCompleted = newTaskDateCompleted.value?.trim() ?: ""

        if (name.isBlank() || dueDate.isBlank()) return

        recalculateTaskStats()

        val task = Task(
            name = name,
            tags = tags,
            dueDate = dueDate,
            datePostponed = datePostponed,
            description = description,
            whyTaskPushedOff = whyPushedOff,
            penalties = penalties,
            numDaysDelayed = newTaskNumDaysDelayed.value ?: "0",
            overdueStatus = newTaskOverdueStatus.value ?: false,
            dateCompleted = dateCompleted,
            totalDeferrals = if (datePostponed.isNotBlank()) 1 else 0,
            isCompleted = dateCompleted.isNotBlank()
        )

        viewModelScope.launch {
            dao.insert(task)
        }

        clearTaskFields()
    }

    fun updateTask() {
        val name = newTaskName.value?.trim() ?: ""
        val tags = selectedTag.value?.tagName ?: ""
        val dueDate = newTaskDueDate.value?.trim() ?: ""
        val datePostponed = newTaskDatePostponed.value?.trim() ?: ""
        val description = newTaskDescription.value?.trim() ?: ""
        val whyPushedOff = newTaskWhyPushedOff.value?.trim() ?: ""
        val penalties = newTaskPenalties.value?.trim()?.ifBlank { "n/a" } ?: "n/a"
        val dateCompleted = newTaskDateCompleted.value?.trim() ?: ""

        if (name.isBlank() || dueDate.isBlank()) return

        recalculateTaskStats()

        val originalTask = allTasks.value?.find { it.taskId == currentTaskId }
        val deferrals = originalTask?.totalDeferrals ?: if (datePostponed.isNotBlank()) 1 else 0

        val updatedTask = Task(
            taskId = currentTaskId,
            name = name,
            tags = tags,
            dueDate = dueDate,
            datePostponed = datePostponed,
            description = description,
            whyTaskPushedOff = whyPushedOff,
            penalties = penalties,
            numDaysDelayed = newTaskNumDaysDelayed.value ?: "0",
            overdueStatus = newTaskOverdueStatus.value ?: false,
            dateCompleted = dateCompleted,
            totalDeferrals = deferrals,
            isCompleted = dateCompleted.isNotBlank()
        )

        viewModelScope.launch {
            dao.update(updatedTask)
        }
    }

    fun updateTask(task: Task) {
        val updatedTask = task.copy(
            overdueStatus = calculateOverdue(task),
            numDaysDelayed = calculateDelayedDaysString(task.dueDate),
            isCompleted = task.dateCompleted.isNotBlank() || task.isCompleted
        )

        viewModelScope.launch {
            dao.update(updatedTask)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.delete(task)
        }
    }

    fun postponeTask(task: Task, newDueDate: String, postponedDate: String, reason: String = "") {
        val updatedTask = task.copy(
            dueDate = newDueDate,
            datePostponed = postponedDate,
            whyTaskPushedOff = if (reason.isBlank()) task.whyTaskPushedOff else reason,
            totalDeferrals = task.totalDeferrals + 1,
            overdueStatus = calculateOverdueFromString(newDueDate),
            numDaysDelayed = calculateDelayedDaysString(newDueDate)
        )

        viewModelScope.launch {
            dao.update(updatedTask)
        }
    }

    fun markTaskCompleted(task: Task, completedDate: String) {
        val updatedTask = task.copy(
            dateCompleted = completedDate,
            isCompleted = true,
            overdueStatus = false
        )

        viewModelScope.launch {
            dao.update(updatedTask)
        }
    }

    fun markTaskIncomplete(task: Task) {
        val updatedTask = task.copy(
            dateCompleted = "",
            isCompleted = false,
            overdueStatus = calculateOverdue(task)
        )

        viewModelScope.launch {
            dao.update(updatedTask)
        }
    }

    fun refreshAllTaskStatuses() {
        val tasks = allTasks.value ?: return

        viewModelScope.launch {
            tasks.forEach { task ->
                val updatedTask = task.copy(
                    overdueStatus = calculateOverdue(task),
                    numDaysDelayed = calculateDelayedDaysString(task.dueDate)
                )
                dao.update(updatedTask)
            }
        }
    }

    // =========================================
    // ANALYTICS
    // =========================================
    val postponedTaskCount = MediatorLiveData<Int>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = tasks.count { it.totalDeferrals > 0 }
        }
        addSource(allTasks) { update() }
    }

    val totalDeferrals = MediatorLiveData<Int>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = tasks.sumOf { it.totalDeferrals }
        }
        addSource(allTasks) { update() }
    }

    val overdueTaskCount = MediatorLiveData<Int>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = tasks.count { calculateOverdue(it) && !it.isCompleted }
        }
        addSource(allTasks) { update() }
    }

    val overdueCount = overdueTaskCount

    val completedTaskCount = MediatorLiveData<Int>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = tasks.count { it.isCompleted }
        }
        addSource(allTasks) { update() }
    }

    val incompleteTaskCount = MediatorLiveData<Int>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = tasks.count { !it.isCompleted }
        }
        addSource(allTasks) { update() }
    }

    val averageDelay = MediatorLiveData<Double>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            val validDelays = tasks.mapNotNull { it.numDaysDelayed.toDoubleOrNull() }
            value = if (validDelays.isEmpty()) 0.0 else validDelays.average()
        }
        addSource(allTasks) { update() }
    }

    val mostDeferredTask = MediatorLiveData<Task?>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = tasks.maxByOrNull { it.totalDeferrals }
        }
        addSource(allTasks) { update() }
    }

    val mostDeferredTaskName = MediatorLiveData<String>().apply {
        fun update() {
            value = mostDeferredTask.value?.name ?: "None"
        }
        addSource(mostDeferredTask) { update() }
    }

    val mostDeferredTaskDeferrals = MediatorLiveData<Int>().apply {
        fun update() {
            value = mostDeferredTask.value?.totalDeferrals ?: 0
        }
        addSource(mostDeferredTask) { update() }
    }

    val futureBurdenScore = MediatorLiveData<Int>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = tasks.sumOf { task ->
                val overdueWeight = if (calculateOverdue(task) && !task.isCompleted) 3 else 0
                val deferralWeight = task.totalDeferrals * 2
                val delayWeight = task.numDaysDelayed.toIntOrNull() ?: 0
                overdueWeight + deferralWeight + delayWeight
            }
        }
        addSource(allTasks) { update() }
    }

    val weeklyAverageDelay = MediatorLiveData<List<Float>>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = listOf(
                averageDelayForDay(tasks, 0),
                averageDelayForDay(tasks, 1),
                averageDelayForDay(tasks, 2),
                averageDelayForDay(tasks, 3),
                averageDelayForDay(tasks, 4),
                averageDelayForDay(tasks, 5),
                averageDelayForDay(tasks, 6)
            )
        }
        addSource(allTasks) { update() }
    }

    val procrastinationBreakdown = MediatorLiveData<Map<String, Int>>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = mapOf(
                "Overdue" to tasks.count { calculateOverdue(it) && !it.isCompleted },
                "Completed" to tasks.count { it.isCompleted },
                "Deferred" to tasks.count { it.totalDeferrals > 0 },
                "On Time" to tasks.count { !calculateOverdue(it) && !it.isCompleted }
            )
        }
        addSource(allTasks) { update() }
    }

    val overdueByTag = MediatorLiveData<Map<String, Int>>().apply {
        fun update() {
            val tasks = allTasks.value ?: emptyList()
            value = tasks
                .filter { calculateOverdue(it) && !it.isCompleted }
                .groupBy { if (it.tags.isBlank()) "No Tag" else it.tags }
                .mapValues { entry -> entry.value.size }
        }
        addSource(allTasks) { update() }
    }

    // =========================================
    // HELPERS
    // =========================================
    private fun clearTaskFields() {
        currentTaskId = 0L
        newTaskName.value = ""
        newTaskTags.value = ""
        newTaskDueDate.value = ""
        newTaskDatePostponed.value = ""
        newTaskDescription.value = ""
        newTaskWhyPushedOff.value = ""
        newTaskPenalties.value = "n/a"
        newTaskDateCompleted.value = ""
        newTaskNumDaysDelayed.value = "0"
        newTaskOverdueStatus.value = false
        selectedTag.value = null
    }

    private fun calculateOverdue(task: Task): Boolean {
        if (task.isCompleted) return false
        return calculateOverdueFromString(task.dueDate)
    }

    private fun calculateOverdueFromString(dueDate: String): Boolean {
        val due = parseDate(dueDate) ?: return false
        return stripTime(due).before(stripTime(Date()))
    }

    private fun calculateDelayedDaysString(dueDate: String): String {
        val due = parseDate(dueDate) ?: return "0"
        val today = stripTime(Date())
        val cleanDue = stripTime(due)

        val diffMillis = today.time - cleanDue.time
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return if (diffDays > 0) diffDays.toString() else "0"
    }

    private fun averageDelayForDay(tasks: List<Task>, dayIndex: Int): Float {
        val filtered = tasks.filter { task ->
            val postponedDate = parseDate(task.datePostponed) ?: return@filter false
            val calendar = Calendar.getInstance()
            calendar.time = postponedDate

            val mappedDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> 6
            }

            mappedDay == dayIndex
        }

        val delays = filtered.mapNotNull { it.numDaysDelayed.toFloatOrNull() }
        return if (delays.isEmpty()) 0f else delays.average().toFloat()
    }

    private fun stripTime(date: Date): Date {
        val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return formatter.parse(formatter.format(date)) ?: date
    }

    private fun parseDate(dateString: String): Date? {
        val formats = listOf(
            "MM/dd/yyyy",
            "M/d/yyyy",
            "yyyy-MM-dd"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(dateString)
            } catch (_: Exception) {
            }
        }
        return null
    }

    // =========================================
    // Tags
    // =========================================
    fun addTag() {
        val name = tagNameInput.value?.trim() ?: ""
        if (name.isNotEmpty()) {
            viewModelScope.launch {
                tagDao.insert(Tag(tagName = name))
                tagNameInput.value = ""
            }
        }
    }

    val tagTOBeDeleted = MutableLiveData<Tag?>(null)
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagDao.delete(tag)
        }
    }
}
