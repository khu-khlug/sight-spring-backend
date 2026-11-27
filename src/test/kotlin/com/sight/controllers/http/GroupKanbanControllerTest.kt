package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.GroupKanbanQueryService
import com.sight.service.dto.GroupCardSummaryResult
import com.sight.service.dto.GroupKanbanListResult
import com.sight.service.dto.GroupKanbanListsResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.Optional

@WebMvcTest(
    com.sight.controllers.http.GroupKanbanController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupKanbanControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var kanban: GroupKanbanQueryService

    @MockBean
    private lateinit var groupRepository: GroupRepository

    @MockBean
    private lateinit var groupMemberRepository: GroupMemberRepository

    @MockBean
    private lateinit var groupAuthorizationRepository: GroupAuthorizationRepository

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `중단된 공개 그룹의 첫 페이지를 카드 요약과 함께 반환한다`() {
        authenticate()
        given(groupRepository.findById(1L)).willReturn(
            Optional.of(testGroup(id = 1L, visibility = GroupVisibility.PUBLIC, status = GroupStatus.STOP)),
        )
        val card =
            GroupCardSummaryResult(
                id = 9007199254740993L,
                title = "카드",
                authorName = "작성자",
                recordCount = 2,
                hasDescription = true,
                isInPortfolio = false,
                assigneeUserId = 2,
                assigneeName = "담당자",
                coverFileId = "cover",
                labels = listOf("LIGHT_GRAY", "RED"),
                isDisabled = true,
                createdAt = Instant.parse("2026-11-20T00:00:00Z"),
            )
        given(kanban.listGroupKanbanLists(1L, 0L)).willReturn(
            GroupKanbanListsResult(0, 11, listOf(GroupKanbanListResult(33, "리스트", "", 16, listOf(card)))),
        )

        mockMvc.perform(get("/groups/1/lists"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.totalCount").value(11))
            .andExpect(jsonPath("$.receivedCount").value(1))
            .andExpect(jsonPath("$.lists[0].id").value("33"))
            .andExpect(jsonPath("$.lists[0].description").value(""))
            .andExpect(jsonPath("$.lists[0].cardCount").value(16))
            .andExpect(jsonPath("$.lists[0].cards[0].id").value("9007199254740993"))
            .andExpect(jsonPath("$.lists[0].cards[0].isInPortfolio").value(false))
            .andExpect(jsonPath("$.lists[0].cards[0].isDisabled").value(true))
            .andExpect(jsonPath("$.lists[0].cards[0].hasDescription").value(true))
            .andExpect(jsonPath("$.lists[0].cards[0].assigneeUserId").value(2))
            .andExpect(jsonPath("$.lists[0].cards[0].coverImageUrl").value("/groups/1/files/cover"))
            .andExpect(jsonPath("$.lists[0].cards[0].labels[0]").value("lightGray"))
            .andExpect(jsonPath("$.lists[0].cards[0].createdAt").value("2026-11-20T00:00:00Z"))
            .andExpect(jsonPath("$.lists[0].cards[0].description").doesNotExist())

        mockMvc.perform(get("/groups/1/lists").param("page", ""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(0))
    }

    @Test
    fun `잘못된 페이지와 그룹 아이디는 400을 반환한다`() {
        authenticate()
        given(groupRepository.findById(1L)).willReturn(Optional.of(testGroup(id = 1L, visibility = GroupVisibility.PUBLIC)))

        for (page in listOf("-1", "0.5", "abc", "9223372036854775808")) {
            mockMvc.perform(get("/groups/1/lists").param("page", page)).andExpect(status().isBadRequest)
        }
        for (id in listOf("0", "01", "-1", "1.1", "9223372036854775808")) {
            mockMvc.perform(get("/groups/{groupId}/lists", id)).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `미인증과 비공개 그룹 접근과 없는 그룹을 구분한다`() {
        mockMvc.perform(get("/groups/1/lists")).andExpect(status().isUnauthorized)

        authenticate()
        given(groupRepository.findById(1L)).willReturn(
            Optional.of(testGroup(id = 1L, leaderMemberId = 1L, visibility = GroupVisibility.PRIVATE)),
        )
        mockMvc.perform(get("/groups/1/lists")).andExpect(status().isForbidden)
        mockMvc.perform(get("/groups/2/lists")).andExpect(status().isNotFound)
    }

    private fun authenticate() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                Requester(userId = 2L, role = UserRole.USER),
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
    }
}
