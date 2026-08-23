package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.member.Member
import com.sight.domain.notification.NotificationCategory
import com.sight.domain.supportrequest.SupportRequest
import com.sight.domain.supportrequest.SupportRequestCategory
import com.sight.domain.supportrequest.SupportRequestComment
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.MemberRepository
import com.sight.repository.SupportRequestCommentRepository
import com.sight.repository.SupportRequestRepository
import com.sight.service.discord.DiscordMessageSender
import com.sight.service.discord.DiscordWebhookAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class SupportRequestUser(
    val userId: Long,
    val name: String,
)

data class SupportRequestSummary(
    val supportRequest: SupportRequest,
    val requester: SupportRequestUser,
    val hasComments: Boolean,
)

data class SupportRequestCommentResult(
    val comment: SupportRequestComment,
    val author: SupportRequestUser,
)

data class SupportRequestDetail(
    val supportRequest: SupportRequest,
    val requester: SupportRequestUser,
    val comments: List<SupportRequestCommentResult>,
)

data class SupportRequestListResult(
    val count: Long,
    val supportRequests: List<SupportRequestSummary>,
)

@Service
class SupportRequestService(
    private val supportRequestRepository: SupportRequestRepository,
    private val supportRequestCommentRepository: SupportRequestCommentRepository,
    private val memberRepository: MemberRepository,
    private val notificationService: NotificationService,
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val discordMessageSender: DiscordMessageSender,
    private val discordWebhookAdapter: DiscordWebhookAdapter,
) {
    private val logger = LoggerFactory.getLogger(SupportRequestService::class.java)

    @Transactional
    fun createSupportRequest(
        requesterId: Long,
        category: SupportRequestCategory,
        title: String,
        content: String,
    ): SupportRequestSummary {
        val requester = getMember(requesterId)
        val supportRequest =
            supportRequestRepository.save(
                SupportRequest(
                    id = UlidCreator.getUlid().toString(),
                    requesterId = requesterId,
                    category = category,
                    title = title,
                    content = content,
                ),
            )

        createRequestNotifications(supportRequest)
        sendRequestCreatedDiscordWebhook(supportRequest, requester)

        return SupportRequestSummary(
            supportRequest = supportRequest,
            requester = requester.toSupportRequestUser(),
            hasComments = false,
        )
    }

    @Transactional(readOnly = true)
    fun listSupportRequests(
        offset: Int,
        limit: Int,
        category: String?,
    ): SupportRequestListResult {
        val supportRequestCategory = category?.let(SupportRequestCategory::valueOf)
        val supportRequests = supportRequestRepository.findSupportRequests(offset, limit, supportRequestCategory)
        val count = supportRequestRepository.countSupportRequests(supportRequestCategory)
        val requesters = findUsers(supportRequests.map { it.requesterId })

        return SupportRequestListResult(
            count = count,
            supportRequests =
                supportRequests.map { supportRequest ->
                    SupportRequestSummary(
                        supportRequest = supportRequest,
                        requester = checkNotNull(requesters[supportRequest.requesterId]),
                        hasComments = supportRequestCommentRepository.existsBySupportRequestId(supportRequest.id),
                    )
                },
        )
    }

    @Transactional(readOnly = true)
    fun getSupportRequestById(supportRequestId: String): SupportRequestDetail {
        val supportRequest = getSupportRequest(supportRequestId)
        val requester = getMember(supportRequest.requesterId).toSupportRequestUser()
        val storedComments = supportRequestCommentRepository.findBySupportRequestIdOrderByCreatedAtAscIdAsc(supportRequestId)
        val authors = findUsers(storedComments.map { it.authorId })
        val comments =
            storedComments
                .map { comment -> SupportRequestCommentResult(comment, checkNotNull(authors[comment.authorId])) }

        return SupportRequestDetail(supportRequest, requester, comments)
    }

    @Transactional
    fun updateSupportRequest(
        supportRequestId: String,
        requesterId: Long,
        category: SupportRequestCategory,
        title: String,
        content: String,
    ): SupportRequestSummary {
        val supportRequest = getSupportRequestForUpdate(supportRequestId)
        if (supportRequest.requesterId != requesterId) {
            throw NotFoundException("지원 신청을 찾을 수 없습니다")
        }
        if (supportRequestCommentRepository.existsBySupportRequestId(supportRequestId)) {
            throw ConflictException("댓글이 등록된 지원 신청은 수정할 수 없습니다")
        }

        supportRequest.update(category, title, content)
        return SupportRequestSummary(
            supportRequest = supportRequest,
            requester = getMember(requesterId).toSupportRequestUser(),
            hasComments = false,
        )
    }

    @Transactional
    fun deleteSupportRequest(supportRequestId: String) {
        val supportRequest = getSupportRequestForUpdate(supportRequestId)
        supportRequestRepository.delete(supportRequest)
    }

    @Transactional
    fun createSupportRequestComment(
        supportRequestId: String,
        authorId: Long,
        isManager: Boolean,
        content: String,
    ): SupportRequestCommentResult {
        val supportRequest = getSupportRequestForUpdate(supportRequestId)
        if (!isManager && supportRequest.requesterId != authorId) {
            throw ForbiddenException("다른 회원의 지원 신청에는 댓글을 작성할 수 없습니다")
        }

        val author = getMember(authorId)
        val comment =
            supportRequestCommentRepository.save(
                SupportRequestComment(
                    id = UlidCreator.getUlid().toString(),
                    supportRequest = supportRequest,
                    authorId = authorId,
                    content = content,
                ),
            )

        createCommentNotifications(supportRequest)
        if (isManager) {
            sendManagerCommentDiscordMessage(supportRequest, author)
        }

        return SupportRequestCommentResult(comment, author.toSupportRequestUser())
    }

    private fun createRequestNotifications(supportRequest: SupportRequest) {
        runCatching {
            notificationService.createNotificationForManagers(
                category = NotificationCategory.SYSTEM,
                title = "새 지원 신청",
                content = "${supportRequest.title} 지원 신청이 등록되었습니다.",
                url = "/support/${supportRequest.id}",
            )
        }.onFailure { error ->
            logger.error("지원 신청 사이트 내 알림 생성 실패: supportRequestId={}", supportRequest.id, error)
        }
    }

    private fun createCommentNotifications(supportRequest: SupportRequest) {
        runCatching {
            val managerNotifications =
                notificationService.createNotificationForManagers(
                    category = NotificationCategory.SYSTEM,
                    title = "지원 신청 댓글",
                    content = "${supportRequest.title} 지원 신청에 댓글이 등록되었습니다.",
                    url = "/support/${supportRequest.id}",
                )
            if (managerNotifications.none { it.userId == supportRequest.requesterId }) {
                notificationService.createNotification(
                    userId = supportRequest.requesterId,
                    category = NotificationCategory.SYSTEM,
                    title = "지원 신청 댓글",
                    content = "${supportRequest.title} 지원 신청에 댓글이 등록되었습니다.",
                    url = "/support/${supportRequest.id}",
                )
            }
        }.onFailure { error ->
            logger.error("지원 신청 댓글 사이트 내 알림 생성 실패: supportRequestId={}", supportRequest.id, error)
        }
    }

    private fun sendRequestCreatedDiscordWebhook(
        supportRequest: SupportRequest,
        requester: Member,
    ) {
        runCatching {
            discordWebhookAdapter.sendSystemAlert(
                mapOf(
                    "embeds" to
                        listOf(
                            mapOf(
                                "title" to "새 지원 신청",
                                "description" to
                                    listOf(
                                        "지원 신청 ID: ${supportRequest.id}",
                                        "카테고리: ${supportRequest.category.name}",
                                        "제목: ${supportRequest.title}",
                                        "신청자: ${requester.realname}",
                                    ).joinToString("\n"),
                            ),
                        ),
                ),
            )
        }.onFailure { error ->
            logger.error("Discord 전송 실패: supportRequestId={}, target=SYSTEM_ALERT_WEBHOOK", supportRequest.id, error)
        }
    }

    private fun sendManagerCommentDiscordMessage(
        supportRequest: SupportRequest,
        author: Member,
    ) {
        val discordIntegration = discordIntegrationRepository.findByUserId(supportRequest.requesterId) ?: return
        runCatching {
            discordMessageSender.sendDirectMessage(
                discordUserId = discordIntegration.discordUserId,
                content = "지원 신청 댓글: [${supportRequest.id}] ${supportRequest.title} / ${author.realname}",
            )
        }.onFailure { error ->
            logger.error("Discord 전송 실패: supportRequestId={}, target=REQUESTER_DM", supportRequest.id, error)
        }
    }

    private fun getSupportRequest(supportRequestId: String): SupportRequest =
        supportRequestRepository.findById(supportRequestId).orElseThrow {
            NotFoundException("지원 신청을 찾을 수 없습니다")
        }

    private fun getSupportRequestForUpdate(supportRequestId: String): SupportRequest =
        supportRequestRepository.findByIdForUpdate(supportRequestId)
            ?: throw NotFoundException("지원 신청을 찾을 수 없습니다")

    private fun getMember(userId: Long): Member =
        memberRepository.findById(userId).orElseThrow {
            NotFoundException("회원을 찾을 수 없습니다")
        }

    private fun findUsers(userIds: List<Long>): Map<Long, SupportRequestUser> =
        memberRepository.findAllById(userIds.distinct()).associate { member ->
            member.id to member.toSupportRequestUser()
        }

    private fun Member.toSupportRequestUser(): SupportRequestUser = SupportRequestUser(id, realname)
}
