package com.sight.repository

import com.sight.domain.file.FileUpload
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository

@Repository
class FileUploadRepositoryImpl(private val entityManager: EntityManager) : FileUploadRepositoryCustom {
    override fun findForUpdateById(id: String): FileUpload? {
        entityManager.flush()
        val file =
            entityManager.createQuery("select f from FileUpload f where f.id = :id", FileUpload::class.java)
                .setParameter("id", id).setLockMode(LockModeType.PESSIMISTIC_WRITE).resultList.firstOrNull() ?: return null
        // 같은 영속성 컨텍스트에서 미리 읽은 파일도 잠금 획득 후의 최신 상태로 맞춘다.
        entityManager.refresh(file, LockModeType.PESSIMISTIC_WRITE)
        return file
    }
}
