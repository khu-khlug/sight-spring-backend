package com.sight.core.exception

import org.springframework.http.HttpStatus

class InternalServerErrorException(
    override val message: String,
    override val data: Any? = null,
) : BaseException(
        statusCode = HttpStatus.INTERNAL_SERVER_ERROR,
        message = message,
        data = data,
    )
