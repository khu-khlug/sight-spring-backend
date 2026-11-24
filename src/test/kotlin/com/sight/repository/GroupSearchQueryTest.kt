package com.sight.repository

import com.sight.config.QueryDslConfig
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupKanbanList
import com.sight.domain.group.GroupRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.text.Normalizer
import java.time.Instant

// 실제 MySQL에서 검색용 조회(카드 파생 쿼리, 기록 조인 쿼리)의 삭제 제외·정렬·페이징을 검증한다.
// Docker를 쓸 수 없는 환경에서는 다른 테스트가 실패하지 않도록 이 테스트만 건너뛴다.
// 기존 DB 테스트와 같이 테이블은 엔티티로 만들고(create-drop) SQL 초기화 스크립트는 실행하지 않는다(spring.sql.init.mode=never).
@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(QueryDslConfig::class, GroupSearchRepositoryImpl::class)
class GroupSearchQueryTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var groupCardRepository: GroupCardRepository

    @Autowired
    private lateinit var groupKanbanListRepository: GroupKanbanListRepository

    @Autowired
    private lateinit var searchRepository: GroupSearchRepositoryImpl

    private fun at(second: Int): Instant = Instant.parse("2025-01-01T00:00:00Z").plusSeconds(second.toLong())

    private fun list(
        id: Long,
        groupId: Long = GROUP_ID,
        title: String = "할 일",
    ) = entityManager.persist(GroupKanbanList(id = id, groupId = groupId, title = title, position = id, createdAt = at(0)))

    private fun card(
        id: Long,
        title: String,
        groupId: Long = GROUP_ID,
        listId: Long = LIST_ID,
        createdAt: Instant = at(0),
    ) = entityManager.persist(
        GroupCard(
            id = id,
            groupId = groupId,
            listId = listId,
            authorMemberId = AUTHOR_ID,
            title = title,
            position = id,
            createdAt = createdAt,
        ),
    )

    private fun record(
        id: Long,
        cardId: Long,
        createdAt: Instant = at(0),
    ) = entityManager.persist(GroupRecord(id = id, cardId = cardId, authorMemberId = AUTHOR_ID, content = "본문$id", createdAt = createdAt))

    private fun commit() {
        entityManager.flush()
        entityManager.clear()
    }

    private fun setUpBase() {
        list(LIST_ID)
        list(OTHER_GROUP_LIST_ID, groupId = OTHER_GROUP_ID, title = "다른 그룹")
    }

    private fun cards(
        pageIndex: Int = 0,
        size: Int = 50,
    ): List<GroupCard> =
        groupCardRepository.findByGroupIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(GROUP_ID, PageRequest.of(pageIndex, size))

    private fun cardIds(
        pageIndex: Int = 0,
        size: Int = 50,
    ): List<Long> = cards(pageIndex, size).map { it.id }

    @Test
    fun `카드 조회는 삭제된 카드와 다른 그룹의 카드를 제외하고 비활성 카드는 포함한다`() {
        setUpBase()
        card(1, "카드1")
        card(2, "삭제된 카드").also { it.delete(at(5)) }
        card(3, "비활성 카드").also { it.changeDisabled(true, at(5)) }
        card(4, "다른 그룹 카드", groupId = OTHER_GROUP_ID, listId = OTHER_GROUP_LIST_ID)
        commit()

        assertEquals(setOf(1L, 3L), cardIds().toSet())
    }

    @Test
    fun `카드 수정 시각 내림차순으로 정렬하고 같으면 아이디 내림차순이다`() {
        setUpBase()
        card(1, "정렬 가", createdAt = at(10))
        card(2, "정렬 나", createdAt = at(30))
        card(3, "정렬 다", createdAt = at(30))
        card(4, "정렬 라", createdAt = at(20))
        commit()

        assertEquals(listOf(3L, 2L, 4L, 1L), cardIds())
    }

    @Test
    fun `기록 추가로 카드 수정 시각이 바뀌면 카드 정렬에 반영된다`() {
        setUpBase()
        card(1, "정렬 가", createdAt = at(10))
        card(2, "정렬 나", createdAt = at(20))
        commit()
        assertEquals(listOf(2L, 1L), cardIds())

        entityManager.find(GroupCard::class.java, 1L).increaseRecordCount(at(100))
        commit()

        assertEquals(listOf(1L, 2L), cardIds())
    }

    @Test
    fun `카드 페이지는 페이지 번호와 크기로 나눈다`() {
        setUpBase()
        (1L..5L).forEach { card(it, "페이지 $it", createdAt = at(it.toInt())) }
        commit()

        assertEquals(listOf(5L, 4L), cardIds(pageIndex = 0, size = 2))
        assertEquals(listOf(3L, 2L), cardIds(pageIndex = 1, size = 2))
        assertEquals(listOf(1L), cardIds(pageIndex = 2, size = 2))
        assertEquals(emptyList<Long>(), cardIds(pageIndex = 3, size = 2))
    }

    @Test
    fun `카드와 리스트는 아이디로 객체를 읽을 수 있다`() {
        setUpBase()
        card(1, "스프링 스터디")
        commit()

        val card = cards().single()
        val list = groupKanbanListRepository.findAllById(listOf(card.listId!!)).single()

        assertEquals("스프링 스터디", card.title)
        assertEquals(AUTHOR_ID, card.authorMemberId)
        assertEquals("할 일", list.title)
    }

    @Test
    fun `이모지와 풀어쓴 한글 악센트 분리 문자는 저장·조회해도 그대로 돌아온다`() {
        setUpBase()
        val titles = listOf("스프링 😀", Normalizer.normalize("한글 café", Normalizer.Form.NFD), "ＣＡＦＥ")
        titles.forEachIndexed { index, title -> card(index + 1L, title, createdAt = at(index)) }
        commit()

        val stored = cards().sortedBy { it.id }.map { it.title }

        assertEquals(titles, stored)
    }

    @Test
    fun `기록 조회는 삭제된 기록·삭제된 카드의 기록·다른 그룹의 기록을 제외하고 비활성 카드의 기록은 포함한다`() {
        setUpBase()
        card(1, "카드1")
        card(2, "삭제된 카드").also { it.delete(at(5)) }
        card(3, "비활성 카드").also { it.changeDisabled(true, at(5)) }
        card(4, "다른 그룹 카드", groupId = OTHER_GROUP_ID, listId = OTHER_GROUP_LIST_ID)
        record(10, cardId = 1)
        record(11, cardId = 1).also { it.delete(at(5)) }
        record(12, cardId = 2)
        record(13, cardId = 3)
        record(14, cardId = 4)
        commit()

        val ids = searchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0, 50).map { it.recordId }

        assertEquals(setOf(10L, 13L), ids.toSet())
    }

    @Test
    fun `기록은 기록 수정 시각 내림차순으로 정렬하고 같으면 아이디 내림차순이며 offset과 limit으로 나눈다`() {
        setUpBase()
        card(1, "카드1", createdAt = at(1000))
        record(10, cardId = 1, createdAt = at(10))
        record(11, cardId = 1, createdAt = at(30))
        record(12, cardId = 1, createdAt = at(30))
        record(13, cardId = 1, createdAt = at(20))
        commit()

        assertEquals(listOf(12L, 11L, 13L, 10L), searchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0, 50).map { it.recordId })
        assertEquals(listOf(13L, 10L), searchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 2, 2).map { it.recordId })

        entityManager.find(GroupRecord::class.java, 10L).edit("수정된 본문", at = at(500))
        commit()

        assertEquals(10L, searchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0, 50).first().recordId)
    }

    companion object {
        private const val GROUP_ID = 100L
        private const val OTHER_GROUP_ID = 200L
        private const val LIST_ID = 1L
        private const val OTHER_GROUP_LIST_ID = 2L
        private const val AUTHOR_ID = 11L

        @Container
        @ServiceConnection
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
    }
}
