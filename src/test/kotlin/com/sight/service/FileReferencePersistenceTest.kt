package com.sight.service

import com.sight.config.QueryDslConfig
import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.testFileUpload
import com.sight.repository.FileReferenceRepository
import com.sight.repository.FileReferenceRepositoryImpl
import com.sight.repository.FileUploadRepository
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

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig::class, FileReferenceRepositoryImpl::class, FileReferenceService::class, FileUploadDeletionService::class)
@Testcontainers(disabledWithoutDocker = true)
class FileReferencePersistenceTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var fileReferenceService: FileReferenceService

    @Autowired
    private lateinit var fileReferenceRepository: FileReferenceRepository

    @Autowired
    private lateinit var fileUploadRepository: FileUploadRepository

    @MockBean
    private lateinit var storage: StorageService

    @MockBean(name = "fileDeletionExecutor")
    private lateinit var deletionExecutor: TaskExecutor

    @Test
    fun `마지막 파일 참조를 삭제하는 트랜잭션이 롤백될 때 참조와 파일을 보존한다`() {
        val fileReference = persistFileWithOneReference("rolled-back")
        TestTransaction.flagForCommit()
        TestTransaction.end()
        TestTransaction.start()

        fileReferenceService.deleteFileReference(fileReference.id, fileReference.referenceType, fileReference.referrerId)

        assertFalse(fileReferenceRepository.existsByFileUploadId(fileReference.fileUploadId))
        assertTrue(fileUploadRepository.findById(fileReference.fileUploadId).orElseThrow().isDeleted)
        verifyNoInteractions(deletionExecutor, storage)
        TestTransaction.flagForRollback()
        TestTransaction.end()
        TestTransaction.start()

        assertEquals(fileReference.fileUploadId, fileReferenceRepository.findById(fileReference.id)?.fileUploadId)
        assertFalse(fileUploadRepository.findById(fileReference.fileUploadId).orElseThrow().isDeleted)
        verifyNoInteractions(deletionExecutor, storage)
    }

    @Test
    fun `마지막 파일 참조를 삭제할 때 파일 삭제 작업은 커밋 후에 등록한다`() {
        val fileReference = persistFileWithOneReference("committed")

        fileReferenceService.deleteFileReference(fileReference.id, fileReference.referenceType, fileReference.referrerId)

        assertFalse(fileReferenceRepository.existsByFileUploadId(fileReference.fileUploadId))
        assertTrue(fileUploadRepository.findById(fileReference.fileUploadId).orElseThrow().isDeleted)
        verifyNoInteractions(deletionExecutor, storage)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        verify(deletionExecutor).execute(any())
        verifyNoInteractions(storage)
    }

    @Test
    fun `파일 참조를 삭제할 때 다른 참조가 남아 있으면 파일을 삭제하지 않는다`() {
        val fileReference = persistFileWithOneReference("still-referenced")
        val remainingReference = FileReference("remaining-ref", fileReference.fileUploadId, fileReference.referenceType, "other-referrer")
        entityManager.persist(remainingReference)
        entityManager.flush()

        fileReferenceService.deleteFileReference(fileReference.id, fileReference.referenceType, fileReference.referrerId)

        assertTrue(fileReferenceRepository.existsByFileUploadId(fileReference.fileUploadId))
        assertFalse(fileUploadRepository.findById(fileReference.fileUploadId).orElseThrow().isDeleted)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        verifyNoInteractions(deletionExecutor, storage)
    }

    private fun persistFileWithOneReference(fileId: String): FileReference {
        entityManager.persist(testFileUpload(id = fileId, isUploaded = true))
        val fileReference = FileReference("ref-$fileId", fileId, FileReferenceType.GROUP_ACTIVITY_REPORT, "referrer-$fileId")
        entityManager.persist(fileReference)
        entityManager.flush()
        entityManager.clear()
        return fileReference
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer<Nothing>("mysql:8.0.40")
    }
}
