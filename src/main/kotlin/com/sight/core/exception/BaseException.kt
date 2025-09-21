package com.sight.core.exception

import org.springframework.http.HttpStatus

open class BaseException(
    open val statusCode: HttpStatus,
    override val message: String,
    open val data: Any? = null,
) : RuntimeException(
        message,
    )
