package com.example.futureme

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query

@Dao
interface TaskDao {
        @Insert
        suspend fun insert(task: Task)

        @Update
        suspend fun update(task: Task)

        @Delete
        suspend fun delete(task: Task)

        @Query("SELECT * FROM task_table WHERE taskId = :taskId")
        fun get(taskId: Long): LiveData<Task>

        @Query("SELECT * FROM task_table ORDER BY taskId DESC")
        fun getAllTasks(): LiveData<List<Task>>
}
