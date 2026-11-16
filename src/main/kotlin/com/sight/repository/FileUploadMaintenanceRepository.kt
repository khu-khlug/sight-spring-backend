package com.sight.repository

import com.sight.domain.file.FileUpload
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository
import java.time.Instant

data class FileUploadReservationView(val id: String, val uploadCheckAt: Instant)

@Repository
class FileUploadMaintenanceRepository(private val entityManager: EntityManager) {
    fun findById(id: String): FileUpload? = entityManager.find(FileUpload::class.java, id)

    fun findLockedById(id: String): FileUpload? = entityManager.find(FileUpload::class.java, id, LockModeType.PESSIMISTIC_WRITE)

    fun findPendingReservationsAfter(
        cursor: String,
        horizon: Instant,
        limit: Int,
    ): List<FileUploadReservationView> =
        entityManager.createQuery(
            "select f from FileUpload f where f.id > :cursor and f.isUploaded = false and f.deletedAt is null " +
                "and f.uploadCheckedAt is null and f.uploadCheckAt <= :horizon order by f.id",
            FileUpload::class.java,
        ).setParameter("cursor", cursor).setParameter("horizon", horizon).setMaxResults(limit).resultList.map {
            FileUploadReservationView(it.id, it.uploadCheckAt)
        }

    fun findCleanupCandidatesAfter(
        cursor: String,
        at: Instant,
        limit: Int,
    ): List<String> =
        entityManager.createQuery(
            "select f.id from FileUpload f where f.id > :cursor and " +
                "(f.deletedAt is not null or (f.uploadCheckAt <= :at and (f.isUploaded = false or not exists " +
                "(select l.id from GroupRecordFile l where l.fileUploadId = f.id)))) order by f.id",
            String::class.java,
        ).setParameter("cursor", cursor).setParameter("at", at).setMaxResults(limit).resultList

    fun hasRecordLinks(id: String): Boolean =
        entityManager.createQuery("select l.id from GroupRecordFile l where l.fileUploadId = :id", java.lang.Long::class.java)
            .setParameter("id", id).setMaxResults(1).resultList.isNotEmpty()

    fun hasOwnerReferences(id: String): Boolean {
        if (hasRecordLinks(id)) return true
        return hasSingleUseOwnerReferences(id)
    }

    fun hasSingleUseOwnerReferences(id: String): Boolean {
        val report =
            entityManager.createQuery(
                "select r.id from GroupActivityReport r where r.reportFileUploadId = :id",
                String::class.java,
            ).setParameter("id", id).setMaxResults(1).resultList.isNotEmpty()
        if (report) return true
        return entityManager.createQuery(
            "select c.id from GroupCard c where c.coverFileUploadId = :id",
            java.lang.Long::class.java,
        ).setParameter("id", id).setMaxResults(1).resultList.isNotEmpty()
    }

    fun deleteRecordLinks(id: String) {
        entityManager.createQuery("delete from GroupRecordFile l where l.fileUploadId = :id").setParameter("id", id).executeUpdate()
    }

    fun markUnusedUploadDeleted(
        id: String,
        at: Instant,
    ): Boolean = markDeletedIf(id, at, "f.is_uploaded = false")

    fun markUnreferencedUploadDeleted(
        id: String,
        at: Instant,
    ): Boolean = markDeletedIf(id, at, "NOT EXISTS (SELECT 1 FROM khlug_group_record_file l WHERE l.file_upload_id = f.id)")

    fun markRequestedUploadDeleted(
        id: String,
        at: Instant,
    ): Boolean {
        // 소유 데이터의 변경을 반영한 뒤 현재 참조가 없는 파일만 삭제 대상으로 전환한다.
        entityManager.flush()
        return entityManager.createNativeQuery(
            "UPDATE file_upload f SET f.deleted_at = :at, f.read_url = NULL, f.read_url_expires_at = NULL " +
                "WHERE f.id = :id AND f.deleted_at IS NULL " +
                "AND NOT EXISTS (SELECT 1 FROM khlug_group_record_file l WHERE l.file_upload_id = f.id) " +
                "AND NOT EXISTS (SELECT 1 FROM khlug_group_activity_report r WHERE r.report_file_upload_id = f.id) " +
                "AND NOT EXISTS (SELECT 1 FROM khlug_group_card c WHERE c.cover_file_upload_id = f.id)",
        ).setParameter("id", id).setParameter("at", at).executeUpdate() == 1
    }

    private fun markDeletedIf(
        id: String,
        at: Instant,
        condition: String,
    ): Boolean =
        entityManager.createNativeQuery(
            "UPDATE file_upload f SET f.deleted_at = :at, f.read_url = NULL, f.read_url_expires_at = NULL " +
                "WHERE f.id = :id AND f.deleted_at IS NULL AND f.upload_check_at <= :at AND $condition " +
                "AND NOT EXISTS (SELECT 1 FROM khlug_group_activity_report r WHERE r.report_file_upload_id = f.id) " +
                "AND NOT EXISTS (SELECT 1 FROM khlug_group_card c WHERE c.cover_file_upload_id = f.id)",
        ).setParameter("id", id).setParameter("at", at).executeUpdate() == 1

    fun deleteUpload(file: FileUpload) {
        entityManager.createQuery("delete from GroupFile g where g.fileUploadId = :id").setParameter("id", file.id).executeUpdate()
        entityManager.remove(file)
    }
}
