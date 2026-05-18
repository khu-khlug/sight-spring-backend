package com.sight.service

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.repository.ScheduleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun `getScheduleById는 존재하는 일정을 반환한다`() {
        val schedule =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "동아리 정기 모임",
                author = 1L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(schedule)

        val result = scheduleService.getScheduleById(1L)

        assertEquals(1L, result.id)
        assertEquals("동아리 정기 모임", result.title)
        verify(scheduleRepository).findActiveById(1L)
    }

    @Test
    fun `getScheduleById는 존재하지 않는 일정에 NotFoundException을 던진다`() {
        given(scheduleRepository.findActiveById(999L)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.getScheduleById(999L)
        }
    }

    @Test
    fun `createSchedule은 정상 등록한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.createSchedule(
                requester = requester,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = "khlug_406",
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 10,
            )

        assertEquals(ScheduleCategory.CLUB, result.category)
        assertEquals(1L, result.author)
        assertEquals("khlug_406", result.location)
        assertNotNull(result.checkCode)
        verify(scheduleRepository).save(any<Schedule>())
    }

    @Test
    fun `createSchedule은 USER가 그룹활동 외 카테고리 생성 시도 시 ForbiddenException 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.createSchedule(
                requester = requester,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `createSchedule은 endAt이 scheduledAt 이후가 아니면 BadRequestException 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.createSchedule(
                requester = requester,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `updateSchedule은 기존 일정을 수정한다`() {
        val existing =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "old",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                checkCode = "1234",
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 99L, role = UserRole.MANAGER)

        val result =
            scheduleService.updateSchedule(
                requester = requester,
                id = 1L,
                title = "new",
                category = ScheduleCategory.SEMINAR,
                location = "khlug_406",
                scheduledAt = LocalDateTime.of(2026, 5, 20, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 20, 16, 0),
                expoint = 5,
            )

        assertEquals("new", result.title)
        assertEquals(ScheduleCategory.SEMINAR, result.category)
        assertEquals("1234", result.checkCode)
    }

    @Test
    fun `updateSchedule은 없는 일정에 NotFoundException 던진다`() {
        given(scheduleRepository.findActiveById(999L)).willReturn(null)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<NotFoundException> {
            scheduleService.updateSchedule(
                requester = requester,
                id = 999L,
                title = "x",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `updateSchedule은 USER가 타인 작성 일정을 수정 시도 시 ForbiddenException 던진다`() {
        val existing =
            Schedule(
                id = 1L,
                category = ScheduleCategory.GROUP_ACTIVITY,
                title = "x",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 99L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.updateSchedule(
                requester = requester,
                id = 1L,
                title = "x",
                category = ScheduleCategory.GROUP_ACTIVITY,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `updateSchedule은 USER가 본인 작성 그룹활동을 다른 카테고리로 변경 시도 시 ForbiddenException 던진다`() {
        val existing =
            Schedule(
                id = 1L,
                category = ScheduleCategory.GROUP_ACTIVITY,
                title = "x",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 10L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.updateSchedule(
                requester = requester,
                id = 1L,
                title = "x",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `deleteSchedule은 일정 state를 TRASH로 전환한다`() {
        val existing =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "x",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 99L, role = UserRole.MANAGER)

        scheduleService.deleteSchedule(requester, 1L)

        val captor = org.mockito.kotlin.argumentCaptor<Schedule>()
        verify(scheduleRepository).save(captor.capture())
        assertEquals(ScheduleState.TRASH, captor.firstValue.state)
    }

    @Test
    fun `deleteSchedule은 없는 일정에 NotFoundException 던진다`() {
        given(scheduleRepository.findActiveById(999L)).willReturn(null)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<NotFoundException> {
            scheduleService.deleteSchedule(requester, 999L)
        }
    }

    @Test
    fun `deleteSchedule은 USER가 타인 작성 일정을 삭제 시도 시 ForbiddenException 던진다`() {
        val existing =
            Schedule(
                id = 1L,
                category = ScheduleCategory.GROUP_ACTIVITY,
                title = "x",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 99L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.deleteSchedule(requester, 1L)
        }
    }

    @Test
    fun `deleteSchedule은 USER가 본인 작성 비그룹활동 일정을 삭제 시도 시 ForbiddenException 던진다`() {
        val existing =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "x",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 10L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.deleteSchedule(requester, 1L)
        }
    }

    @Test
    fun `listInProgressSchedules는 진행 중인 일정 목록을 반환한다`() {
        // given
        val schedules =
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "진행 중",
                    author = 1L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.now().minusHours(1),
                    endAt = LocalDateTime.now().plusHours(1),
                ),
            )
        whenever(scheduleRepository.findInProgress(any(), any())).thenReturn(schedules)

        // when
        val result = scheduleService.listInProgressSchedules(5)

        // then
        assertEquals(1, result.size)
        assertEquals("진행 중", result[0].title)
        verify(scheduleRepository).findInProgress(any(), any())
    }

    @Test
    fun `listInProgressSchedules는 일정이 없을 때 빈 목록을 반환한다`() {
        whenever(scheduleRepository.findInProgress(any(), any())).thenReturn(emptyList())

        val result = scheduleService.listInProgressSchedules(5)

        assertTrue(result.isEmpty())
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
