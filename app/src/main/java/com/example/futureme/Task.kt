package com.example.futureme

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo



@Entity(tableName = "task_table")


data class Task (
    @PrimaryKey(autoGenerate = true)
    var taskId: Long = 0L,
    @ColumnInfo(name = "task_name")
    var name: String = "",
    @ColumnInfo(name = "tags")
    var tags: String = "",
    @ColumnInfo(name = "new_due_date")
    var dueDate: String = "",
    @ColumnInfo(name = "date_postponed")
    var datePostponed: String = "",
    @ColumnInfo(name = "description")
    var description: String = "",
    @ColumnInfo(name = "why_task_pushed_off")
    var whyTaskPushedOff: String = "",
    @ColumnInfo(name = "penalties")
    var penalties: String ="n/a",
    @ColumnInfo(name = "num_days_delayed")
    var numDaysDelayed: String = "",
    @ColumnInfo(name = "overdue_status")
    var overdueStatus: Boolean = false,
    @ColumnInfo(name = "date_completed")
    var dateCompleted: String = ""
)
