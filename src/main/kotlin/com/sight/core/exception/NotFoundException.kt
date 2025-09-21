package com.sight.core.exception

import org.springframework.http.HttpStatus

class NotFoundException(
    override val message: String,
    override val data: Any? = null,
) : BaseException(
        statusCode = HttpStatus.NOT_FOUND,
        message = message,
        data = data,
    )
