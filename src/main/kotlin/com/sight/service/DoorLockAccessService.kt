package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

data class CreateDoorLockAccessResult(
    val name: String,
)

@Service
class DoorLockAccessService(
    private val memberRepository: MemberRepository,
    private val pointService: PointService,
) {
    @Transactional
    fun createDoorLockAccess(
        number: Long,
        roomNumber: Int,
        now: LocalDateTime = LocalDateTime.now(KST),
    ): CreateDoorLockAccessResult {
        if (number <= 0) {
            throw BadRequestException("학번은 양수여야 합니다")
        }
        if (roomNumber <= 0) {
            throw BadRequestException("방 번호는 양수여야 합니다")
        }

        val member =
            memberRepository.findByNumber(number)
                ?: throw NotFoundException("사용자를 찾을 수 없습니다")
        val isFirstAccessToday = member.lastEnter.toLocalDate() != now.toLocalDate()

        memberRepository.save(member.copy(lastEnter = now))

        if (isFirstAccessToday) {
            pointService.givePoint(
                targetUserId = member.id,
                point = DOOR_LOCK_ACCESS_EXPOINT,
                message = "동방 출입",
            )
        }

        return CreateDoorLockAccessResult(name = member.realname)
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private const val DOOR_LOCK_ACCESS_EXPOINT = 1
    }
}
