package com.sight.core.exception

import org.springframework.http.HttpStatus

class UnprocessableEntityException(
    override val message: String,
    override val data: Any? = null,
) : BaseException(
        statusCode = HttpStatus.UNPROCESSABLE_ENTITY,
        message = message,
        data = data,
    )
