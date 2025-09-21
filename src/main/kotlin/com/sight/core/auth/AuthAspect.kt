package com.sight.core.auth

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.UnauthorizedException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class AuthAspect {
    @Around("@annotation(auth)")
    fun checkAuth(
        joinPoint: ProceedingJoinPoint,
        auth: Auth,
    ): Any? {
        val requester =
            AuthenticationHelper.getCurrentRequester()
                ?: throw UnauthorizedException("인증이 필요합니다")

        val requiredRoles = auth.roles
        if (!requiredRoles.contains(requester.role)) {
            throw ForbiddenException("권한이 부족합니다")
        }

        return joinPoint.proceed()
    }
}
