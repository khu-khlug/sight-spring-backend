package com.sight.domain.application

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "application_form")
class ApplicationForm(
    id: String,
    info21Id: String,
    submittee: String,
    status: ApplicationFormStatus,
    assignedUserId: Long? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String = id

    @Column(name = "info21_id", nullable = false, length = 100)
    val info21Id: String = info21Id

    @Column(name = "submittee", nullable = false, length = 255)
    val submittee: String = submittee

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: ApplicationFormStatus = status
        private set

    @Column(name = "assigned_user_id", nullable = true)
    var assignedUserId: Long? = assignedUserId
        private set

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = createdAt

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = updatedAt

    fun assignManager(managerUserId: Long) {
        assignedUserId = managerUserId
    }

    fun submit() {
        require(status == ApplicationFormStatus.DRAFT) { "임시저장 상태의 가입신청서만 제출할 수 있습니다" }
        status = ApplicationFormStatus.SUBMITTED
    }

    fun pass() = changeSubmittedStatus(ApplicationFormStatus.PASSED)

    fun reject() = changeSubmittedStatus(ApplicationFormStatus.REJECTED)

    fun suspend() = changeSubmittedStatus(ApplicationFormStatus.SUSPENDED)

    private fun changeSubmittedStatus(targetStatus: ApplicationFormStatus) {
        require(status == ApplicationFormStatus.SUBMITTED) { "제출된 상태의 가입신청서만 상태를 변경할 수 있습니다" }
        status = targetStatus
    }
}
