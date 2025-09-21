package com.sight.core.exception

import org.springframework.http.HttpStatus

class ForbiddenException(
    override val message: String,
    override val data: Any? = null,
) : BaseException(
        statusCode = HttpStatus.FORBIDDEN,
        message = message,
        data = data,
    )
