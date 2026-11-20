package com.sight.controllers.http.dto

data class ListGroupSearchResultsResponse(
    val page: Int,
    val totalPages: Long,
    val receivedCount: Int,
    val results: List<ListGroupSearchResultResponse>,
)

data class ListGroupSearchResultResponse(
    val listId: String,
    val listTitle: String,
    val cardId: String,
    val cardTitle: String,
    val recordId: String?,
    val matchedText: String?,
)
