package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

// 그룹 로그를 남긴 행위로 한 회원에게 적용한 경험치 변동. 그룹·로그타입·발생 일시는 group_log_id로 이은 그룹 로그를 따른다.
// 행위를 취소할 때 당시 적용한 회원과 양 그대로 되돌리기 위해 보관한다.
@Entity
// 인덱스(target_id): 대상의 경험치 변동 조회. 인덱스(group_log_id): 같은 행위로 적용한 변동 조회.
@Table(
    name = "khlug_group_expoint_change",
    indexes = [Index(columnList = "target_id"), Index(columnList = "group_log_id")],
)
class GroupExpointChange(
    id: Long,
    groupLogId: Long,
    targetId: String,
    memberId: Long,
    point: Int,
) {
    // 변동 행의 식별자.
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = id

    // 변동을 일으킨 행위의 그룹 로그.
    @Column(name = "group_log_id", nullable = false)
    val groupLogId: Long = groupLogId

    // 변동을 일으킨 대상의 식별자. 그룹·리스트·카드·기록은 전역 채번이고 활동보고는 ULID라 종류가 달라도 겹치지 않는다.
    // 같은 대상의 지급과 되돌리기는 같은 값으로 남긴다.
    @Column(name = "target_id", nullable = false, length = TARGET_ID_MAX_LENGTH)
    val targetId: String = targetId

    // 변동을 받은 회원.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 이 회원에게 적용한 변동량.
    @Column(name = "point", nullable = false)
    val point: Int = point

    init {
        require(id > 0) { "식별자가 올바르지 않습니다." }
        require(groupLogId > 0) { "로그 식별자가 올바르지 않습니다." }
        require(targetId.isNotBlank() && targetId.length <= TARGET_ID_MAX_LENGTH) { "경험치 변동 대상이 올바르지 않습니다." }
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
        require(point != 0) { "경험치 변동량이 올바르지 않습니다." }
    }

    companion object {
        const val TARGET_ID_MAX_LENGTH: Int = 100
    }
}
