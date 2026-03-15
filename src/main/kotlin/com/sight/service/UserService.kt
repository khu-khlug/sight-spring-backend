package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.member.Member
import com.sight.domain.member.MemberTagFilter
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.member.needAuth
import com.sight.domain.member.needPayFee
import com.sight.domain.member.needPayHalfFee
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.FeeHistoryRepository
import com.sight.repository.MemberRepository
import com.sight.util.UnivPeriod
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MemberWithTags(
    val member: Member,
    val normalTags: List<String>,
    val redTags: List<String>,
)

@Service
class UserService(
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val discordMemberService: DiscordMemberService,
    private val memberRepository: MemberRepository,
    private val feeHistoryRepository: FeeHistoryRepository,
    private val pointService: PointService,
    private val notificationService: NotificationService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }

    fun applyUserInfoToEnteredDiscordUser(discordUserId: String) {
        val discordIntegration = discordIntegrationRepository.findByDiscordUserId(discordUserId) ?: return

        val userId = discordIntegration.userId
        discordMemberService.reflectUserInfoToDiscordUser(userId)
    }

    fun getMemberById(userId: Long): Member =
        memberRepository.findById(userId).orElseThrow {
            NotFoundException("사용자를 찾을 수 없습니다")
        }

    @Transactional
    fun graduateMember(userId: Long) {
        val member =
            memberRepository.findById(userId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }
        if (member.studentStatus == StudentStatus.GRADUATE) {
            throw UnprocessableEntityException("이미 졸업 처리된 사용자입니다")
        }

        memberRepository.save(
            member.copy(
                studentStatus = StudentStatus.GRADUATE,
                grade = 0L,
                manager = false,
                updatedAt = LocalDateTime.now(),
            ),
        )
        discordMemberService.reflectUserInfoToDiscordUser(userId)

        notificationService.createNotification(
            userId,
            NotificationCategory.SYSTEM,
            "졸업 처리",
            "졸업 처리되었어요.",
        )
        notificationService.createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "졸업 처리",
            "${member.realname} 회원이 졸업 처리되었어요.",
        )
    }

    @Transactional
    fun ungraduateMember(userId: Long) {
        val member =
            memberRepository.findById(userId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }
        if (member.studentStatus != StudentStatus.GRADUATE) {
            throw UnprocessableEntityException("졸업 처리된 사용자가 아닙니다")
        }

        memberRepository.save(
            member.copy(
                studentStatus = StudentStatus.UNDERGRADUATE,
                grade = 4L,
                updatedAt = LocalDateTime.now(),
            ),
        )
        discordMemberService.reflectUserInfoToDiscordUser(userId)

        notificationService.createNotification(
            userId,
            NotificationCategory.SYSTEM,
            "졸업 취소 처리",
            "아직 졸업 안 하셨네요. 졸업 처리가 취소되었어요.",
        )
        notificationService.createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "졸업 취소 처리",
            "${member.realname} 회원이 졸업 취소 처리되었어요.",
        )
    }

    @Transactional
    fun pauseMember(
        targetUserId: Long,
        returnAt: LocalDate,
        reason: String,
    ) {
        val member =
            memberRepository.findById(targetUserId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }
        if (member.returnAt != null) {
            throw UnprocessableEntityException("이미 정지 처리된 회원입니다")
        }

        val returnAtInstant = returnAt.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant()
        memberRepository.save(
            member.copy(
                returnAt = returnAtInstant,
                returnReason = reason,
                updatedAt = LocalDateTime.now(),
            ),
        )

        val formattedDate = returnAt.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
        notificationService.createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "회원 활동 정지",
            "${member.realname} 회원은 $reason 사유로 ${formattedDate}까지 활동을 쉽니다.",
        )
        notificationService.createNotification(
            targetUserId,
            NotificationCategory.SYSTEM,
            "활동 정지",
            "${member.realname} 회원님은 $reason 사유로 ${formattedDate}까지 활동을 쉽니다. " +
                "쉬는 동안에는 회비 납부 와 발표 그룹 참여 의무가 면제되나, 여유가 되실 때에는 언제든지 활동을 하셔도 됩니다.",
        )
    }

    @Transactional
    fun resumeMember(targetUserId: Long) {
        val member =
            memberRepository.findById(targetUserId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }
        if (member.returnAt == null) {
            throw UnprocessableEntityException("정지 처리된 회원이 아닙니다")
        }

        memberRepository.save(
            member.copy(
                returnAt = null,
                returnReason = null,
                updatedAt = LocalDateTime.now(),
            ),
        )

        notificationService.createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "회원 활동 재개",
            "${member.realname} 회원은 활동을 재개합니다.",
        )
        notificationService.createNotification(
            targetUserId,
            NotificationCategory.SYSTEM,
            "활동 재개",
            "돌아오신 것을 환영합니다!",
        )
    }

    @Transactional
    fun blockMember(userId: Long) {
        val member =
            memberRepository.findById(userId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }
        if (member.status != UserStatus.ACTIVE) {
            throw UnprocessableEntityException("차단할 수 없는 상태의 회원입니다")
        }

        memberRepository.save(
            member.copy(
                status = UserStatus.INACTIVE,
                updatedAt = LocalDateTime.now(),
            ),
        )
        discordMemberService.reflectUserInfoToDiscordUser(userId)

        pointService.givePoint(
            targetUserId = userId,
            point = -1100,
            message = "사이트 접속이 차단되었습니다.",
        )
        notificationService.createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "회원 차단",
            "${member.realname} 회원의 접속이 차단되었습니다.",
        )
    }

    @Transactional
    fun unblockMember(userId: Long) {
        val member =
            memberRepository.findById(userId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }
        if (member.status != UserStatus.INACTIVE) {
            throw UnprocessableEntityException("차단된 회원이 아닙니다")
        }

        memberRepository.save(
            member.copy(
                status = UserStatus.ACTIVE,
                updatedAt = LocalDateTime.now(),
            ),
        )
        discordMemberService.reflectUserInfoToDiscordUser(userId)

        pointService.givePoint(
            targetUserId = userId,
            point = 700,
            message = "사이트 접속 차단이 해제되었습니다.",
        )
        notificationService.createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "회원 차단 해제",
            "${member.realname} 회원의 접속 차단이 해제되었습니다.",
        )
    }

    @Transactional
    fun expelMember(
        requesterUserId: Long,
        targetUserId: Long,
    ) {
        if (requesterUserId == targetUserId) {
            throw BadRequestException("자기 자신을 제명할 수 없습니다.")
        }

        val member =
            memberRepository.findById(targetUserId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }
        if (member.status == UserStatus.UNAUTHORIZED) {
            throw BadRequestException("이미 탈퇴한 회원입니다.")
        }

        memberRepository.save(
            member.copy(
                name = targetUserId.toString(),
                password = "",
                email = "",
                phone = "",
                homepage = "",
                language = "",
                prefer = "",
                number = 0L,
                expoint = 0L,
                status = UserStatus.UNAUTHORIZED,
                manager = false,
                slack = null,
                returnAt = null,
                returnReason = null,
                updatedAt = LocalDateTime.now(),
            ),
        )
        discordMemberService.clearDiscordIntegration(targetUserId)
    }

    @Transactional
    fun deleteMember(userId: Long) {
        val member =
            memberRepository.findById(userId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }
        memberRepository.save(
            member.copy(
                password = "",
                number = 0L,
                email = "",
                phone = "",
                homepage = "",
                language = "",
                prefer = "",
                expoint = 0L,
                status = UserStatus.UNAUTHORIZED,
                manager = false,
                slack = null,
                returnAt = null,
                returnReason = null,
                updatedAt = LocalDateTime.now(),
            ),
        )
        discordMemberService.clearDiscordIntegration(userId)
    }

    fun listMembers(
        email: String?,
        phone: String?,
        name: String?,
        number: String?,
        college: String?,
        grade: Int?,
        studentStatus: StudentStatus?,
        tag: MemberTagFilter?,
        limit: Int,
        offset: Int,
    ): Pair<Long, List<MemberWithTags>> {
        val members = memberRepository.findMembers(email, phone, name, number, college, grade, studentStatus, tag, limit, offset)
        val count = memberRepository.countMembers(email, phone, name, number, college, grade, studentStatus, tag)

        val feeTargetUserIds = members.filter { it.needPayFee() }.map { it.id }
        val thisTerm = UnivPeriod.fromDate(LocalDate.now(ZoneId.of("Asia/Seoul"))).toTerm()
        val paidUserIds =
            if (feeTargetUserIds.isEmpty()) {
                emptySet()
            } else {
                feeHistoryRepository
                    .findByUserIdInAndYearAndSemester(feeTargetUserIds, thisTerm.year, thisTerm.semester)
                    .map { it.userId }
                    .toSet()
            }

        val membersWithTags =
            members.map { member ->
                val redTags = mutableListOf<String>()
                val normalTags = mutableListOf<String>()

                if (member.needAuth()) redTags.add("미인증")
                if (member.status == UserStatus.INACTIVE) redTags.add("차단")
                if (member.expoint < 0) redTags.add("-exp")

                if (member.needPayFee() && !paidUserIds.contains(member.id)) {
                    if (member.needPayHalfFee()) {
                        normalTags.add("반액 납부 대상")
                    } else {
                        normalTags.add("납부 대상")
                    }
                }

                MemberWithTags(member = member, normalTags = normalTags, redTags = redTags)
            }

        return count to membersWithTags
    }

    @Transactional
    fun checkFirstTodayLogin(userId: Long) {
        val member =
            memberRepository.findById(userId).orElseThrow {
                NotFoundException("사용자를 찾을 수 없습니다")
            }

        val kst = ZoneId.of("Asia/Seoul")
        val nowInstant = Instant.now()
        val nowKst = nowInstant.atZone(kst)
        val lastLoginKst = member.lastLogin.atZone(kst)
        val isFirstEnterToday = lastLoginKst.toLocalDate() != nowKst.toLocalDate()

        memberRepository.save(
            member.copy(
                lastLogin = nowInstant,
                updatedAt = LocalDateTime.ofInstant(nowInstant, ZoneId.of("UTC")),
            ),
        )

        if (isFirstEnterToday) {
            val message = "${nowKst.year}년 ${nowKst.monthValue}월 ${nowKst.dayOfMonth}일 사이트 첫 방문"

            pointService.givePoint(
                targetUserId = userId,
                point = 1,
                message = message,
            )
        }
    }

    @Transactional
    fun appointManager(
        requesterUserId: Long,
        targetUserId: Long,
    ) {
        if (requesterUserId == targetUserId) {
            throw UnprocessableEntityException("스스로를 운영진에 임명할 수 없습니다")
        }

        val targetUser =
            memberRepository.findById(targetUserId).orElseThrow {
                NotFoundException("대상 유저가 존재하지 않습니다")
            }

        if (targetUser.status == UserStatus.UNAUTHORIZED) {
            throw UnprocessableEntityException("인증되지 않은 회원은 운영진에 임명할 수 없습니다")
        }

        if (targetUser.manager) {
            return
        }

        notificationService.createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "신규 운영진 임명",
            "${targetUser.realname} 회원이 운영진으로 임명되었어요.",
        )

        memberRepository.save(targetUser.copy(manager = true))

        notificationService.createNotification(
            targetUserId,
            NotificationCategory.SYSTEM,
            "운영진 임명",
            "쿠러그의 운영진이 된 것을 환영해요. 회원 분들의 멋진 경험을 위해 같이 노력해봐요!",
        )

        logger.info("운영진($requesterUserId)이 회원($targetUserId)에게 운영진 권한을 부여하였습니다.")

        discordMemberService.reflectUserInfoToDiscordUser(targetUserId)
    }

    @Transactional
    fun stepdownManager(
        requesterUserId: Long,
        targetUserId: Long,
    ) {
        if (requesterUserId == targetUserId) {
            throw UnprocessableEntityException("스스로 운영진에서 퇴임할 수 없습니다")
        }

        val targetUser =
            memberRepository.findById(targetUserId).orElseThrow {
                NotFoundException("대상 유저가 존재하지 않습니다")
            }

        if (!targetUser.manager) {
            return
        }

        memberRepository.save(targetUser.copy(manager = false))

        notificationService.createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "운영진 퇴임",
            "${targetUser.realname} 운영진의 업무가 종료되었습니다.",
        )

        // 간단하게 알림만 하고 메시지는 알림톡으로 보내는 건 어떨까 싶기도.
        notificationService.createNotification(
            targetUserId,
            NotificationCategory.SYSTEM,
            "운영진 퇴임",
            listOf(
                "운영진 업무가 종료되었습니다.",
                "그동안 쿠러그를 위해 고생해주셔서 감사합니다.",
                "쿠러그 운영과 관련된 내부 사항은 업무가 종료되었더라도 허가 없이 발설하시면 안 됩니다.",
                "만약 마무리되지 않은 업무가 있는 경우 현 운영진으로부터 이와 관해 연락이 갈 수 있는 점 참고 부탁드립니다.",
            ).joinToString(" "),
        )

        notificationService.createNotification(
            targetUserId,
            NotificationCategory.SYSTEM,
            "운영진 퇴임",
            listOf(
                "${targetUser.realname} 회원님의 기여 덕분에 쿠러그가 이만큼이나 성장할 수 있었습니다.",
                "쿠러그는 ${targetUser.realname} 회원님께서 만들어주신 토대를 바탕으로, 앞으로도 수많은 기회를 만들어내는 플랫폼이 되겠습니다.",
                "이곳에서의 경험이 앞으로 ${targetUser.realname} 회원님이 나아가는데 있어 좋은 발판이 되었으면 좋겠습니다.",
            ).joinToString(" "),
        )

        logger.info("운영진($requesterUserId)이 회원($targetUserId)의 운영진 권한을 제거하였습니다.")

        discordMemberService.reflectUserInfoToDiscordUser(targetUserId)
    }
}
