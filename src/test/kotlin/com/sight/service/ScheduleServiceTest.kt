package com.sight.service

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.domain.seminar.BigSeminar
import com.sight.repository.BigSeminarRepository
import com.sight.repository.ScheduleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduleServiceTest {
    private val scheduleRepository: ScheduleRepository = mock()
    private val bigSeminarRepository: BigSeminarRepository = mock()
    private lateinit var scheduleService: ScheduleService

    @BeforeEach
    fun setUp() {
        scheduleService =
            ScheduleService(
                scheduleRepository = scheduleRepository,
                bigSeminarRepository = bigSeminarRepository,
            )
    }

    @Test
    fun `listSchedules는 지정된 시간 이후의 일정 목록을 반환한다`() {
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
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

        val result = scheduleService.listSchedules(from, 5)

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
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        given(scheduleRepository.findUpcoming(any(), any())).willReturn(emptyList())

        val result = scheduleService.listSchedules(from, 5)

        assertTrue(result.isEmpty())
        verify(scheduleRepository).findUpcoming(any(), any())
    }

    @Test
    fun `listSchedules는 limit 개수만큼 일정을 반환한다`() {
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val schedules =
            listOf(
                scheduleOf(id = 1L),
                scheduleOf(id = 2L, category = ScheduleCategory.ACADEMIC),
            )
        given(scheduleRepository.findUpcoming(any(), any())).willReturn(schedules)

        val result = scheduleService.listSchedules(from, 2)

        assertEquals(2, result.size)
    }

    @Test
    fun `getScheduleById는 존재하는 일정을 반환한다`() {
        val schedule = scheduleOf(id = 1L, category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(schedule)

        val result = scheduleService.getScheduleById(1L)

        assertEquals(1L, result.id)
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
    fun `createSchedule은 generateCheckCode가 false면 checkCode를 null로 저장한다`() {
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.createSchedule(
                requesterUserId = 1L,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = "khlug_406",
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 10,
                generateCheckCode = false,
            )

        assertEquals(ScheduleCategory.CLUB, result.category)
        assertEquals(1L, result.author)
        assertEquals("khlug_406", result.location)
        assertNull(result.checkCode)
        verify(scheduleRepository).save(any<Schedule>())
    }

    @Test
    fun `createSchedule은 generateCheckCode가 true면 4자리 숫자 checkCode를 생성한다`() {
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.createSchedule(
                requesterUserId = 1L,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
                generateCheckCode = true,
            )

        val checkCode = result.checkCode
        assertNotNull(checkCode)
        assertTrue(checkCode.matches(Regex("^\\d{4}$")))
    }

    @Test
    fun `createSchedule은 운영진 카테고리가 아니면 BadRequestException 던진다`() {
        listOf(ScheduleCategory.GROUP_ACTIVITY, ScheduleCategory.SEMINAR).forEach { category ->
            assertThrows<BadRequestException> {
                scheduleService.createSchedule(
                    requesterUserId = 1L,
                    title = "test",
                    category = category,
                    location = null,
                    scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                    endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                    expoint = 0,
                    generateCheckCode = false,
                )
            }
        }
    }

    @Test
    fun `createSchedule은 endAt이 scheduledAt 이후가 아니면 BadRequestException 던진다`() {
        assertThrows<BadRequestException> {
            scheduleService.createSchedule(
                requesterUserId = 1L,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                expoint = 0,
                generateCheckCode = false,
            )
        }
    }

    @Test
    fun `updateSchedule은 운영진 카테고리 일정을 수정하며 카테고리는 유지된다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB, checkCode = "1234")
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.updateSchedule(
                id = 1L,
                title = "new",
                location = "khlug_406",
                scheduledAt = LocalDateTime.of(2026, 5, 20, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 20, 16, 0),
                expoint = 5,
            )

        assertEquals("new", result.title)
        assertEquals(ScheduleCategory.CLUB, result.category)
        assertEquals("1234", result.checkCode)
        assertEquals(5, result.expoint)
    }

    @Test
    fun `updateSchedule은 대상이 운영진 카테고리가 아니면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)

        assertThrows<BadRequestException> {
            scheduleService.updateSchedule(
                id = 1L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `updateSchedule은 없는 일정에 NotFoundException 던진다`() {
        given(scheduleRepository.findActiveById(999L)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.updateSchedule(
                id = 999L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `updateScheduleCategory는 SEMINAR로 변경 시 빅세미나 레코드를 생성한다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(null)
        given(bigSeminarRepository.save(any<BigSeminar>())).willAnswer { it.arguments[0] as BigSeminar }

        val (schedule, bigSeminar) =
            scheduleService.updateScheduleCategory(
                id = 1L,
                category = ScheduleCategory.SEMINAR,
                isSummerSeason = true,
                isSpeakAfter = false,
            )

        assertEquals(ScheduleCategory.SEMINAR, schedule.category)
        assertNotNull(bigSeminar)
        verify(bigSeminarRepository).save(any<BigSeminar>())
    }

    @Test
    fun `updateScheduleCategory는 SEMINAR로 변경하는데 빅세미나 필드가 없으면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        assertThrows<BadRequestException> {
            scheduleService.updateScheduleCategory(
                id = 1L,
                category = ScheduleCategory.SEMINAR,
                isSummerSeason = null,
                isSpeakAfter = null,
            )
        }
    }

    @Test
    fun `updateScheduleCategory는 SEMINAR에서 다른 카테고리로 변경 시 빅세미나 레코드를 삭제한다`() {
        val existing = scheduleOf(category = ScheduleCategory.SEMINAR)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val (schedule, bigSeminar) =
            scheduleService.updateScheduleCategory(
                id = 1L,
                category = ScheduleCategory.CLUB,
                isSummerSeason = null,
                isSpeakAfter = null,
            )

        assertEquals(ScheduleCategory.CLUB, schedule.category)
        assertNull(bigSeminar)
        verify(bigSeminarRepository).deleteByScheduleId(1L)
    }

    @Test
    fun `updateScheduleCategory는 GROUP_ACTIVITY로 변경 시 BadRequestException 던진다`() {
        assertThrows<BadRequestException> {
            scheduleService.updateScheduleCategory(
                id = 1L,
                category = ScheduleCategory.GROUP_ACTIVITY,
                isSummerSeason = null,
                isSpeakAfter = null,
            )
        }
    }

    @Test
    fun `deleteSchedule은 운영진 일정을 삭제한다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)

        scheduleService.deleteSchedule(1L)

        verify(scheduleRepository).deleteActiveById(1L)
    }

    @Test
    fun `deleteSchedule은 대상이 운영진 카테고리가 아니면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)

        assertThrows<BadRequestException> {
            scheduleService.deleteSchedule(1L)
        }
    }

    @Test
    fun `deleteSchedule은 없는 일정에 NotFoundException 던진다`() {
        given(scheduleRepository.findActiveById(999L)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.deleteSchedule(999L)
        }
    }

    @Test
    fun `createGroupActivitySchedule은 GROUP_ACTIVITY로 expoint 0 checkCode 없이 생성한다`() {
        val requester = Requester(userId = 1L, role = UserRole.USER)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.createGroupActivitySchedule(
                requester = requester,
                title = "스터디",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )

        assertEquals(ScheduleCategory.GROUP_ACTIVITY, result.category)
        assertEquals(0, result.expoint)
        assertEquals(1L, result.author)
        assertNull(result.checkCode)
    }

    @Test
    fun `createBigSeminarSchedule은 SEMINAR 일정과 빅세미나 레코드를 함께 생성한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        given(bigSeminarRepository.save(any<BigSeminar>())).willAnswer { it.arguments[0] as BigSeminar }

        val (schedule, bigSeminar) =
            scheduleService.createBigSeminarSchedule(
                requester = requester,
                title = "총회",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 50,
                generateCheckCode = false,
                isSummerSeason = true,
                isSpeakAfter = false,
            )

        assertEquals(ScheduleCategory.SEMINAR, schedule.category)
        assertEquals(schedule.id, bigSeminar.scheduleId)
        assertTrue(bigSeminar.isSummerSeason)
        verify(bigSeminarRepository).save(any<BigSeminar>())
    }

    @Test
    fun `updateGroupActivitySchedule은 본인 작성 그룹활동을 수정한다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 10L, role = UserRole.USER)

        val result =
            scheduleService.updateGroupActivitySchedule(
                requester = requester,
                id = 1L,
                title = "new",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 20, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 20, 16, 0),
            )

        assertEquals("new", result.title)
        assertEquals(ScheduleCategory.GROUP_ACTIVITY, result.category)
    }

    @Test
    fun `updateGroupActivitySchedule은 USER가 타인 작성 일정을 수정하면 ForbiddenException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 99L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.updateGroupActivitySchedule(
                requester = requester,
                id = 1L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        }
    }

    @Test
    fun `updateGroupActivitySchedule은 대상이 그룹활동이 아니면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 10L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.updateGroupActivitySchedule(
                requester = requester,
                id = 1L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        }
    }

    @Test
    fun `updateBigSeminarSchedule은 세미나 일정과 빅세미나 정보를 갱신한다`() {
        val existing = scheduleOf(category = ScheduleCategory.SEMINAR)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(null)
        given(bigSeminarRepository.save(any<BigSeminar>())).willAnswer { it.arguments[0] as BigSeminar }
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        val (schedule, bigSeminar) =
            scheduleService.updateBigSeminarSchedule(
                requester = requester,
                id = 1L,
                title = "new",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 20, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 20, 16, 0),
                expoint = 50,
                isSummerSeason = false,
                isSpeakAfter = true,
            )

        assertEquals("new", schedule.title)
        assertEquals(ScheduleCategory.SEMINAR, schedule.category)
        assertTrue(bigSeminar.isSpeakAfter)
    }

    @Test
    fun `updateBigSeminarSchedule은 대상이 세미나가 아니면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.updateBigSeminarSchedule(
                requester = requester,
                id = 1L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
                isSummerSeason = true,
                isSpeakAfter = true,
            )
        }
    }

    @Test
    fun `deleteGroupActivitySchedule은 본인 작성 그룹활동을 삭제한다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 10L, role = UserRole.USER)

        scheduleService.deleteGroupActivitySchedule(requester, 1L)

        verify(scheduleRepository).deleteActiveById(1L)
    }

    @Test
    fun `deleteGroupActivitySchedule은 USER가 타인 작성 일정을 삭제하면 ForbiddenException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 99L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.deleteGroupActivitySchedule(requester, 1L)
        }
    }

    @Test
    fun `deleteBigSeminarSchedule은 세미나 일정과 빅세미나를 삭제한다`() {
        val existing = scheduleOf(category = ScheduleCategory.SEMINAR)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        scheduleService.deleteBigSeminarSchedule(requester, 1L)

        verify(scheduleRepository).deleteActiveById(1L)
        verify(bigSeminarRepository).deleteByScheduleId(1L)
    }

    private fun scheduleOf(
        id: Long = 1L,
        category: ScheduleCategory = ScheduleCategory.CLUB,
        author: Long = 10L,
        checkCode: String? = null,
    ): Schedule {
        return Schedule(
            id = id,
            category = category,
            title = "일정",
            author = author,
            state = ScheduleState.PUBLIC,
            scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
            endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            checkCode = checkCode,
        )
    }
}
