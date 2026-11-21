package com.sight.repository

import com.sight.config.QueryDslConfig
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupMember
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig::class)
@Testcontainers
class RecentGroupsRepositoryTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer<Nothing>("mysql:8.0")
    }

    @Autowired private lateinit var groups: GroupRepository

    @Autowired private lateinit var entityManager: EntityManager

    @Test
    fun `열람할 수 있는 그룹만 최근 활동 순으로 페이지를 나눈다`() {
        val regular = 1L
        val exchange = 2L
        val manager = 3L
        val leader = 4L
        for (id in listOf(regular, exchange, manager, leader)) {
            entityManager.persist(
                Member(
                    id = id,
                    name = "user$id",
                    studentStatus = if (id == exchange) StudentStatus.UNITED else StudentStatus.UNDERGRADUATE,
                    status = UserStatus.ACTIVE,
                    manager = id == manager,
                ),
            )
        }

        fun group(
            id: Long,
            grade: GroupAccessGrade,
            minute: Int,
            author: Long = leader,
        ) {
            entityManager.persist(
                Group(
                    id = id,
                    category = GroupCategory.PROJECT,
                    title = "그룹 $id",
                    author = author,
                    master = leader,
                    grade = grade,
                    changedAt = LocalDateTime.of(2026, 1, 1, 12, minute),
                ),
            )
        }
        group(10, GroupAccessGrade.ALL, 1)
        group(11, GroupAccessGrade.MEMBER, 2)
        group(12, GroupAccessGrade.MANAGER, 3)
        group(13, GroupAccessGrade.PRIVATE, 4)
        group(14, GroupAccessGrade.PRIVATE, 5, author = regular)
        group(15, GroupAccessGrade.PRIVATE, 5)
        entityManager.persist(GroupMember(group = 15, member = regular))
        entityManager.persist(GroupMember(group = 12, member = regular))
        entityManager.persist(GroupMember(group = 11, member = exchange))
        entityManager.flush()

        fun ids(
            requester: Long,
            offset: Int = 0,
            limit: Int = 6,
        ) = groups.findRecentVisibleGroups(offset, limit, requester).map { it.id }

        assertEquals(listOf(15L, 14L, 11L, 10L), ids(regular))
        assertEquals(listOf(14L, 11L), ids(regular, offset = 1, limit = 2))
        assertEquals(listOf(10L), ids(exchange))
        assertEquals(listOf(12L, 11L, 10L), ids(manager))
    }
}
