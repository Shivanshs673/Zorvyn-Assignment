package com.example.zorvynassignment

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(exception.status, exception.message, request, emptyList())
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val details = exception.bindingResult.fieldErrors.map { fieldError ->
            "${fieldError.field}: ${fieldError.defaultMessage ?: "invalid value"}"
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val details = exception.constraintViolations.map { violation ->
            "${violation.propertyPath}: ${violation.message}"
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidJson(exception: HttpMessageNotReadableException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request", request, listOfNotNull(exception.mostSpecificCause.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request, listOfNotNull(exception.message))
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
        details: List<String>,
    ): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            timestamp = Instant.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI,
            details = details.filter { it.isNotBlank() },
        )
        return ResponseEntity.status(status).body(body)
    }
}