package com.sight.core

import com.sight.domain.auth.UserRole

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Auth(val roles: Array<UserRole>)
