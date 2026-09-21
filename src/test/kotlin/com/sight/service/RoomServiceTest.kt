package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.room.DailyVisitLog
import com.sight.repository.DailyVisitLogRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDate
import kotlin.test.assertEquals

class RoomServiceTest {
    private val dailyVisitLogRepository: DailyVisitLogRepository = mock()
    private val service = RoomService(dailyVisitLogRepository)

    @Test
    fun `유효하지 않은 방 번호면 BadRequestException을 던진다`() {
        assertThrows<BadRequestException> {
            service.getDailyVisitCount(999)
        }
    }

    @Test
    fun `유효한 방 번호면 오늘 날짜 기준 방문자 수를 반환한다`() {
        given(dailyVisitLogRepository.countByDateAndRoomNumber(any(), eq(406)))
            .willReturn(3L)

        val result = service.getDailyVisitCount(406)

        assertEquals(3L, result)
    }

    @Test
    fun `아무도 방문하지 않았으면 0을 반환한다`() {
        given(dailyVisitLogRepository.countByDateAndRoomNumber(any(), eq(406)))
            .willReturn(0L)

        val result = service.getDailyVisitCount(406)

        assertEquals(0L, result)
    }

    @Test
    fun `upsertDailyVisitLog는 기존 기록이 없으면 새로 저장한다`() {
        given(dailyVisitLogRepository.findByRoomNumberAndMemberId(406, 1L)).willReturn(null)

        service.upsertDailyVisitLog(406, 1L, LocalDate.of(2026, 5, 31))

        val captor = argumentCaptor<DailyVisitLog>()
        verify(dailyVisitLogRepository).save(captor.capture())
        assertEquals(406, captor.firstValue.roomNumber)
        assertEquals(1L, captor.firstValue.memberId)
        assertEquals(LocalDate.of(2026, 5, 31), captor.firstValue.date)
    }

    @Test
    fun `upsertDailyVisitLog는 기존 기록이 있으면 새로 만들지 않고 날짜만 갱신한다`() {
        val existing = DailyVisitLog(roomNumber = 406, memberId = 1L, date = LocalDate.of(2026, 5, 20))
        given(dailyVisitLogRepository.findByRoomNumberAndMemberId(406, 1L)).willReturn(existing)

        service.upsertDailyVisitLog(406, 1L, LocalDate.of(2026, 5, 31))

        verify(dailyVisitLogRepository).save(existing)
        assertEquals(LocalDate.of(2026, 5, 31), existing.date)
    }

    @Test
    fun `같은 방에 같은 회원이 여러 번 방문해도 row는 하나만 유지된다`() {
        given(dailyVisitLogRepository.findByRoomNumberAndMemberId(406, 1L)).willReturn(null)

        service.upsertDailyVisitLog(406, 1L, LocalDate.of(2026, 5, 29))

        val captor = argumentCaptor<DailyVisitLog>()
        verify(dailyVisitLogRepository).save(captor.capture())
        val createdLog = captor.firstValue

        // 두 번째, 세 번째 방문부터는 실제 저장소가 방금 만든 row를 돌려주는 상황을 흉내낸다
        given(dailyVisitLogRepository.findByRoomNumberAndMemberId(406, 1L)).willReturn(createdLog)

        service.upsertDailyVisitLog(406, 1L, LocalDate.of(2026, 5, 30))
        service.upsertDailyVisitLog(406, 1L, LocalDate.of(2026, 5, 31))

        // save()는 세 번 호출되지만 매번 같은 인스턴스라 실제로는 row 하나만 갱신되는 셈이다
        verify(dailyVisitLogRepository, org.mockito.kotlin.times(3)).save(createdLog)
        assertEquals(LocalDate.of(2026, 5, 31), createdLog.date)
    }

    @Test
    fun `같은 회원이 다른 방을 방문하면 방마다 별도 row가 생긴다`() {
        given(dailyVisitLogRepository.findByRoomNumberAndMemberId(405, 1L)).willReturn(null)
        given(dailyVisitLogRepository.findByRoomNumberAndMemberId(406, 1L)).willReturn(null)

        service.upsertDailyVisitLog(405, 1L, LocalDate.of(2026, 5, 31))
        service.upsertDailyVisitLog(406, 1L, LocalDate.of(2026, 5, 31))

        val captor = argumentCaptor<DailyVisitLog>()
        verify(dailyVisitLogRepository, org.mockito.kotlin.times(2)).save(captor.capture())
        val savedRoomNumbers = captor.allValues.map { it.roomNumber }
        assertEquals(listOf(405, 406), savedRoomNumbers)
    }
}
