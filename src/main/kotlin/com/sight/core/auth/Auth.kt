package com.sight.core.auth

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Auth(val roles: Array<UserRole>)
