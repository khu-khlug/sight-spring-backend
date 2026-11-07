package com.sight.controllers.http

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
import com.sight.service.GroupLogListResult
import com.sight.service.GroupLogService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@WebMvcTest(
    GroupLogController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupLogControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var groupLogService: GroupLogService

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
    fun `활동 로그는 완전 공개 그룹에서도 그룹원만 조회할 수 있다`() {
        given(groupRepository.findById(GROUP_ID))
            .willReturn(Optional.of(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID, visibility = GroupVisibility.PUBLIC)))
        given(groupMemberRepository.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)
        given(groupLogService.listGroupLogs(GROUP_ID, 0))
            .willReturn(GroupLogListResult(skip = 0, totalCount = 0, receivedCount = 0, logs = emptyList()))

        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/logs")).andExpect(status().isForbidden)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(get("/groups/$GROUP_ID/logs")).andExpect(status().isForbidden)

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/logs")).andExpect(status().isOk)
    }

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
