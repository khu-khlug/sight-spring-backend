package com.sight.core.exception

import org.springframework.http.HttpStatus

class ConflictException(
    override val message: String,
    override val data: Any? = null,
) : BaseException(
        statusCode = HttpStatus.CONFLICT,
        message = message,
        data = data,
    )
