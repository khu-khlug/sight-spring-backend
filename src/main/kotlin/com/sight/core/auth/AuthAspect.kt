package com.sight.core.auth

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

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
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다")

        val requiredRoles = auth.roles
        if (!requiredRoles.contains(requester.role)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 부족합니다")
        }

        return joinPoint.proceed()
    }
}
