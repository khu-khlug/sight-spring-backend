package com.sight.repository

import com.sight.config.QueryDslConfig
import com.sight.domain.group.GroupCard
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
class GroupCardRepositoryTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var cards: GroupCardRepository

    @Test
    fun `일반 조회는 같은 그룹의 정상 카드와 비활성 카드만 반환한다`() {
        val active = GroupCard(3L, 1L, 5L, 2L, "정상", 1L, NOW)
        val disabled = GroupCard(4L, 1L, 5L, 2L, "비활성", 2L, NOW).apply { changeDisabled(true, NOW) }
        val deleted = GroupCard(6L, 1L, 5L, 2L, "삭제", 3L, NOW).apply { delete(NOW) }
        listOf(active, disabled, deleted).forEach(entityManager::persist)
        entityManager.flush()
        entityManager.clear()

        assertEquals(active.id, cards.findAvailableByIdAndGroupId(active.id, 1L)?.id)
        assertEquals(true, cards.findAvailableByIdAndGroupId(disabled.id, 1L)?.isDisabled)
        assertNull(cards.findAvailableByIdAndGroupId(deleted.id, 1L))
        assertNull(cards.findAvailableByIdAndGroupId(active.id, 9L))
        assertNull(cards.findAvailableByIdAndGroupId(deleted.id, 9L))
        assertNull(cards.findAvailableByIdAndGroupId(999L, 1L))
        assertTrue(cards.existsDeletedByIdAndGroupId(deleted.id, 1L))
        assertFalse(cards.existsDeletedByIdAndGroupId(deleted.id, 9L))
        assertFalse(cards.existsDeletedByIdAndGroupId(active.id, 1L))
        assertFalse(cards.existsDeletedByIdAndGroupId(disabled.id, 1L))
        assertFalse(cards.existsDeletedByIdAndGroupId(999L, 1L))
    }

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer<Nothing>("mysql:8.0.40")
    }
}
