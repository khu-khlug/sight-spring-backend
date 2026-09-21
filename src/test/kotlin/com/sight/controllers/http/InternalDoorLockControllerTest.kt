package com.sight.controllers.http

import com.sight.core.exception.BadRequestException
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.service.DoorLockMemberService
import com.sight.service.RoomService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(InternalDoorLockController::class, excludeAutoConfiguration = [SecurityAutoConfiguration::class])
class InternalDoorLockControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var doorLockMemberService: DoorLockMemberService

    @MockBean
    private lateinit var roomService: RoomService

    private fun member(
        id: Long,
        realname: String,
        number: Long?,
    ): Member =
        Member(
            id = id,
            name = "user$id",
            realname = realname,
            number = number,
            studentStatus = StudentStatus.UNDERGRADUATE,
            status = UserStatus.ACTIVE,
        )

    @Test
    fun `회원 명단을 이름과 학번으로 반환한다`() {
        given(doorLockMemberService.listDoorLockMembers()).willReturn(
            listOf(
                member(1L, "김철수", 2020001L),
                member(2L, "이영희", 2020002L),
            ),
        )

        mockMvc.perform(get("/internal/door-lock/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(2))
            .andExpect(jsonPath("$.members[0].name").value("김철수"))
            .andExpect(jsonPath("$.members[0].number").value(2020001))
            .andExpect(jsonPath("$.members[1].name").value("이영희"))
            .andExpect(jsonPath("$.members[1].number").value(2020002))
    }

    @Test
    fun `응답에는 이메일·전화번호 등 도어락에 불필요한 필드가 포함되지 않는다`() {
        given(doorLockMemberService.listDoorLockMembers()).willReturn(
            listOf(member(1L, "김철수", 2020001L)),
        )

        mockMvc.perform(get("/internal/door-lock/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.members[0].email").doesNotExist())
            .andExpect(jsonPath("$.members[0].phone").doesNotExist())
            .andExpect(jsonPath("$.members[0].id").doesNotExist())
    }

    @Test
    fun `명단 대상 회원이 없으면 count가 0인 빈 배열을 반환한다`() {
        given(doorLockMemberService.listDoorLockMembers()).willReturn(emptyList())

        mockMvc.perform(get("/internal/door-lock/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.members").isArray)
            .andExpect(jsonPath("$.members").isEmpty)
    }

    @Test
    fun `방별 오늘 방문자 수를 반환한다`() {
        given(roomService.getDailyVisitCount(eq(406))).willReturn(3L)

        mockMvc.perform(get("/internal/door-lock/daily-visit-count").param("room", "406"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(3))
    }

    @Test
    fun `유효하지 않은 방 번호로 조회하면 400을 반환한다`() {
        given(roomService.getDailyVisitCount(eq(999))).willThrow(BadRequestException("유효하지 않은 방 번호입니다"))

        mockMvc.perform(get("/internal/door-lock/daily-visit-count").param("room", "999"))
            .andExpect(status().isBadRequest)
    }
}
