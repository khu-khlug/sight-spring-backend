package com.sight.controllers.http.dto

import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupInterest
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// 그룹 정보를 모두 덮어쓰는 요청. 길이·개수·중복·형식 검사는 그룹 도메인이 한다.
data class UpdateGroupRequest(
    @field:NotBlank(message = "그룹 이름은 필수입니다")
    val title: String?,
    val description: String?,
    @field:NotNull(message = "그룹 유형은 필수입니다")
    val category: GroupCategory?,
    val interests: List<GroupInterest>?,
    // 쉼표로 구분한 사용 기술. 빈 문자열 또는 null은 사용 기술 없음이다.
    val techStack: String?,
    // LF 줄바꿈으로 구분한 저장소 URL. 빈 문자열 또는 null은 저장소 없음이다.
    val repositoryUrls: String?,
    @field:NotNull(message = "참여 신청 가능 여부는 필수입니다")
    val allowJoin: Boolean?,
    @field:NotNull(message = "공개 범위는 필수입니다")
    val visibility: GroupVisibility?,
)
