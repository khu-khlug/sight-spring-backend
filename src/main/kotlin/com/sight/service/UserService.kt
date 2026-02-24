package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.member.Member
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class UserService(
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val discordMemberService: DiscordMemberService,
    private val memberRepository: MemberRepository,
    private val pointService: PointService,
    private val notificationService: NotificationService,
) {
    fun applyUserInfoToEnteredDiscordUser(discordUserId: String) {
        val discordIntegration = discordIntegrationRepository.findByDiscordUserId(discordUserId) ?: return

        val userId = discordIntegration.userId
        discordMemberService.reflectUserInfoToDiscordUser(userId)
    }

    fun getMemberById(userId: Long): Member {
        return memberRepository.findById(userId).orElseThrow {
            NotFoundException("사용자를 찾을 수 없습니다")
        }
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

            notificationService.createNotification(
                userId = userId,
                category = NotificationCategory.SYSTEM,
                title = "일일 첫 방문",
                content = "오늘의 첫 방문을 축하합니다! 포인트 1점이 지급되었습니다.",
            )
        }
    }
}
