package com.example.futureme

enum class TaskStatusFilter {
    ALL,
    OVERDUE,
    COMPLETED,
    INCOMPLETE
}

enum class TaskSortOption {
    DUE_DATE_ASC,
    DUE_DATE_DESC,
    NAME_AZ,
    TAG_AZ
}