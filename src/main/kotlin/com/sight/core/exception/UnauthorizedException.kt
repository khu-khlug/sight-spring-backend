package com.sight.core.exception

import org.springframework.http.HttpStatus

class UnauthorizedException(
    override val message: String,
    override val data: Any? = null,
) : BaseException(
        statusCode = HttpStatus.UNAUTHORIZED,
        message = message,
        data = data,
    )
