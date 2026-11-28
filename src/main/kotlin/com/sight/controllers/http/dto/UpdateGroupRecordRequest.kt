package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode

data class UpdateGroupRecordRequest(
    val content: String,
    val files: List<RecordFileRequest>,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(node: JsonNode): UpdateGroupRecordRequest {
            val request = CreateGroupRecordRequest.from(node)
            return UpdateGroupRecordRequest(request.content, request.files)
        }
    }
}

data class UpdateGroupRecordResponse(val uploads: List<RecordFileUploadResponse>)
