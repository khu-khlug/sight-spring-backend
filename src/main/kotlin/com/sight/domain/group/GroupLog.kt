package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    id: Long,
    groupId: Long,
    actorMemberId: Long?,
    logType: GroupLogType,
    logData: String,
    createdAt: Instant,
) {
    // 로그 식별자. 레거시 로그의 아이디를 유지하며 신규 로그는 서비스가 발급한 TSID를 쓴다.
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = id

    // 로그가 속한 그룹.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 행위자 회원. 시스템이 남긴 로그면 null이다.
    @Column(name = "actor_member_id")
    val actorMemberId: Long? = actorMemberId

    // 로그타입. enum 이름으로 저장한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 64)
    val logType: GroupLogType = logType

    // 로그타입에 대응하는 로그데이터 JSON 객체. 필드는 로그타입별 GroupLogData가 정하며 부가 데이터가 없으면 빈 객체다.
    // 이름·제목 등은 발생 당시 텍스트 스냅샷이며 HTML·Markdown 링크를 넣지 않는다.
    @Column(name = "log_data", nullable = false, columnDefinition = "JSON")
    val logData: String = logData

    // 발생 일시.
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    init {
        require(id > 0) { "로그 식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        require(actorMemberId == null || actorMemberId > 0) { "회원 식별자가 올바르지 않습니다." }
        require(logData.isNotBlank()) { "로그데이터가 필요합니다." }
    }
}
