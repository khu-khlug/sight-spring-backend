package com.sight.core.exception

import org.springframework.http.HttpStatus

class BadRequestException(
    override val message: String,
    override val data: Any? = null,
) : BaseException(
        statusCode = HttpStatus.BAD_REQUEST,
        message = message,
        data = data,
    )
