package com.sight.core.auth

import org.springframework.security.core.context.SecurityContextHolder

object AuthenticationHelper {
    fun getCurrentRequester(): Requester? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.isAuthenticated == true) {
            authentication.principal as? Requester
        } else {
            null
        }
    }

    fun requireCurrentRequester(): Requester {
        return getCurrentRequester()
            ?: throw IllegalStateException("인증된 사용자가 없습니다")
    }
}
