package com.sight.controllers.http

import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.service.ScheduleService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(ActiveScheduleController::class, excludeAutoConfiguration = [SecurityAutoConfiguration::class])
class AttendanceHistoryControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var scheduleService: ScheduleService

    @Test
    fun `year를 쿼리스트링으로 넘기면 해당 연도의 출석코드가 있는 일정 목록을 반환한다`() {
        given(scheduleService.listAttendanceHistory(2026)).willReturn(
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "동아리 정기 모임",
                    author = 10L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                    endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                    checkCode = "1234",
                ),
            ),
        )

        mockMvc.perform(get("/attendance-history").param("year", "2026"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.schedules[0].id").value(1))
            .andExpect(jsonPath("$.schedules[0].title").value("동아리 정기 모임"))
            .andExpect(jsonPath("$.schedules[0].category").value("CLUB"))
            .andExpect(jsonPath("$.schedules[0].scheduledAt").exists())
            .andExpect(jsonPath("$.schedules[0].endAt").exists())

        verify(scheduleService).listAttendanceHistory(2026)
    }

    @Test
    fun `year 쿼리스트링이 없으면 null을 서비스로 전달한다`() {
        given(scheduleService.listAttendanceHistory(null)).willReturn(emptyList())

        mockMvc.perform(get("/attendance-history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.schedules").isArray)
            .andExpect(jsonPath("$.schedules").isEmpty)

        verify(scheduleService).listAttendanceHistory(null)
    }

    @Test
    fun `응답에는 checkCode 필드가 포함되지 않는다`() {
        given(scheduleService.listAttendanceHistory(any())).willReturn(
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "동아리 정기 모임",
                    author = 10L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                    endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                    checkCode = "1234",
                ),
            ),
        )

        mockMvc.perform(get("/attendance-history").param("year", "2026"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.schedules[0].checkCode").doesNotExist())
    }
}
