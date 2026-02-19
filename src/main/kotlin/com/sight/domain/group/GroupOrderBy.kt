package com.sight.domain.group

enum class GroupOrderBy(val value: String) {
    CHANGED_AT("changedAt"),
    CREATED_AT("createdAt"),
    ;

    companion object {
        fun fromValue(value: String): GroupOrderBy? = entries.find { it.value == value }
    }
}
