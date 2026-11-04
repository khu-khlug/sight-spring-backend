package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

// 그룹 활동 로그. 이름·제목 등은 발생 당시 값으로 저장하며 대상이 바뀌거나 삭제돼도 덮어쓰지 않는다.
@Entity
// 인덱스(group_id, created_at, id): 그룹 로그를 최신순으로 조회.
@Table(
    name = "khlug_group_log",
    indexes = [Index(columnList = "group_id, created_at, id")],
)
class GroupLog(
    groupId: Long,
    actorMemberId: Long?,
    actorName: String?,
    logType: GroupLogType,
    logData: String,
    createdAt: Instant,
) {
    // 로그 식별자.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 로그가 속한 그룹.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 행위자 회원. 시스템이 남긴 로그면 null이다.
    @Column(name = "actor_member_id")
    val actorMemberId: Long? = actorMemberId

    // 발생 당시 행위자 실명. 회원이 탈퇴해도 이 이름을 반환한다. 시스템 로그면 null이다.
    @Column(name = "actor_name", length = 255)
    val actorName: String? = actorName

    // 로그타입. enum 이름으로 저장한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 64)
    val logType: GroupLogType = logType

    // 로그타입에 대응하는 로그데이터 JSON 객체. 필수 필드는 로그타입의 requiredFields이며 부가 데이터가 없으면 빈 객체다.
    // 이름·제목 등은 발생 당시 텍스트 스냅샷이며 HTML·Markdown 링크를 넣지 않는다.
    @Column(name = "log_data", nullable = false, columnDefinition = "JSON")
    val logData: String = logData

    // 발생 일시.
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    init {
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        require((actorMemberId == null) == (actorName == null)) { "행위자 아이디와 이름은 함께 있어야 합니다." }
        require(actorMemberId == null || actorMemberId > 0) { "회원 식별자가 올바르지 않습니다." }
        require(logData.isNotBlank()) { "로그데이터가 필요합니다." }
    }
}
