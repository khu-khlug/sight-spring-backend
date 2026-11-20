package com.sight.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.config.IntegerIdConfig
import com.sight.config.QueryDslConfig
import com.sight.core.exception.BaseException
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogType
import com.sight.domain.group.testGroup
import com.sight.domain.group.testGroupMember
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberPointRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop", "integer-id.node-id=0"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(
    GroupService::class,
    GroupMemberRepository::class,
    MemberPointRepository::class,
    GroupLogService::class,
    NotificationService::class,
    IntegerIdConfig::class,
    IntegerIdService::class,
    QueryDslConfig::class,
    ObjectMapper::class,
)
class GroupApiTransactionTest {
    @Autowired
    private lateinit var groups: GroupRepository

    @Autowired
    private lateinit var members: MemberRepository

    @Autowired
    private lateinit var groupMembers: GroupMemberRepository

    @Autowired
    private lateinit var service: GroupService

    @Autowired
    private lateinit var logs: GroupLogService

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @BeforeEach
    fun setUp() {
        jdbc.execute(
            """
            CREATE TABLE IF NOT EXISTS expoint_log (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                `member` BIGINT NOT NULL,
                `from` BIGINT NOT NULL,
                message LONGTEXT NOT NULL,
                point BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL
            ) ENGINE=InnoDB
            """.trimIndent(),
        )
        listOf("expoint_log", "notification", "khlug_group_log", "khlug_group_bookmark", "khlug_group_member").forEach {
            jdbc.update("DELETE FROM $it")
        }
        groups.deleteAll()
        members.deleteAll()
        for (id in listOf(10L, 20L)) {
            members.save(Member(id = id, name = "member$id", studentStatus = StudentStatus.UNDERGRADUATE, status = UserStatus.ACTIVE))
        }
        groups.save(testGroup(leaderMemberId = 10L))
        groupMembers.saveAll(listOf(testGroupMember(groupId = 1L, memberId = 10L), testGroupMember(groupId = 1L, memberId = 20L)))
    }

    @Test
    fun `동시 즐겨찾기 추가와 취소는 각각 한 요청만 성공한다`() {
        assertEquals(listOf(201, 422), race(201) { service.addBookmark(1L, 10L) })
        assertEquals(1L, count("SELECT COUNT(*) FROM khlug_group_bookmark WHERE group_id = 1 AND member_id = 10"))
        service.addBookmark(1L, 20L)

        assertEquals(listOf(204, 422), race(204) { service.cancelBookmark(1L, 10L) })
        assertEquals(0L, count("SELECT COUNT(*) FROM khlug_group_bookmark WHERE group_id = 1 AND member_id = 10"))
        assertEquals(1L, count("SELECT COUNT(*) FROM khlug_group_bookmark WHERE group_id = 1 AND member_id = 20"))
        assertEquals(2L, count("SELECT COUNT(*) FROM notification WHERE user_id = 10"))
        assertEquals(1L, count("SELECT COUNT(*) FROM notification WHERE user_id = 20"))
    }

