package com.sight.repository

import com.sight.config.QueryDslConfig
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordArchive
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig::class)
@Testcontainers(disabledWithoutDocker = true)
class GroupRecordPersistenceTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var archives: GroupRecordArchiveRepository

    @Test
    fun `보관 시각이 같은 기록도 페이지를 나누어 조회할 때 중복되거나 누락되지 않는다`() {
        entityManager.persist(testGroup(id = 1L))
        entityManager.persist(testGroup(id = 2L, status = GroupStatus.SUCCESS))
        val author = Member(5L, "writer", realname = "작성자", studentStatus = StudentStatus.UNDERGRADUATE, status = UserStatus.ACTIVE)
        entityManager.persist(author)
        val activeCard = GroupCard(3L, 1L, 10L, 5L, "카드", 1L, NOW)
        val deletedCard = GroupCard(4L, 2L, 20L, 5L, "삭제된 카드", 1L, NOW).apply { delete(NOW) }
        entityManager.persist(activeCard)
        entityManager.persist(deletedCard)
        for (id in 101L..111L) {
            val cardId = if (id == 111L) deletedCard.id else activeCard.id
            val record = GroupRecord(id, cardId, 5L, "본문 $id", NOW)
            if (id == 110L) record.delete(NOW)
            entityManager.persist(record)
            entityManager.persist(GroupRecordArchive(id + 100, 6L, id, if (id == 111L) 2L else 1L, NOW))
        }
        entityManager.persist(GroupRecord(112L, activeCard.id, 5L, "다른 회원 기록", NOW))
        entityManager.persist(GroupRecordArchive(212L, 7L, 112L, 1L, NOW.plusSeconds(1)))
        entityManager.flush()
        entityManager.clear()

        val first = archives.findSavedRecordsByMemberIdOrderBySavedAtDesc(6L, 0L, 10)
        val second = archives.findSavedRecordsByMemberIdOrderBySavedAtDesc(6L, 10L, 10)
        val allAtOnce = archives.findSavedRecordsByMemberIdOrderBySavedAtDesc(6L, 0L, 11)
        val allRecords = first + second
        val recordIds = allRecords.map { it.recordId }

        assertEquals(11L, archives.countByMemberId(6L))
        assertEquals(10, first.size)
        assertEquals(1, second.size)
        assertEquals(recordIds.size, recordIds.distinct().size)
        assertEquals((101L..111L).toSet(), recordIds.toSet())
        assertEquals(allAtOnce.map { it.recordId }, recordIds)
        assertTrue(archives.findSavedRecordsByMemberIdOrderBySavedAtDesc(6L, 20L, 10).isEmpty())
        val recordInDeletedCard = allRecords.single { it.recordId == 111L }
        assertEquals(2L, recordInDeletedCard.groupId)
        assertTrue(recordInDeletedCard.isCardDeleted)
        assertTrue(allRecords.single { it.recordId == 110L }.isDeleted)
        assertEquals("본문 101", allRecords.single { it.recordId == 101L }.content)
    }

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer<Nothing>("mysql:8.0.40")
    }
}
