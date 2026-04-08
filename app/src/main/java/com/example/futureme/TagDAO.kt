package com.example.futureme

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query


@Dao
interface TagDAO {
    @Query("SELECT * from tags_table ORDER BY tag_name ASC")
        fun getAllTags(): LiveData<List<Tag>>

    @Insert
    suspend fun insert(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

}