    @Test
    fun `다른 회원은 같은 그룹의 즐겨찾기 트랜잭션을 기다리지 않는다`() {
        val inserted = CountDownLatch(1)
        val commit = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit {
                TransactionTemplate(transactionManager).executeWithoutResult {
                    service.addBookmark(1L, 10L)
                    inserted.countDown()
                    assertTrue(commit.await(15, TimeUnit.SECONDS))
                }
            }
            assertTrue(inserted.await(10, TimeUnit.SECONDS))
            executor.submit { service.addBookmark(1L, 20L) }.get(10, TimeUnit.SECONDS)
            commit.countDown()
            first.get(10, TimeUnit.SECONDS)
            assertEquals(2L, count("SELECT COUNT(*) FROM khlug_group_bookmark WHERE group_id = 1"))
        } finally {
            commit.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `알림 저장이 실패하면 즐겨찾기 추가와 취소도 롤백된다`() {
        service.addBookmark(1L, 20L)
        jdbc.execute("ALTER TABLE notification ADD CONSTRAINT reject_test_bookmark_alert UNIQUE (category)")
        try {
            assertThrows<DataIntegrityViolationException> { service.addBookmark(1L, 10L) }
            assertThrows<DataIntegrityViolationException> { service.cancelBookmark(1L, 20L) }
        } finally {
            jdbc.execute("ALTER TABLE notification DROP INDEX reject_test_bookmark_alert")
        }

        assertEquals(0L, count("SELECT COUNT(*) FROM khlug_group_bookmark WHERE member_id = 10"))
        assertEquals(1L, count("SELECT COUNT(*) FROM khlug_group_bookmark WHERE member_id = 20"))
        assertEquals(1L, count("SELECT COUNT(*) FROM notification"))
    }

    @Test
    fun `동시 포트폴리오 발행과 취소는 각각 한 번만 경험치 알림과 로그를 저장한다`() {
        assertEquals(listOf(200, 400), race(200) { service.publishPortfolio(1L, 10L) })
        assertEquals(true, groups.findById(1L).orElseThrow().isPortfolioPublished)
        assertEquals(2L, count("SELECT COUNT(*) FROM khlug_members WHERE expoint = 10"))
        assertEquals(2L, count("SELECT COUNT(*) FROM expoint_log WHERE point = 10"))
        assertEquals(2L, count("SELECT COUNT(*) FROM notification"))
        assertEquals(1L, count("SELECT COUNT(*) FROM khlug_group_log WHERE log_type = '${GroupLogType.GROUP_PORTFOLIO_PUBLISHED}'"))

        assertEquals(listOf(204, 404), race(204) { service.cancelPortfolio(1L, 10L) })
        assertEquals(false, groups.findById(1L).orElseThrow().isPortfolioPublished)
        assertEquals(2L, count("SELECT COUNT(*) FROM khlug_members WHERE expoint = 0"))
        assertEquals(2L, count("SELECT COUNT(*) FROM expoint_log WHERE point = -10"))
        assertEquals(4L, count("SELECT COUNT(*) FROM notification"))
        assertEquals(1L, count("SELECT COUNT(*) FROM khlug_group_log WHERE log_type = '${GroupLogType.GROUP_PORTFOLIO_UNPUBLISHED}'"))
    }

    @Test
    fun `포트폴리오 로그 저장이 실패하면 앞선 DB 변경도 롤백된다`() {
        logs.createLog(1L, 10L, GroupLogData.GroupProgress)
        jdbc.execute("ALTER TABLE khlug_group_log ADD CONSTRAINT reject_test_group_log UNIQUE (group_id)")
        try {
            assertThrows<DataIntegrityViolationException> { service.publishPortfolio(1L, 10L) }
        } finally {
            jdbc.execute("ALTER TABLE khlug_group_log DROP INDEX reject_test_group_log")
        }

        assertEquals(false, groups.findById(1L).orElseThrow().isPortfolioPublished)
        assertEquals(2L, count("SELECT COUNT(*) FROM khlug_members WHERE expoint = 0"))
        assertEquals(0L, count("SELECT COUNT(*) FROM expoint_log"))
        assertEquals(0L, count("SELECT COUNT(*) FROM notification"))
        assertEquals(1L, count("SELECT COUNT(*) FROM khlug_group_log WHERE log_type = 'GROUP_PROGRESS'"))
        assertEquals(1L, count("SELECT COUNT(*) FROM khlug_group_log"))
    }

    private fun count(sql: String): Long = requireNotNull(jdbc.queryForObject(sql, Long::class.javaObjectType))

    private fun race(successStatus: Int, request: () -> Unit): List<Int> {
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val requests = List(2) {
                executor.submit(
                    Callable {
                        barrier.await(10, TimeUnit.SECONDS)
                        try {
                            request()
                            successStatus
                        } catch (failure: BaseException) {
                            failure.statusCode.value()
                        }
                    },
                )
            }
            return requests.map { it.get(20, TimeUnit.SECONDS) }.sorted()
        } finally {
            executor.shutdownNow()
        }
    }

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer<Nothing>("mysql:8.0.43")

        @DynamicPropertySource
        @JvmStatic
        fun database(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
        }
    }
}
