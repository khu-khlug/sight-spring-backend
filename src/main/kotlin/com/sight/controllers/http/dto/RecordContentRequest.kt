package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class CreateGroupRecordRequest(
    val content: String,
    val files: List<RecordFileRequest>,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(node: JsonNode): CreateGroupRecordRequest {
            val content =
                node["content"]?.takeIf { it.isTextual }?.textValue()
                    ?: throw BadRequestException("기록 본문은 문자열이어야 합니다.")
            if (content.isEmpty() || content.toByteArray(Charsets.UTF_8).size > 1_048_576) {
                throw BadRequestException("기록 본문은 비어 있지 않은 1 MiB 이하 문자열이어야 합니다.")
            }
            return CreateGroupRecordRequest(content, readRecordFiles(node["files"]))
        }
    }
}

data class RecordFileRequest(
    val id: String,
    val fileId: String?,
    val name: String?,
    val size: Long?,
    val contentType: String?,
)

internal fun readRecordFiles(node: JsonNode?): List<RecordFileRequest> {
    if (node == null || !node.isArray) throw BadRequestException("files는 필수 배열입니다.")
    return node.map { file ->
        val id =
            file["id"]?.takeIf { it.isTextual }?.textValue()
                ?: throw BadRequestException("파일 식별자는 필수 문자열입니다.")
        val fileIdNode = file["fileId"] ?: throw BadRequestException("fileId는 필수 항목입니다.")
        val fileId =
            if (fileIdNode.isNull) {
                null
            } else {
                fileIdNode.takeIf { it.isTextual }?.textValue()
                    ?: throw BadRequestException("fileId는 문자열 또는 null이어야 합니다.")
            }
        val name = file["name"]?.takeIf { it.isTextual }?.textValue()
        val size = file["size"]?.takeIf { it.isIntegralNumber && it.canConvertToLong() }?.longValue()
        val contentType = file["contentType"]?.takeIf { it.isTextual }?.textValue()
        if (fileId == null && (name == null || size == null || contentType == null)) {
            throw BadRequestException("새 파일의 이름·크기·MIME 타입은 필수 항목입니다.")
        }
        RecordFileRequest(id, fileId, name, size, contentType)
    }
}

data class RecordFileUploadResponse(
    val id: String,
    val fileId: String,
    val url: String,
    val uploadUrl: String,
)
