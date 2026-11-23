package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException
import com.sight.core.id.IntegerId
import java.time.Instant

data class UpdateGroupRecordDraftRequest(
    @field:IntegerId
    val cardId: String,
    @field:IntegerId
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

            val record = node["recordId"] ?: throw BadRequestException("recordId가 필요합니다.")
            val recordId =
                if (record.isNull) {
                    null
                } else {
                    if (!record.isTextual) throw BadRequestException("recordId는 문자열 또는 null이어야 합니다.")
                    record.asText()
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
            return UpdateGroupRecordDraftRequest(text("cardId"), recordId, text("content"), instant, files)
        }
    }
}

data class UpdateGroupRecordDraftResponse(val uploads: List<DraftRecordFileUploadResponse>)

data class DraftRecordFileUploadResponse(val id: String, val fileId: String, val url: String, val uploadUrl: String)
