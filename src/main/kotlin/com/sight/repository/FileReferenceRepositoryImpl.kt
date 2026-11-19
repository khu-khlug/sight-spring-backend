package com.sight.repository

import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class FileReferenceRepositoryImpl(private val entityManager: EntityManager) : FileReferenceRepository {
    override fun findById(id: String): FileReference? = entityManager.find(FileReference::class.java, id)

    override fun findByTypeAndOwnerOrderById(
        referenceType: FileReferenceType,
        ownerId: String,
        cursor: String,
        limit: Int,
    ): List<FileReference> =
        entityManager.createQuery(
            "select r from FileReference r where r.referenceType = :type and r.ownerId = :owner " +
                "and r.id > :cursor order by r.id",
            FileReference::class.java,
        ).setParameter("type", referenceType).setParameter("owner", ownerId)
            .setParameter("cursor", cursor).setMaxResults(limit).resultList

    override fun existsByFileUploadId(fileUploadId: String): Boolean =
        entityManager.createQuery(
            "select r.id from FileReference r where r.fileUploadId = :fileId",
            String::class.java,
        ).setParameter("fileId", fileUploadId).setMaxResults(1).resultList.isNotEmpty()

    override fun findDistinctTypesByFileUploadId(fileUploadId: String): Set<FileReferenceType> =
        entityManager.createQuery(
            "select distinct r.referenceType from FileReference r where r.fileUploadId = :fileId",
            FileReferenceType::class.java,
        ).setParameter("fileId", fileUploadId).resultList.toSet()

    override fun save(reference: FileReference): FileReference {
        entityManager.persist(reference)
        // 도메인별 부가정보가 이 참조의 FK를 저장하기 전에 참조 행을 생성한다.
        entityManager.flush()
        return reference
    }

    override fun deleteByIdAndTypeAndOwner(
        id: String,
        referenceType: FileReferenceType,
        ownerId: String,
    ): Boolean {
        // 도메인별 부가정보의 FK 해제를 먼저 반영한다.
        entityManager.flush()
        return entityManager.createQuery(
            "delete from FileReference r where r.id = :id and r.referenceType = :type and r.ownerId = :owner",
        ).setParameter("id", id).setParameter("type", referenceType).setParameter("owner", ownerId).executeUpdate() == 1
    }
}
