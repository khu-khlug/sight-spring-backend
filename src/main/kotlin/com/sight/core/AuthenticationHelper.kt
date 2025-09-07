package com.sight.core

import com.sight.domain.auth.Requester
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
            ?: throw IllegalStateException("No authenticated user found")
    }
}
