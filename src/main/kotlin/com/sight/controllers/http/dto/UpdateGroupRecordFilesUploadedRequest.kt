package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class UpdateGroupRecordFilesUploadedRequest(val fileIds: List<String>) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(node: JsonNode): UpdateGroupRecordFilesUploadedRequest {
            val ids =
                node["fileIds"]?.takeIf { it.isArray && !it.isEmpty }
                    ?: throw BadRequestException("비어 있지 않은 fileIds 배열이 필요합니다.")
            return UpdateGroupRecordFilesUploadedRequest(
                ids.map {
                    if (!it.isTextual) throw BadRequestException("파일 아이디는 문자열이어야 합니다.")
                    it.asText()
                },
            )
        }
    }
}

data class UpdateGroupRecordFilesUploadedResponse(val results: List<GroupRecordFileVerificationResponse>)

data class GroupRecordFileVerificationResponse(
    val fileId: String,
    val isSuccess: Boolean,
    val reason: String?,
)
