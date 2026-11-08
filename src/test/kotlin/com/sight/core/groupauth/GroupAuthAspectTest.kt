package com.sight.core.groupauth

import com.sight.core.auth.Auth
import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.UnauthorizedException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.willThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupAuthAspectTest {
    private val permissions = mock<GroupAuthorizationService>()
    private val target = ProtectedController()
    private val controller =
        AspectJProxyFactory(target).apply {
            addAspect(AuthAspect())
            addAspect(GroupAuthAspect(permissions))
        }.getProxy<ProtectedController>()

    @AfterEach
    fun `다음 테스트에 로그인 상태가 남지 않도록 비운다`() {
        SecurityContextHolder.clearContext()
    }

    private fun authenticate(role: UserRole) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(Requester(7L, role), null, emptyList())
    }

    @Test
    fun `로그인하지 않았거나 SYSTEM 역할이면 그룹 권한을 조회하기 전에 Auth에서 거부된다`() {
        assertTrue(AnnotationAwareOrderComparator.INSTANCE.compare(AuthAspect(), GroupAuthAspect(permissions)) < 0)
        assertThrows<UnauthorizedException> { controller.record(1L, 10L) }
        authenticate(UserRole.SYSTEM)
        assertThrows<ForbiddenException> { controller.record(1L, 10L) }
        verifyNoInteractions(permissions)
        assertEquals(0, target.calls)
    }

    @Test
    fun `GroupAuth에 지정한 경로 변수의 값으로 권한을 검사하고 통과하면 컨트롤러가 실행된다`() {
        authenticate(UserRole.MANAGER)
        assertEquals("허용", controller.record(1L, 10L))
        verify(permissions).requirePermission(1L, 10L, 7L, true, setOf(GroupPermission.RECORD_AUTHOR, GroupPermission.LEADER), false)
        assertEquals(1, target.calls)
    }

    @Test
    fun `GroupAuth에 기록 ID를 지정하지 않으면 그룹 ID만으로 권한을 검사한다`() {
        authenticate(UserRole.USER)
        assertEquals(1L, controller.group("다른 인자", 1L))
        verify(permissions).requirePermission(1L, null, 7L, false, setOf(GroupPermission.MEMBER), false)
        assertEquals(1, target.calls)
    }

    @Test
    fun `그룹 권한 검사에서 거부되면 컨트롤러가 실행되지 않는다`() {
        authenticate(UserRole.USER)
        willThrow(ForbiddenException("그룹에 대한 권한이 부족합니다.")).given(permissions)
            .requirePermission(1L, 10L, 7L, false, setOf(GroupPermission.RECORD_AUTHOR, GroupPermission.LEADER), false)
        assertThrows<ForbiddenException> { controller.record(1L, 10L) }
        assertEquals(0, target.calls)
    }

    open class ProtectedController {
        var calls = 0

        @Auth([UserRole.USER, UserRole.MANAGER])
        @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
        @GetMapping("/groups/{groupId}")
        open fun group(
            unused: String,
            @PathVariable groupId: Long,
        ): Long {
            calls++
            return groupId
        }

        @Auth([UserRole.USER, UserRole.MANAGER])
        @GroupAuth(
            [GroupPermission.RECORD_AUTHOR, GroupPermission.LEADER],
            groupId = "teamId",
            recordId = "entryId",
            allowManager = false,
        )
        @GetMapping("/groups/{teamId}/records/{entryId}")
        open fun record(
            @PathVariable("teamId") groupId: Long,
            @PathVariable("entryId") recordId: Long,
        ): String {
            calls++
            return "허용"
        }
    }
}
