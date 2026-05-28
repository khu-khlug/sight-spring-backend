package com.sight.core.info21

import com.fasterxml.jackson.annotation.JsonProperty

interface Info21AuthClient {
    fun authenticate(request: Info21AuthRequest): StuauthResponse
}

data class Info21AuthRequest(
    // Info21 아이디
    val info21Id: String,
    // Info21 비밀번호
    val info21Password: String,
)

data class StuauthResponse(
    // 응답 코드
    val code: Int,
    // 응답 메시지
    val message: String,
    // 학생 인증 정보
    val data: StuauthData,
)

data class StuauthData(
    // 학번
    @field:JsonProperty("id")
    val studentNumber: Int,
    // 이름
    val name: String,
    // 학년
    val grade: Int,
    // 전공
    val major: List<StuauthMajor>,
    // 전화번호
    val phone: String,
)

data class StuauthMajor(
    // 단과대학
    val college: String,
    // 학과
    val department: String,
    // 세부 전공
    val major: String? = null,
)
