package com.sight.repository

import com.sight.config.QueryDslConfig
import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordArchive
import com.sight.domain.group.GroupRecordFile
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.service.FileReferenceService
import com.sight.service.FileUploadDeletionService
import com.sight.service.GroupFilePaths
import com.sight.service.GroupRecordAttachmentService
import com.sight.service.GroupRecordUploadService
import com.sight.service.IntegerIdService
import com.sight.service.StorageService
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.core.task.TaskExecutor
import org.springframework.test.context.transaction.TestTransaction
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    QueryDslConfig::class,
    FileReferenceRepositoryImpl::class,
    FileReferenceService::class,
    FileUploadDeletionService::class,
    GroupRecordAttachmentService::class,
)
@Testcontainers(disabledWithoutDocker = true)
class GroupRecordPersistenceTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var archives: GroupRecordArchiveRepository

    @Autowired
    private lateinit var attachments: GroupRecordAttachmentService

    @Autowired
    private lateinit var fileUploadRepository: FileUploadRepository

    @Autowired
    private lateinit var recordFileRepository: GroupRecordFileRepository

    @MockBean
    private lateinit var storage: StorageService

    @MockBean
    private lateinit var ids: IntegerIdService

    @MockBean
    private lateinit var uploadChecks: GroupRecordUploadService

    @MockBean(name = "fileDeletionExecutor")
    private lateinit var deletionExecutor: TaskExecutor

    @Test
    fun `보관 시각이 같은 기록을 페이지별로 조회하면 요청자가 보관한 기록만 기록 아이디 내림차순으로 반환한다`() {
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

        assertEquals(11L, archives.countByMemberId(6L))
        assertEquals((111L downTo 102L).toList(), first.map { it.recordId })
        assertEquals(listOf(101L), second.map { it.recordId })
        assertTrue(archives.findSavedRecordsByMemberIdOrderBySavedAtDesc(6L, 20L, 10).isEmpty())
        assertEquals(2L, first.first().groupId)
        assertTrue(first.first().isCardDeleted)
        assertTrue(first[1].isDeleted)
        assertEquals("본문 101", second.single().content)
    }

    @Test
    fun `기록과 임시저장의 첨부 연결 해제를 롤백하면 두 연결을 유지하고 파일을 삭제하지 않는다`() {
        persistSharedFile("rolled-back", 30L)
        TestTransaction.flagForCommit()
        TestTransaction.end()
        TestTransaction.start()

        detachSharedFile("rolled-back", 30L)
        TestTransaction.flagForRollback()
        TestTransaction.end()
        TestTransaction.start()

        assertFalse(fileUploadRepository.findById("rolled-back").orElseThrow().isDeleted)
        assertEquals(2, recordFileRepository.findByFileUploadIdOrderById("rolled-back").size)
        verifyNoInteractions(deletionExecutor, storage)
    }

    @Test
    fun `기록과 임시저장이 공유하는 파일은 두 연결을 모두 해제하고 커밋한 뒤에 삭제 작업을 등록한다`() {
        persistSharedFile("committed", 40L)

        detachSharedFile("committed", 40L)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        verify(deletionExecutor).execute(any())
        verifyNoInteractions(storage)
    }

    private fun detachSharedFile(
        fileId: String,
        referrerId: Long,
    ) {
        attachments.replaceRecordFiles(1L, 2L, referrerId, emptyList())
        assertFalse(fileUploadRepository.findById(fileId).orElseThrow().isDeleted)
        assertEquals(1, recordFileRepository.findByFileUploadIdOrderById(fileId).size)

        attachments.deleteDraftFiles(referrerId)
        assertTrue(fileUploadRepository.findById(fileId).orElseThrow().isDeleted)
        assertTrue(recordFileRepository.findByFileUploadIdOrderById(fileId).isEmpty())
        verifyNoInteractions(deletionExecutor)
    }

    private fun persistSharedFile(
        fileId: String,
        linkId: Long,
    ) {
        entityManager.persist(FileUpload(fileId, "key-$fileId", 2L, GroupFilePaths.recordAttachmentApiPath(1L), "file.txt", 1L, "text/plain", NOW))
        val recordReference = FileReference("record-$fileId", fileId, FileReferenceType.GROUP_RECORD_ATTACHMENT, linkId.toString())
        val draftReference = FileReference("draft-$fileId", fileId, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, linkId.toString())
        entityManager.persist(recordReference)
        entityManager.persist(draftReference)
        entityManager.persist(GroupRecordFile(linkId, recordReference.id, "record-block"))
        entityManager.persist(GroupRecordFile(linkId + 1, draftReference.id, "draft-block"))
        entityManager.flush()
    }

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer<Nothing>("mysql:8.0.40")
    }
}
