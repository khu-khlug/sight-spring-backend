package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.GroupSearchItem
import com.sight.service.GroupSearchResult
import com.sight.service.GroupSearchService
import com.sight.service.GroupSearchTarget
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@WebMvcTest(
    GroupSearchController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupSearchControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var groupSearchService: GroupSearchService

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

    private fun givenGroup(
        visibility: GroupVisibility,
        status: GroupStatus = GroupStatus.PROGRESS,
        category: GroupCategory = GroupCategory.STUDY,
    ) {
        given(groupRepository.findById(GROUP_ID))
            .willReturn(
                Optional.of(
                    testGroup(
                        id = GROUP_ID,
                        leaderMemberId = LEADER_ID,
                        visibility = visibility,
                        status = status,
                        category = category,
                    ),
                ),
            )
        given(groupMemberRepository.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)
    }

    private fun givenResult() {
        given(groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.RECORD, "스프링", 1))
            .willReturn(
                GroupSearchResult(
                    page = 1,
                    totalPages = 1,
                    results =
                        listOf(
                            GroupSearchItem(
                                listId = 10,
                                listTitle = "할 일",
                                cardId = 20,
                                cardTitle = "스터디",
                                recordId = 30,
                                matchedText = "스프링 백엔드",
                            ),
                        ),
                ),
            )
    }

    @Test
    fun `검색 결과의 아이디는 10진 문자열로 반환한다`() {
        givenGroup(GroupVisibility.PRIVATE)
        givenResult()

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/search").param("target", "RECORD").param("keyword", "스프링"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.receivedCount").value(1))
            .andExpect(jsonPath("$.results[0].listId").value("10"))
            .andExpect(jsonPath("$.results[0].cardId").value("20"))
            .andExpect(jsonPath("$.results[0].recordId").value("30"))
            .andExpect(jsonPath("$.results[0].matchedText").value("스프링 백엔드"))
    }

    @Test
    fun `공개 범위에 따라 검색할 수 있는 회원이 정해진다`() {
        givenResult()

        givenGroup(GroupVisibility.PRIVATE)
        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(search()).andExpect(status().isForbidden)

        givenGroup(GroupVisibility.PUBLIC)
        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(search()).andExpect(status().isOk)
    }

    @Test
    fun `운영진 공개 그룹은 운영진만 검색할 수 있다`() {
        givenResult()
        givenGroup(GroupVisibility.MANAGER_ONLY, category = GroupCategory.MANAGE)

        // 운영 그룹에는 운영진만 참가하므로 운영진 공개는 일반 회원 역할의 요청자를 허용하지 않는다.
        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(search()).andExpect(status().isForbidden)

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(search()).andExpect(status().isForbidden)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(search()).andExpect(status().isOk)
    }

    @Test
    fun `그룹 상태와 관계없이 중단·종료 그룹에서도 검색할 수 있다`() {
        givenResult()
        authenticate(MEMBER_ID, UserRole.USER)

        for (status in listOf(GroupStatus.STOP, GroupStatus.SUCCESS, GroupStatus.FAIL)) {
            givenGroup(GroupVisibility.PRIVATE, status = status)
            mockMvc.perform(search()).andExpect(status().isOk)
        }
    }

    @Test
    fun `인증되지 않으면 401을 반환한다`() {
        givenGroup(GroupVisibility.PUBLIC)
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        mockMvc.perform(search()).andExpect(status().isUnauthorized)
    }

    @Test
    fun `그룹이 없으면 404를 반환한다`() {
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.empty())

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(search()).andExpect(status().isNotFound)
    }

    @Test
    fun `검색어가 없거나 공백이거나 21자이면 400을 반환한다`() {
        givenGroup(GroupVisibility.PUBLIC)
        authenticate(MEMBER_ID, UserRole.USER)

        mockMvc.perform(get("/groups/$GROUP_ID/search").param("target", "TITLE")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/groups/$GROUP_ID/search").param("target", "TITLE").param("keyword", "   "))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get("/groups/$GROUP_ID/search").param("target", "TITLE").param("keyword", "가".repeat(21)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `target이 없거나 허용하지 않는 값이면 400을 반환한다`() {
        givenGroup(GroupVisibility.PUBLIC)
        authenticate(MEMBER_ID, UserRole.USER)

        mockMvc.perform(get("/groups/$GROUP_ID/search").param("keyword", "스프링")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/groups/$GROUP_ID/search").param("target", "CONTENT").param("keyword", "스프링"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `page가 1보다 작거나 숫자가 아니면 400을 반환한다`() {
        givenGroup(GroupVisibility.PUBLIC)
        authenticate(MEMBER_ID, UserRole.USER)

        for (page in listOf("0", "-1", "1.5", "abc")) {
            mockMvc.perform(search().param("page", page)).andExpect(status().isBadRequest)
        }
    }

    private fun search() = get("/groups/$GROUP_ID/search").param("target", "RECORD").param("keyword", "스프링")

    private fun authenticate(
        userId: Long,
        role: UserRole,
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                Requester(userId = userId, role = role),
                null,
                listOf(SimpleGrantedAuthority("ROLE_${role.name}")),
            )
    }

    companion object {
        private const val GROUP_ID = 1L
        private const val LEADER_ID = 1L
        private const val MEMBER_ID = 2L
        private const val OUTSIDER_ID = 3L
        private const val MANAGER_ID = 9L
    }
}
