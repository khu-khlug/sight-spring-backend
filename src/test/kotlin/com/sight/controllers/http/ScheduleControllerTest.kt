package com.sight.controllers.http

import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.service.ScheduleService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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

@WebMvcTest(ScheduleController::class, excludeAutoConfiguration = [SecurityAutoConfiguration::class])
class ScheduleControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var scheduleService: ScheduleService

    @Test
    fun `일정 목록 조회 응답에 신규 컬럼이 포함된다`() {
        val schedule =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "동아리 정기 모임",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                location = "khlug_406",
                expoint = 10,
                checkCode = "1234",
            )
        given(scheduleService.listSchedules(anyOrNull(), any())).willReturn(listOf(schedule))

        mockMvc.perform(get("/schedules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.schedules[0].id").value(1))
            .andExpect(jsonPath("$.schedules[0].title").value("동아리 정기 모임"))
            .andExpect(jsonPath("$.schedules[0].location").value("khlug_406"))
            .andExpect(jsonPath("$.schedules[0].expoint").value(10))
            .andExpect(jsonPath("$.schedules[0].author").value(10))
            .andExpect(jsonPath("$.schedules[0].scheduledAt").exists())
            .andExpect(jsonPath("$.schedules[0].endAt").exists())
            .andExpect(jsonPath("$.schedules[0].state").value("public"))
    }

    @Test
    fun `일정 목록 응답에는 checkCode 필드가 포함되지 않는다`() {
        val schedule =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "test",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.now(),
                endAt = LocalDateTime.now().plusHours(1),
                checkCode = "1234",
            )
        given(scheduleService.listSchedules(anyOrNull(), any())).willReturn(listOf(schedule))

        mockMvc.perform(get("/schedules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.schedules[0].checkCode").doesNotExist())
    }

    @Test
    fun `인증 없이도 일정 목록을 조회할 수 있다`() {
        given(scheduleService.listSchedules(anyOrNull(), any())).willReturn(emptyList())

        mockMvc.perform(get("/schedules"))
            .andExpect(status().isOk)
    }
}
