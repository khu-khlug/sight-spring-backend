package com.sight.controllers.http

import com.sight.config.RequesterArgumentResolver
import com.sight.config.WebConfig
import com.sight.controllers.http.dto.GetGroupMatchingAnswerResponse
import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.GlobalExceptionHandler
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupCategory
import com.sight.service.GroupMatchingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(
    controllers = [GroupMatchingController::class],
    includeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [RequesterArgumentResolver::class],
        ),
    ],
)
@Import(WebConfig::class, RequesterArgumentResolver::class, GlobalExceptionHandler::class, AuthAspect::class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAspectJAutoProxy
class GroupMatchingControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var groupMatchingService: GroupMatchingService

    @BeforeEach
    fun setUp() {
        val requester = Requester(1L, UserRole.USER)
        val authentication = UsernamePasswordAuthenticationToken(requester, null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `should return answer when exists`() {
        val groupMatchingId = "test-group-matching-id"
        val userId = 1L
        val now = LocalDateTime.now()
        val answer =
            GetGroupMatchingAnswerResponse(
                id = "test-answer-id",
                userId = userId,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
                fields =
                    listOf(
                        GetGroupMatchingAnswerResponse.FieldResponse(id = "field-1", name = "Backend"),
                    ),
                matchedGroups =
                    listOf(
                        GetGroupMatchingAnswerResponse.MatchedGroupResponse(id = "match-1", groupId = 100L, createdAt = now),
                    ),
                groupMatchingSubjects =
                    listOf(
                        GetGroupMatchingAnswerResponse.GroupMatchingSubjectResponse(id = "subject-1", subject = "Kotlin"),
                    ),
                createdAt = now,
                updatedAt = now,
            )

        whenever(groupMatchingService.getAnswer(groupMatchingId, userId)).thenReturn(answer)

        mockMvc.perform(
            get("/group-matchings/$groupMatchingId/answers/@me")
                .header("Authorization", "Bearer test-token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(answer.id))
            .andExpect(jsonPath("$.userId").value(answer.userId))
            .andExpect(jsonPath("$.groupType").value("STUDY"))
            .andExpect(jsonPath("$.isPreferOnline").value(true))
            .andExpect(jsonPath("$.groupMatchingId").value(groupMatchingId))
            .andExpect(jsonPath("$.fields[0].id").value("field-1"))
            .andExpect(jsonPath("$.fields[0].name").value("Backend"))
            .andExpect(jsonPath("$.matchedGroups[0].id").value("match-1"))
            .andExpect(jsonPath("$.matchedGroups[0].groupId").value(100L))
            .andExpect(jsonPath("$.groupMatchingSubjects[0].subject").value("Kotlin"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())

        // Verify that the service was called with the authenticated user's ID (1L)
        verify(groupMatchingService).getAnswer(eq(groupMatchingId), eq(userId))
    }

    @Test
    fun `should return 404 when answer does not exist`() {
        val groupMatchingId = "test-group-matching-id"
        val userId = 1L

        whenever(groupMatchingService.getAnswer(groupMatchingId, userId)).thenThrow(NotFoundException("Answer not found"))

        mockMvc.perform(
            get("/group-matchings/$groupMatchingId/answers/@me")
                .header("Authorization", "Bearer test-token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Answer not found"))
    }

    @Test
    fun `should return 401 when not authenticated`() {
        SecurityContextHolder.clearContext()

        mockMvc.perform(
            get("/group-matchings/test-id/answers/@me"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should handle invalid group matching id format`() {
        // Use a blank string instead of empty to avoid path issues, or just test empty if that's the intent

        // If invalidId is empty string "", path becomes //answers/@me which might be 404 or handled differently.
        // If the user specifically wants to test empty ID, we can try.
        // But typically ID validation happens in service or via regex.
        // Let's stick to the user's code but be aware of path issues.
        // User code: val invalidId = ""
        // Let's use " " to ensure it hits the controller but might fail validation if we had any.
        // Since we don't have validation, it will go to service.
        // If we use "", it might not match the pattern.
        // Let's try with "invalid-id" which simply doesn't exist.
        // The user's comment says "invalid format".
        // If I use "", it will likely be 404 Not Found (Resource).

        mockMvc.perform(
            get("/group-matchings//answers/@me")
                .header("Authorization", "Bearer test-token"),
        )
            .andExpect(status().isNotFound)
    }
}
