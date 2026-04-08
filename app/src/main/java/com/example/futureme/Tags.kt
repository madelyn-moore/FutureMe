package com.example.futureme

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// creating new table for a tag class
@Entity(tableName = "tags_table")
data class Tag(
    @PrimaryKey
    @ColumnInfo(name = "tag_name")
    val tagName: String ) {
    override fun toString(): String = tagName
}