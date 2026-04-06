package com.example.zorvynassignment

import org.springframework.http.HttpStatus

open class ApiException(
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message)

class BadRequestException(message: String) : ApiException(HttpStatus.BAD_REQUEST, message)

class NotFoundException(message: String) : ApiException(HttpStatus.NOT_FOUND, message)

class UnauthorizedException(message: String = "Missing or invalid user header") :
    ApiException(HttpStatus.UNAUTHORIZED, message)

class ForbiddenException(message: String = "You do not have permission to perform this action") :
    ApiException(HttpStatus.FORBIDDEN, message)

class TooManyRequestsException(message: String = "Rate limit exceeded") :
    ApiException(HttpStatus.TOO_MANY_REQUESTS, message)