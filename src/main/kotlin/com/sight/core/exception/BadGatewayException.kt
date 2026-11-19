package com.sight.core.exception

import org.springframework.http.HttpStatus

class BadGatewayException(message: String) : BaseException(HttpStatus.BAD_GATEWAY, message)
