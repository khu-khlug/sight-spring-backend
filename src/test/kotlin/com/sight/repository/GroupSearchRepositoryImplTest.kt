package com.sight.repository

import com.sight.config.QueryDslConfig
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupKanbanList
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupSearchKeywords
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

// 실제 MySQL에서 검색 쿼리의 삭제 제외·정렬·페이징·글자 그대로 검색을 검증한다.
// Docker를 쓸 수 없는 환경에서는 다른 테스트가 실패하지 않도록 이 테스트만 건너뛴다.
@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(QueryDslConfig::class, GroupSearchRepositoryImpl::class)
class GroupSearchRepositoryImplTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: GroupSearchRepositoryImpl

    private fun at(second: Int): Instant = Instant.parse("2025-01-01T00:00:00Z").plusSeconds(second.toLong())

    private fun keywords(text: String) = GroupSearchKeywords.parse(text)

    private fun member(
        id: Long,
        realname: String,
    ) = entityManager.persist(
        Member(
            id = id,
            name = "user$id",
            realname = realname,
            admission = "19",
            college = "공과대학",
            grade = 3L,
            studentStatus = StudentStatus.UNDERGRADUATE,
            status = UserStatus.ACTIVE,
        ),
    )

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
        authorId: Long = AUTHOR_ID,
        createdAt: Instant = at(0),
    ) = entityManager.persist(
        GroupCard(
            id = id,
            groupId = groupId,
            listId = listId,
            authorMemberId = authorId,
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
        member(AUTHOR_ID, "Hong Gildong")
        member(OTHER_AUTHOR_ID, "Kim Minsu")
        list(LIST_ID)
        list(OTHER_GROUP_LIST_ID, groupId = OTHER_GROUP_ID, title = "다른 그룹")
    }

    private fun titles(keyword: String): List<Long> =
        repository.findCardsByTitleOrderByLatest(GROUP_ID, keywords(keyword), 0, 50).map { it.cardId }

    @Test
    fun `카드명 검색은 삭제된 카드와 다른 그룹의 카드를 제외하고 비활성 카드는 포함한다`() {
        setUpBase()
        card(1, "스프링 스터디")
        card(2, "스프링 삭제됨").also { it.delete(at(5)) }
        card(3, "스프링 비활성").also { it.changeDisabled(true, at(5)) }
        card(4, "스프링 다른 그룹", groupId = OTHER_GROUP_ID, listId = OTHER_GROUP_LIST_ID)
        card(5, "관계없는 카드")
        commit()

        assertEquals(setOf(1L, 3L), titles("스프링").toSet())
        assertEquals(2L, repository.countCardsByTitle(GROUP_ID, keywords("스프링")))
    }

    @Test
    fun `검색 결과에는 리스트 아이디·제목과 카드 아이디·카드명이 들어 있다`() {
        setUpBase()
        card(1, "스프링 스터디")
        commit()

        val view = repository.findCardsByTitleOrderByLatest(GROUP_ID, keywords("스프링"), 0, 50).single()

        assertEquals(LIST_ID, view.listId)
        assertEquals("할 일", view.listTitle)
        assertEquals(1L, view.cardId)
        assertEquals("스프링 스터디", view.cardTitle)
    }

    @Test
    fun `카드 수정 시각 내림차순으로 정렬하고 같으면 아이디 내림차순이다`() {
        setUpBase()
        card(1, "정렬 가", createdAt = at(10))
        card(2, "정렬 나", createdAt = at(30))
        card(3, "정렬 다", createdAt = at(30))
        card(4, "정렬 라", createdAt = at(20))
        commit()

        assertEquals(listOf(3L, 2L, 4L, 1L), titles("정렬"))
    }

    @Test
    fun `기록 추가로 카드 수정 시각이 바뀌면 카드 정렬에 반영된다`() {
        setUpBase()
        card(1, "정렬 가", createdAt = at(10))
        card(2, "정렬 나", createdAt = at(20))
        commit()
        assertEquals(listOf(2L, 1L), titles("정렬"))

        val older = entityManager.find(GroupCard::class.java, 1L)
        older.increaseRecordCount(at(100))
        commit()

        assertEquals(listOf(1L, 2L), titles("정렬"))
    }

    @Test
    fun `페이지는 offset과 limit으로 나눈다`() {
        setUpBase()
        (1L..5L).forEach { card(it, "페이지 $it", createdAt = at(it.toInt())) }
        commit()

        val page = repository.findCardsByTitleOrderByLatest(GROUP_ID, keywords("페이지"), 2, 2)

        assertEquals(listOf(3L, 2L), page.map { it.cardId })
        assertEquals(5L, repository.countCardsByTitle(GROUP_ID, keywords("페이지")))
    }

    @Test
    fun `모든 검색어가 카드명에 들어 있어야 하고 대소문자를 구분하지 않는다`() {
        setUpBase()
        card(1, "Spring 백엔드 스터디")
        card(2, "Spring 프런트")
        card(3, "백엔드만")
        commit()

        assertEquals(listOf(1L), titles("백엔드 spring"))
        assertEquals(listOf(1L, 2L), titles("SPRING").sorted())
    }

    @Test
    fun `생성자명 검색은 카드 생성자명으로 찾고 삭제된 카드는 제외한다`() {
        setUpBase()
        card(1, "카드 하나", authorId = AUTHOR_ID)
        card(2, "카드 둘", authorId = OTHER_AUTHOR_ID)
        card(3, "삭제된 카드", authorId = AUTHOR_ID).also { it.delete(at(5)) }
        commit()

        val found = repository.findCardsByAuthorNameOrderByLatest(GROUP_ID, keywords("gildong"), 0, 50)

        assertEquals(listOf(1L), found.map { it.cardId })
        assertEquals("Hong Gildong", found.single().authorName)
        assertEquals(1L, repository.countCardsByAuthorName(GROUP_ID, keywords("GILDONG")))
    }

    @Test
    fun `와일드카드 문자와 역슬래시는 글자 그대로 찾는다`() {
        setUpBase()
        card(1, "진행률 100% 달성")
        card(2, "진행률 1000 달성")
        card(3, "a_c 변수")
        card(4, "abc 변수")
        card(5, "경로 C:\\temp")
        card(6, "경로 C:temp")
        card(7, "느낌표! 포함")
        commit()

        assertEquals(listOf(1L), titles("100%"))
        assertEquals(listOf(3L), titles("a_c"))
        assertEquals(listOf(5L), titles("C:\\temp"))
        assertEquals(listOf(7L), titles("!"))
        // `%` 한 글자도 와일드카드가 아니므로 `%`가 실제로 들어 있는 카드만 찾는다.
        assertEquals(listOf(1L), titles("%"))
        assertEquals(listOf(3L), titles("_"))
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

        val ids = repository.findSearchableRecordsOrderByLatest(GROUP_ID, 0, 50).map { it.recordId }

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

        assertEquals(listOf(12L, 11L, 13L, 10L), repository.findSearchableRecordsOrderByLatest(GROUP_ID, 0, 50).map { it.recordId })
        assertEquals(listOf(13L, 10L), repository.findSearchableRecordsOrderByLatest(GROUP_ID, 2, 2).map { it.recordId })

        entityManager.find(GroupRecord::class.java, 10L).edit("수정된 본문", isFileLinksChanged = false, at = at(500))
        commit()

        assertEquals(10L, repository.findSearchableRecordsOrderByLatest(GROUP_ID, 0, 50).first().recordId)
    }

    @Test
    fun `카드 정보 조회는 삭제된 카드와 다른 그룹의 카드를 제외한다`() {
        setUpBase()
        card(1, "카드1")
        card(2, "삭제된 카드").also { it.delete(at(5)) }
        card(3, "다른 그룹 카드", groupId = OTHER_GROUP_ID, listId = OTHER_GROUP_LIST_ID)
        commit()

        val ids = repository.findCardViewsByIds(GROUP_ID, listOf(1L, 2L, 3L)).map { it.cardId }

        assertEquals(listOf(1L), ids)
        assertEquals(emptyList<Long>(), repository.findCardViewsByIds(GROUP_ID, emptyList()).map { it.cardId })
    }

    companion object {
        private const val GROUP_ID = 100L
        private const val OTHER_GROUP_ID = 200L
        private const val LIST_ID = 1L
        private const val OTHER_GROUP_LIST_ID = 2L
        private const val AUTHOR_ID = 11L
        private const val OTHER_AUTHOR_ID = 12L

        @Container
        @ServiceConnection
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
    }
}
