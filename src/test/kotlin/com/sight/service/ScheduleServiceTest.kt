package com.sight.service

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnauthorizedException
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleMemberApply
import com.sight.domain.schedule.ScheduleState
import com.sight.repository.ScheduleMemberApplyRepository
import com.sight.repository.ScheduleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduleServiceTest {
    private val scheduleRepository: ScheduleRepository = mock()
    private val scheduleMemberApplyRepository: ScheduleMemberApplyRepository = mock()
    private val pointService: PointService = mock()
    private lateinit var scheduleService: ScheduleService

    @BeforeEach
    fun setUp() {
        scheduleService =
            ScheduleService(
                scheduleRepository = scheduleRepository,
                scheduleMemberApplyRepository = scheduleMemberApplyRepository,
                pointService = pointService,
            )
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
        given(scheduleRepository.findUpcoming(any(), any())).willReturn(schedules)

        // when
        val result = scheduleService.listSchedules(from, limit)

        // then
        assertEquals(2, result.size)
        assertEquals("동아리 정기 모임", result[0].title)
        assertEquals("세미나", result[1].title)
        verify(scheduleRepository).findUpcoming(any(), any())
    }

    @Test
    fun `listSchedules는 from이 null이면 findAllActive를 호출한다`() {
        given(scheduleRepository.findAllActive(any())).willReturn(emptyList())

        scheduleService.listSchedules(null, 50)

        verify(scheduleRepository).findAllActive(any())
    }

    @Test
    fun `listSchedules는 일정이 없을 때 빈 목록을 반환한다`() {
        // given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val limit = 5
        given(scheduleRepository.findUpcoming(any(), any())).willReturn(emptyList())

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
    fun `createSchedule은 checkCode가 null이면 null 상태로 저장한다`() {
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
                checkCode = null,
            )

        assertEquals(ScheduleCategory.CLUB, result.category)
        assertEquals(1L, result.author)
        assertEquals("khlug_406", result.location)
        assertNull(result.checkCode)
        verify(scheduleRepository).save(any<Schedule>())
    }

    @Test
    fun `createSchedule은 checkCode가 명시되면 그 값을 그대로 저장한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.createSchedule(
                requester = requester,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
                checkCode = "9999",
            )

        assertEquals("9999", result.checkCode)
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
                checkCode = null,
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
                checkCode = null,
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
    fun `listActiveSchedules는 checkCode가 있고 진행 중인 일정만 반환한다`() {
        val now = LocalDateTime.now()
        val activeSchedule =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "출석 진행 중",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = now.minusHours(1),
                endAt = now.plusHours(1),
                checkCode = "1234",
            )
        val endedSchedule =
            activeSchedule.copy(
                id = 2L,
                title = "종료된 일정",
                scheduledAt = now.minusHours(3),
                endAt = now.minusHours(1),
                checkCode = "1234",
            )
        val futureSchedule =
            activeSchedule.copy(
                id = 3L,
                title = "시작 전 일정",
                scheduledAt = now.plusHours(1),
                endAt = now.plusHours(2),
                checkCode = "1234",
            )
        val noCheckCodeSchedule =
            activeSchedule.copy(
                id = 4L,
                title = "코드 없는 일정",
                checkCode = null,
            )
        given(scheduleRepository.findAttendanceActive(any(), any()))
            .willReturn(listOf(activeSchedule, endedSchedule, futureSchedule, noCheckCodeSchedule))

        val result = scheduleService.listActiveSchedules()

        assertEquals(listOf(activeSchedule), result)
        verify(scheduleRepository).findAttendanceActive(any(), any())
    }

    @Test
    fun `listActiveSchedules는 출석 진행 중인 일정이 없으면 빈 목록을 반환한다`() {
        given(scheduleRepository.findAttendanceActive(any(), any())).willReturn(emptyList())

        val result = scheduleService.listActiveSchedules()

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
        given(scheduleRepository.findUpcoming(any(), any())).willReturn(schedules)

        // when
        val result = scheduleService.listSchedules(from, limit)

        // then
        assertEquals(2, result.size)
    }

    @Test
    fun `listScheduleAttendances는 일정의 출석자 목록을 반환한다`() {
        val scheduleId = 100L
        val schedule =
            Schedule(
                id = scheduleId,
                category = ScheduleCategory.CLUB,
                title = "테스트 일정",
                author = 1L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 15, 10, 0),
                endAt = LocalDateTime.of(2026, 5, 15, 18, 0),
            )
        val createdAt = LocalDateTime.of(2026, 5, 15, 10, 0)
        val applies =
            listOf(
                ScheduleMemberApply(
                    memberId = 1L,
                    scheduleId = scheduleId,
                    attendedAt = LocalDateTime.of(2026, 5, 15, 14, 0),
                    createdAt = createdAt,
                ),
                ScheduleMemberApply(
                    memberId = 2L,
                    scheduleId = scheduleId,
                    attendedAt = null,
                    createdAt = createdAt.plusHours(1),
                ),
            )

        given(scheduleRepository.findActiveById(scheduleId)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId)).willReturn(applies)

        val result = scheduleService.listScheduleAttendances(scheduleId)

        assertEquals(2, result.count)
        assertEquals(2, result.attendances.size)
        assertEquals(1L, result.attendances[0].userId)
        assertTrue(result.attendances[0].isChecked)
        assertEquals(createdAt, result.attendances[0].createdAt)
        assertEquals(2L, result.attendances[1].userId)
        assertFalse(result.attendances[1].isChecked)
        verify(scheduleRepository).findActiveById(scheduleId)
        verify(scheduleMemberApplyRepository).findByScheduleIdOrderByCreatedAtAsc(scheduleId)
    }

    @Test
    fun `listScheduleAttendances는 출석자가 없으면 빈 목록을 반환한다`() {
        val scheduleId = 100L
        val schedule =
            Schedule(
                id = scheduleId,
                category = ScheduleCategory.CLUB,
                title = "테스트 일정",
                author = 1L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 15, 10, 0),
                endAt = LocalDateTime.of(2026, 5, 15, 18, 0),
            )

        given(scheduleRepository.findActiveById(scheduleId)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId))
            .willReturn(emptyList())

        val result = scheduleService.listScheduleAttendances(scheduleId)

        assertEquals(0, result.count)
        assertTrue(result.attendances.isEmpty())
        verify(scheduleRepository).findActiveById(scheduleId)
        verify(scheduleMemberApplyRepository).findByScheduleIdOrderByCreatedAtAsc(scheduleId)
    }

    @Test
    fun `listScheduleAttendances는 존재하지 않는 일정이면 NotFoundException을 발생시킨다`() {
        val scheduleId = 999L

        given(scheduleRepository.findActiveById(scheduleId)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.listScheduleAttendances(scheduleId)
        }

        verify(scheduleRepository).findActiveById(scheduleId)
        verify(scheduleMemberApplyRepository, never()).findByScheduleIdOrderByCreatedAtAsc(any())
    }

    @Test
    fun `checkScheduleAttendance는 유효한 코드로 출석 처리하고 ExPoint를 적립한다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val schedule = attendanceSchedule(expoint = 15, checkCode = "1234")
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(false)
        given(scheduleMemberApplyRepository.save(any<ScheduleMemberApply>()))
            .willAnswer { it.arguments[0] as ScheduleMemberApply }

        val result =
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "1234",
            )

        assertEquals(schedule.id, result.scheduleId)
        assertEquals(requester.userId, result.userId)
        assertEquals(15, result.expointGranted)
        verify(scheduleMemberApplyRepository).save(any<ScheduleMemberApply>())
        verify(pointService).givePoint(requester.userId, 15, "${schedule.title} 출석")
    }

    @Test
    fun `checkScheduleAttendance는 같은 일정에 두 번 출석체크하면 ConflictException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val schedule = attendanceSchedule()
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(true)

        assertThrows<ConflictException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "1234",
            )
        }

        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 코드가 일치하지 않으면 UnauthorizedException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val schedule = attendanceSchedule(checkCode = "1234")
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(false)

        assertThrows<UnauthorizedException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "9999",
            )
        }

        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 출석체크 시간 밖이면 BadRequestException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val now = LocalDateTime.now()
        val beforeSchedule =
            attendanceSchedule(
                scheduledAt = now.plusHours(1),
                endAt = now.plusHours(2),
            )
        val afterSchedule =
            attendanceSchedule(
                scheduledAt = now.minusHours(2),
                endAt = now.minusHours(1),
            )
        given(scheduleRepository.findActiveById(beforeSchedule.id)).willReturn(beforeSchedule, afterSchedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, beforeSchedule.id)).willReturn(false)

        assertThrows<BadRequestException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = beforeSchedule.id,
                code = "1234",
            )
        }
        assertThrows<BadRequestException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = beforeSchedule.id,
                code = "1234",
            )
        }

        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 checkCode가 null이면 BadRequestException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val schedule = attendanceSchedule(checkCode = null)
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(false)

        assertThrows<BadRequestException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "1234",
            )
        }

        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 expoint가 0이어도 출석 처리하고 포인트는 적립하지 않는다`() {
        val requester = Requester(userId = 10L, role = UserRole.MANAGER)
        val schedule = attendanceSchedule(expoint = 0, checkCode = "1234")
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(false)
        given(scheduleMemberApplyRepository.save(any<ScheduleMemberApply>()))
            .willAnswer { it.arguments[0] as ScheduleMemberApply }

        val result =
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "1234",
            )

        assertEquals(schedule.id, result.scheduleId)
        assertEquals(requester.userId, result.userId)
        assertEquals(0, result.expointGranted)
        verify(scheduleMemberApplyRepository).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 존재하지 않는 일정이면 NotFoundException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val scheduleId = 999L
        given(scheduleRepository.findActiveById(scheduleId)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = scheduleId,
                code = "1234",
            )
        }

        verify(scheduleMemberApplyRepository, never()).existsByMemberIdAndScheduleId(any(), any())
        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `removeScheduleAttendance는 출석 기록을 삭제하고 ExPoint를 회수한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val userId = 10L
        val schedule = attendanceSchedule(expoint = 15)
        val attendance =
            ScheduleMemberApply(
                memberId = userId,
                scheduleId = schedule.id,
                attendedAt = LocalDateTime.now().minusMinutes(10),
            )
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByMemberIdAndScheduleId(userId, schedule.id)).willReturn(attendance)

        scheduleService.removeScheduleAttendance(
            requester = requester,
            scheduleId = schedule.id,
            userId = userId,
        )

        verify(scheduleMemberApplyRepository).delete(attendance)
        verify(pointService).givePoint(userId, -15, "${schedule.title} 출석 취소")
    }

    @Test
    fun `removeScheduleAttendance는 ExPoint 회수를 음수 적립으로 기록한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val userId = 10L
        val schedule = attendanceSchedule(expoint = 7)
        val attendance =
            ScheduleMemberApply(
                memberId = userId,
                scheduleId = schedule.id,
                attendedAt = LocalDateTime.now().minusMinutes(10),
            )
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByMemberIdAndScheduleId(userId, schedule.id)).willReturn(attendance)

        scheduleService.removeScheduleAttendance(
            requester = requester,
            scheduleId = schedule.id,
            userId = userId,
        )

        verify(pointService).givePoint(userId, -schedule.expoint, "${schedule.title} 출석 취소")
    }

    @Test
    fun `removeScheduleAttendance는 출석한 적이 없으면 NotFoundException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val userId = 10L
        val schedule = attendanceSchedule()
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByMemberIdAndScheduleId(userId, schedule.id)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.removeScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                userId = userId,
            )
        }

        verify(scheduleMemberApplyRepository, never()).delete(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `removeScheduleAttendance는 MANAGER가 아니면 ForbiddenException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.removeScheduleAttendance(
                requester = requester,
                scheduleId = 100L,
                userId = 10L,
            )
        }

        verify(scheduleRepository, never()).findActiveById(any())
        verify(scheduleMemberApplyRepository, never()).delete(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `removeScheduleAttendance는 일정이 없으면 NotFoundException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val scheduleId = 999L
        given(scheduleRepository.findActiveById(scheduleId)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.removeScheduleAttendance(
                requester = requester,
                scheduleId = scheduleId,
                userId = 10L,
            )
        }

        verify(scheduleMemberApplyRepository, never()).findByMemberIdAndScheduleId(any(), any())
        verify(scheduleMemberApplyRepository, never()).delete(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `removeScheduleAttendance는 같은 출석 기록을 두 번 삭제하면 두 번째는 NotFoundException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val userId = 10L
        val schedule = attendanceSchedule()
        val attendance =
            ScheduleMemberApply(
                memberId = userId,
                scheduleId = schedule.id,
                attendedAt = LocalDateTime.now().minusMinutes(10),
            )
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByMemberIdAndScheduleId(userId, schedule.id)).willReturn(attendance, null)

        scheduleService.removeScheduleAttendance(
            requester = requester,
            scheduleId = schedule.id,
            userId = userId,
        )
        assertThrows<NotFoundException> {
            scheduleService.removeScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                userId = userId,
            )
        }

        verify(scheduleMemberApplyRepository).delete(attendance)
        verify(pointService).givePoint(userId, -schedule.expoint, "${schedule.title} 출석 취소")
    }

    private fun attendanceSchedule(
        id: Long = 100L,
        expoint: Int = 10,
        checkCode: String? = "1234",
        scheduledAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1),
    ): Schedule {
        return Schedule(
            id = id,
            category = ScheduleCategory.CLUB,
            title = "출석 테스트 일정",
            author = 1L,
            state = ScheduleState.PUBLIC,
            scheduledAt = scheduledAt,
            endAt = endAt,
            expoint = expoint,
            checkCode = checkCode,
        )
    }
}
