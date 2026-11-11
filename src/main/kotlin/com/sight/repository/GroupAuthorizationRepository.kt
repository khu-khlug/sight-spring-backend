package com.sight.repository

import com.sight.domain.group.GroupCategory
import com.sight.domain.member.StudentStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

data class GroupRecordAuthor(
    val groupId: Long,
    val authorMemberId: Long,
)

@Repository
class GroupAuthorizationRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // 복구 API도 작성자 권한을 판정해야 하므로 삭제된 기록·카드를 포함한다.
    // 삭제·비활성 상태에 따른 행동 제한은 해당 업무 서비스가 판단한다.
    fun findRecordAuthorById(recordId: Long): GroupRecordAuthor? =
        jdbcTemplate.query(
            """
            SELECT c.group_id, r.author_member_id
            FROM khlug_group_record r
            JOIN khlug_group_card c ON c.id = r.card_id
            WHERE r.id = ?
            """.trimIndent(),
            { rs, _ -> GroupRecordAuthor(rs.getLong("group_id"), rs.getLong("author_member_id")) },
            recordId,
        ).firstOrNull()

    fun isManageGroup(groupId: Long): Boolean =
        jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM khlug_group WHERE id = ? AND category = ?)",
            Boolean::class.java,
            groupId,
            GroupCategory.MANAGE.name,
        ) == true

    // 회원이 없으면 null을 반환해 판정기가 다른 권한 분기의 허용 여부를 먼저 판정할 수 있게 한다.
    fun isUnitedMember(memberId: Long): Boolean? =
        jdbcTemplate.query(
            "SELECT state FROM khlug_members WHERE id = ?",
            { rs, _ -> rs.getInt("state") == StudentStatus.UNITED.code },
            memberId,
        ).firstOrNull()
}
