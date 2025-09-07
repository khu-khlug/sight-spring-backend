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
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")

        val requiredRoles = auth.roles
        if (!requiredRoles.contains(requester.role)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient privileges")
        }

        return joinPoint.proceed()
    }
}
