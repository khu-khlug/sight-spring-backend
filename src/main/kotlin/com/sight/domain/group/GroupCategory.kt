package com.sight.domain.group

enum class GroupCategory(val value: String) {
    STUDY("study"),
    PROJECT("project"),
    MANAGE("manage"),
    DOCUMENTATION("documentation"),
    PROGRAM("program"),
    EDUCATION("education"),
    ;

    companion object {
        fun fromValue(value: String): GroupCategory? = entries.find { it.value == value }
    }
}
