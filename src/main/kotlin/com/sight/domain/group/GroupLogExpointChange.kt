package com.sight.domain.group

// 그룹 로그를 남길 때 함께 기록할 경험치 변동. 대상 회원 모두에게 같은 양을 적용한다.
// targetId는 변동을 일으킨 대상의 식별자이며, 같은 대상의 지급과 되돌리기는 같은 값으로 남긴다.
data class GroupLogExpointChange(
    val targetId: String,
    val point: Int,
    val memberIds: List<Long>,
) {
    init {
        require(targetId.isNotBlank()) { "경험치 변동 대상이 필요합니다." }
        require(point != 0) { "경험치 변동량이 올바르지 않습니다." }
        require(memberIds.isNotEmpty()) { "경험치 변동 대상 회원이 필요합니다." }
        require(memberIds.all { it > 0 }) { "회원 식별자가 올바르지 않습니다." }
        require(memberIds.distinct().size == memberIds.size) { "경험치 변동 대상 회원이 중복되었습니다." }
    }
}
