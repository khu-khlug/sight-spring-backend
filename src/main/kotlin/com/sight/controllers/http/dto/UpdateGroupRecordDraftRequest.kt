package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException
import java.time.Instant

data class UpdateGroupRecordDraftRequest(
    val cardId: String,
    val recordId: String?,
    val content: String,
    val updatedAt: Instant,
    val files: List<RecordFileRequest>,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(node: JsonNode): UpdateGroupRecordDraftRequest {
            fun text(field: String): String =
                node[field]?.takeIf { it.isTextual }?.asText()
                    ?: throw BadRequestException("$field 문자열이 필요합니다.")

            fun identifier(value: String): String {
                if (!Regex("[1-9][0-9]{0,18}").matches(value) || value.toLongOrNull() == null) {
                    throw BadRequestException("식별자는 양의 Long 10진 문자열이어야 합니다.")
                }
                return value
            }
            val record = node["recordId"] ?: throw BadRequestException("recordId가 필요합니다.")
            val recordId =
                if (record.isNull) {
                    null
                } else {
                    if (!record.isTextual) throw BadRequestException("recordId는 문자열 또는 null이어야 합니다.")
                    identifier(record.asText())
                }
            val timestamp = text("updatedAt")
            val instant =
                try {
                    if (!timestamp.endsWith("Z")) throw IllegalArgumentException()
                    Instant.parse(timestamp)
                } catch (_: RuntimeException) {
                    throw BadRequestException("updatedAt은 UTC ISO 8601 Z 형식이어야 합니다.")
                }
            val files = readRecordFiles(node["files"])
            return UpdateGroupRecordDraftRequest(identifier(text("cardId")), recordId, text("content"), instant, files)
        }
    }
}

data class UpdateGroupRecordDraftResponse(val uploads: List<DraftRecordFileUploadResponse>)

data class DraftRecordFileUploadResponse(val id: String, val fileId: String, val url: String, val uploadUrl: String)
