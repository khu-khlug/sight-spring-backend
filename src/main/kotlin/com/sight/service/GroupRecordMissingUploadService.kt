package com.sight.service

import com.sight.domain.file.FileUploadMissingEvent
import com.sight.repository.FileReferenceRepository
import com.sight.repository.GroupRecordFileRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class GroupRecordMissingUploadService(
    private val links: GroupRecordFileRepository,
    private val references: FileReferenceRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun deleteMissingUploadReferences(event: FileUploadMissingEvent) {
        val existing = links.findByFileUploadIdOrderById(event.fileUploadId)
        links.deleteAll(existing.map { it.link })
        links.flush()
        existing.forEach { view ->
            references.deleteByIdAndTypeAndOwner(view.reference.id, view.reference.referenceType, view.reference.ownerId)
        }
    }
}
