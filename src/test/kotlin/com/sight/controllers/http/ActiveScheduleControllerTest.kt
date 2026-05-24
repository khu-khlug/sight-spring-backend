package com.sight.controllers.http

import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.service.ScheduleService
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
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
class ActiveScheduleControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var scheduleService: ScheduleService

    @Test
    fun `출석 진행 중인 일정이 있으면 schedule 객체를 반환한다`() {
        given(scheduleService.listActiveSchedules()).willReturn(
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "출석 진행 중",
                    author = 10L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                    endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                    location = "khlug_406",
                    expoint = 10,
                    checkCode = "1234",
                ),
            ),
        )

        mockMvc.perform(get("/active-schedules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.schedules[0].id").value(1))
            .andExpect(jsonPath("$.schedules[0].title").value("출석 진행 중"))
            .andExpect(jsonPath("$.schedules[0].category").value("CLUB"))
            .andExpect(jsonPath("$.schedules[0].location").value("khlug_406"))
            .andExpect(jsonPath("$.schedules[0].scheduledAt").exists())
            .andExpect(jsonPath("$.schedules[0].endAt").exists())
            .andExpect(jsonPath("$.schedules[0].expoint").value(10))
            .andExpect(jsonPath("$.schedules[0].author").value(10))
            .andExpect(jsonPath("$.schedule.title").value("출석 진행 중"))
    }

    @Test
    fun `출석 진행 중인 일정이 없으면 schedule은 null이다`() {
        given(scheduleService.listActiveSchedules()).willReturn(emptyList())

        mockMvc.perform(get("/active-schedules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.schedules").isArray)
            .andExpect(jsonPath("$.schedules").isEmpty)
            .andExpect(jsonPath("$.schedule").value(nullValue()))
    }

    @Test
    fun `출석 진행 중인 일정 응답에는 checkCode 필드가 포함되지 않는다`() {
        given(scheduleService.listActiveSchedules()).willReturn(
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "출석 진행 중",
                    author = 10L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.now().minusHours(1),
                    endAt = LocalDateTime.now().plusHours(1),
                    checkCode = "1234",
                ),
            ),
        )

        mockMvc.perform(get("/active-schedules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.schedules[0].checkCode").doesNotExist())
            .andExpect(jsonPath("$.schedule.checkCode").doesNotExist())
    }
}
