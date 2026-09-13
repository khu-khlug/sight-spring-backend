package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.doorlock.DoorLockOccupancyState
import com.sight.domain.doorlock.DoorLockOccupant
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.DoorLockOccupancyStateRepository
import com.sight.repository.DoorLockOccupantRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DoorLockOccupancyServiceTest {
    private val occupancyStateRepository = mock<DoorLockOccupancyStateRepository>()
    private val occupantRepository = mock<DoorLockOccupantRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val service =
        DoorLockOccupancyService(
            occupancyStateRepository = occupancyStateRepository,
            occupantRepository = occupantRepository,
            memberRepository = memberRepository,
        )

    @Test
    fun `재실자 snapshot과 동기화 시각을 한 번에 교체한다`() {
        val syncedAt = Instant.parse("2026-09-12T10:30:00Z")
        val state = DoorLockOccupancyState()
        given(memberRepository.findAllByNumberIn(listOf(2025101234L, 2025109999L)))
            .willReturn(
                listOf(
                    member(id = 1L, number = 2025101234L, realname = "김철수"),
                    member(id = 2L, number = 2025109999L, realname = "홍길동"),
                ),
            )
        given(occupancyStateRepository.findByIdForUpdate(DoorLockOccupancyState.SINGLETON_ID))
            .willReturn(state)

        service.updateOccupants(
            numbers = listOf(2025101234L, 2025109999L),
            syncedAt = syncedAt,
        )

        verify(occupantRepository).deleteAllInBatch()
        val occupants = argumentCaptor<List<DoorLockOccupant>>()
        verify(occupantRepository).saveAll(occupants.capture())
        assertEquals(listOf(1L, 2L), occupants.firstValue.map { it.userId })
        assertEquals(syncedAt, state.lastSyncedAt)
        verify(occupancyStateRepository).save(state)
    }

    @Test
    fun `빈 snapshot은 기존 재실자를 모두 제거하고 동기화 시각을 갱신한다`() {
        val syncedAt = Instant.parse("2026-09-12T10:30:00Z")
        val state = DoorLockOccupancyState()
        given(occupancyStateRepository.findByIdForUpdate(DoorLockOccupancyState.SINGLETON_ID))
            .willReturn(state)

        service.updateOccupants(numbers = emptyList(), syncedAt = syncedAt)

        verify(memberRepository, never()).findAllByNumberIn(emptyList())
        verify(occupantRepository).deleteAllInBatch()
        verify(occupantRepository).saveAll(emptyList())
        assertEquals(syncedAt, state.lastSyncedAt)
        verify(occupancyStateRepository).save(state)
    }

    @Test
    fun `최초 snapshot이면 동기화 상태 행을 함께 생성한다`() {
        val syncedAt = Instant.parse("2026-09-12T10:30:00Z")
        given(occupancyStateRepository.findByIdForUpdate(DoorLockOccupancyState.SINGLETON_ID))
            .willReturn(null)

        service.updateOccupants(numbers = emptyList(), syncedAt = syncedAt)

        val state = argumentCaptor<DoorLockOccupancyState>()
        verify(occupancyStateRepository).save(state.capture())
        assertEquals(DoorLockOccupancyState.SINGLETON_ID, state.firstValue.id)
        assertEquals(syncedAt, state.firstValue.lastSyncedAt)
    }

    @Test
    fun `중복 학번이 포함되면 기존 snapshot을 변경하지 않는다`() {
        assertFailsWith<BadRequestException> {
            service.updateOccupants(listOf(2025101234L, 2025101234L))
        }

        verify(occupancyStateRepository, never()).findByIdForUpdate(DoorLockOccupancyState.SINGLETON_ID)
        verify(occupantRepository, never()).deleteAllInBatch()
    }

    @Test
    fun `등록되지 않은 학번이 포함되면 기존 snapshot을 변경하지 않는다`() {
        given(memberRepository.findAllByNumberIn(listOf(2025101234L, 2025109999L)))
            .willReturn(listOf(member(id = 1L, number = 2025101234L, realname = "김철수")))

        assertFailsWith<BadRequestException> {
            service.updateOccupants(listOf(2025101234L, 2025109999L))
        }

        verify(occupancyStateRepository, never()).findByIdForUpdate(DoorLockOccupancyState.SINGLETON_ID)
        verify(occupantRepository, never()).deleteAllInBatch()
    }

    @Test
    fun `재실자의 이름과 마지막 동기화 시각을 반환한다`() {
        val syncedAt = Instant.parse("2026-09-12T10:30:00Z")
        given(occupancyStateRepository.findById(DoorLockOccupancyState.SINGLETON_ID))
            .willReturn(Optional.of(DoorLockOccupancyState(lastSyncedAt = syncedAt)))
        given(occupantRepository.findAllByOrderByUserIdAsc())
            .willReturn(listOf(DoorLockOccupant(1L), DoorLockOccupant(2L)))
        given(memberRepository.findAllById(listOf(1L, 2L)))
            .willReturn(
                listOf(
                    member(id = 2L, number = 2025109999L, realname = "홍길동"),
                    member(id = 1L, number = 2025101234L, realname = "김철수"),
                ),
            )

        val result = service.listOccupants()

        assertEquals(listOf("김철수", "홍길동"), result.occupantNames)
        assertEquals(syncedAt, result.lastSyncedAt)
    }

    @Test
    fun `아직 동기화하지 않았으면 빈 목록과 null 동기화 시각을 반환한다`() {
        given(occupancyStateRepository.findById(DoorLockOccupancyState.SINGLETON_ID))
            .willReturn(Optional.empty())
        given(occupantRepository.findAllByOrderByUserIdAsc()).willReturn(emptyList())

        val result = service.listOccupants()

        assertEquals(emptyList(), result.occupantNames)
        assertNull(result.lastSyncedAt)
    }

    private fun member(
        id: Long,
        number: Long,
        realname: String,
    ): Member =
        Member(
            id = id,
            name = "user$id",
            number = number,
            realname = realname,
            studentStatus = StudentStatus.UNDERGRADUATE,
            status = UserStatus.ACTIVE,
        )
}
