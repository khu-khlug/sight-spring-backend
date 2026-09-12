package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.doorlock.DoorLockOccupancyState
import com.sight.domain.doorlock.DoorLockOccupant
import com.sight.repository.DoorLockOccupancyStateRepository
import com.sight.repository.DoorLockOccupantRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class DoorLockOccupancyResult(
    val occupantNames: List<String>,
    val lastSyncedAt: Instant?,
)

@Service
class DoorLockOccupancyService(
    private val occupancyStateRepository: DoorLockOccupancyStateRepository,
    private val occupantRepository: DoorLockOccupantRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun updateOccupants(
        numbers: List<Long>,
        syncedAt: Instant = Instant.now(),
    ) {
        if (numbers.distinct().size != numbers.size) {
            throw BadRequestException("중복된 학번이 포함되어 있습니다")
        }

        val membersByNumber =
            if (numbers.isEmpty()) {
                emptyMap()
            } else {
                memberRepository.findAllByNumberIn(numbers).associateBy { it.number }
            }

        if (membersByNumber.keys != numbers.toSet()) {
            throw BadRequestException("등록되지 않은 학번이 포함되어 있습니다")
        }

        val state =
            occupancyStateRepository.findByIdForUpdate(DoorLockOccupancyState.SINGLETON_ID)
                ?: DoorLockOccupancyState()

        occupantRepository.deleteAllInBatch()
        occupantRepository.saveAll(
            numbers.map { number ->
                DoorLockOccupant(userId = membersByNumber.getValue(number).id)
            },
        )
        state.markSyncedAt(syncedAt)
        occupancyStateRepository.save(state)
    }

    @Transactional(readOnly = true)
    fun listOccupants(): DoorLockOccupancyResult {
        val state = occupancyStateRepository.findById(DoorLockOccupancyState.SINGLETON_ID).orElse(null)
        val occupantUserIds = occupantRepository.findAllByOrderByUserIdAsc().map { it.userId }
        val membersById = memberRepository.findAllById(occupantUserIds).associateBy { it.id }
        val names = occupantUserIds.map { userId -> membersById.getValue(userId).realname }

        return DoorLockOccupancyResult(
            occupantNames = names,
            lastSyncedAt = state?.lastSyncedAt,
        )
    }
}
