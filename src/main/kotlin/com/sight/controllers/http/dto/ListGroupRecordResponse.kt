package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.sight.service.GroupRecordDetailsResult
import com.sight.service.GroupRecordResult
import com.sight.service.RecordFileResult
import java.time.Instant

data class GroupRecordFileResponse(
    val id: String,
    val fileId: String,
    val url: String?,
    val name: String,
    val size: Long,
    val contentType: String,
) {
    companion object {
        fun from(file: RecordFileResult) = GroupRecordFileResponse(file.id, file.fileId, file.url, file.name, file.size, file.contentType)
    }
}

data class ListGroupRecordResponse(
    val id: String,
    val type: String,
    val authorName: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isDeleted: Boolean,
    val isInPortfolio: Boolean,
    val isInUserArchive: Boolean,
    val files: List<GroupRecordFileResponse>,
) {
    companion object {
        fun from(record: GroupRecordResult) =
            ListGroupRecordResponse(
                record.id.toString(), record.type, record.authorName,
                record.content, record.createdAt, record.updatedAt, record.isDeleted, record.isInPortfolio,
                record.isInUserArchive, record.files.map(GroupRecordFileResponse::from),
            )
    }
}

data class GetGroupRecordResponse(
    @get:JsonUnwrapped val record: ListGroupRecordResponse,
    val cardId: String,
    val cardTitle: String,
    val listId: String,
    val listTitle: String,
) {
    companion object {
        fun from(result: GroupRecordDetailsResult) =
            GetGroupRecordResponse(
                ListGroupRecordResponse.from(result.record),
                result.cardId.toString(),
                result.cardTitle,
                result.listId.toString(),
                result.listTitle,
            )
    }
}
