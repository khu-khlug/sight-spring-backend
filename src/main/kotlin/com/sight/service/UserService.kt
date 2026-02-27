package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
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
}
