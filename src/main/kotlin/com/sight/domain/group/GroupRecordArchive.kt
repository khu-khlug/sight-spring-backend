package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

// 회원의 기록 개인 보관. 원본을 복제하지 않고 회원과 기록의 연결만 저장한다.
@Entity
// 유니크(member_id, record_id): 같은 기록 중복 보관 방지, 회원별 보관 조회.
// 보관 일시 인덱스 없음: 회원당 보관 수가 적어 좁힌 뒤 정렬해도 충분하다.
@Table(
    name = "khlug_group_record_archive",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "record_id"])],
)
class GroupRecordArchive(
    memberId: Long,
    recordId: Long,
    groupId: Long,
    savedAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 보관한 회원. 기록과 함께 유일해 같은 기록을 두 번 보관하지 않는다.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 보관한 기록. 기록이 다른 카드로 옮겨도 같은 기록을 가리킨다.
    @Column(name = "record_id", nullable = false)
    val recordId: Long = recordId

    // 기록이 속한 그룹. 기록은 같은 그룹 안에서만 옮겨지므로 바뀌지 않으며, 그룹별 보관 목록 조회에 쓴다.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 보관 일시. 보관 목록의 정렬 기준이며 원본이 삭제됐다가 복구돼도 유지된다.
    @Column(name = "saved_at", nullable = false)
    val savedAt: Instant = savedAt

    init {
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
        require(recordId > 0) { "기록 식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
    }
}
