package com.sight.service

import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.repository.ScheduleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleServiceTest {
    private val scheduleRepository: ScheduleRepository = mock()
    private lateinit var scheduleService: ScheduleService

    @BeforeEach
    fun setUp() {
        scheduleService = ScheduleService(scheduleRepository)
    }

    @Test
    fun `listSchedules는 지정된 시간 이후의 일정 목록을 반환한다`() {
        // given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val limit = 5
        val schedules =
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "동아리 정기 모임",
                    author = 1L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2024, 1, 2, 14, 0),
                    endAt = LocalDateTime.of(2024, 1, 2, 16, 0),
                ),
                Schedule(
                    id = 2L,
                    category = ScheduleCategory.SEMINAR,
                    title = "세미나",
                    author = 2L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2024, 1, 3, 18, 0),
                    endAt = LocalDateTime.of(2024, 1, 3, 20, 0),
                ),
            )
        whenever(scheduleRepository.findUpcoming(any(), any())).thenReturn(schedules)

        // when
        val result = scheduleService.listSchedules(from, limit)

        // then
        assertEquals(2, result.size)
        assertEquals("동아리 정기 모임", result[0].title)
        assertEquals("세미나", result[1].title)
        verify(scheduleRepository).findUpcoming(any(), any())
    }

    @Test
    fun `listSchedules는 일정이 없을 때 빈 목록을 반환한다`() {
        // given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val limit = 5
        whenever(scheduleRepository.findUpcoming(any(), any())).thenReturn(emptyList())

        // when
        val result = scheduleService.listSchedules(from, limit)

        // then
        assertTrue(result.isEmpty())
        verify(scheduleRepository).findUpcoming(any(), any())
    }

    @Test
    fun `listSchedules는 limit 개수만큼 일정을 반환한다`() {
        // given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val limit = 2
        val schedules =
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "일정1",
                    author = 1L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2024, 1, 2, 14, 0),
                    endAt = LocalDateTime.of(2024, 1, 2, 16, 0),
                ),
                Schedule(
                    id = 2L,
                    category = ScheduleCategory.ACADEMIC,
                    title = "일정2",
                    author = 1L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2024, 1, 3, 14, 0),
                    endAt = LocalDateTime.of(2024, 1, 3, 16, 0),
                ),
            )
        whenever(scheduleRepository.findUpcoming(any(), any())).thenReturn(schedules)

        // when
        val result = scheduleService.listSchedules(from, limit)

        // then
        assertEquals(2, result.size)
    }
}
