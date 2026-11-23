package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.GroupSearchRepository
import com.sight.service.GroupRecordTextExtractor
import com.sight.service.GroupSearchService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verifyNoInteractions
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

// 실제 GroupSearchService를 연결해 검색어 검증이 HTTP 응답(400)까지 이어지는지 확인한다.
@WebMvcTest(
    GroupSearchController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(
    AuthAspect::class,
    GroupAuthAspect::class,
    GroupAuthorizationService::class,
    GroupSearchService::class,
    GroupRecordTextExtractor::class,
    ObjectMapper::class,
)
@EnableAspectJAutoProxy
class GroupSearchRequestValidationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var groupSearchRepository: GroupSearchRepository

    @MockBean
    private lateinit var groupRepository: GroupRepository

    @MockBean
    private lateinit var groupMemberRepository: GroupMemberRepository

    @MockBean
    private lateinit var groupAuthorizationRepository: GroupAuthorizationRepository

    @BeforeEach
    fun setUp() {
        given(groupRepository.findById(GROUP_ID))
            .willReturn(Optional.of(testGroup(id = GROUP_ID, visibility = GroupVisibility.PUBLIC)))
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupSearchRepository.findCardsOrderByLatest(any(), any(), any())).willReturn(emptyList())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                Requester(userId = MEMBER_ID, role = UserRole.USER),
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun search(keyword: String): MockHttpServletRequestBuilder =
        get("/groups/$GROUP_ID/search").param("target", "TITLE").param("keyword", keyword)

    @Test
    fun `공백뿐인 검색어는 400과 한국어 오류 메시지를 반환한다`() {
        mockMvc.perform(search("   "))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("검색어가 필요합니다."))

        verifyNoInteractions(groupSearchRepository)
    }

    @Test
    fun `20자를 넘는 검색어는 400을 반환한다`() {
        mockMvc.perform(search("가".repeat(21)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("검색어는 20자 이하여야 합니다."))

        verifyNoInteractions(groupSearchRepository)
    }

    @Test
    fun `이모지 21개는 400이고 20개는 허용한다`() {
        mockMvc.perform(search("😀".repeat(21))).andExpect(status().isBadRequest)
        mockMvc.perform(search("😀".repeat(20))).andExpect(status().isOk)
    }

    @Test
    fun `20자 검색어는 허용한다`() {
        mockMvc.perform(search("가".repeat(20))).andExpect(status().isOk)
    }

    @Test
    fun `없는 그룹은 검색어가 잘못되어도 404를 반환한다`() {
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.empty())
        given(groupRepository.existsById(GROUP_ID)).willReturn(false)

        mockMvc.perform(search("   ")).andExpect(status().isNotFound)
    }

    companion object {
        private const val GROUP_ID = 1L
        private const val MEMBER_ID = 2L
    }
}
