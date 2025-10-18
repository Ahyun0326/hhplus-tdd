package io.hhplus.tdd.advice

import io.hhplus.tdd.exception.InsufficientPointException
import io.hhplus.tdd.exception.NegativePointException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

data class ErrorResponse(val code: String, val message: String)

@RestControllerAdvice
class ApiControllerAdvice : ResponseEntityExceptionHandler() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse("500", "에러가 발생했습니다."),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }

    @ExceptionHandler(NegativePointException::class)
    fun handleNegativePointException(e: NegativePointException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse("400", "포인트는 0보다 커야 합니다."),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(InsufficientPointException::class)
    fun handleInsufficientPointException(e: InsufficientPointException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse("400", "잔고가 부족하여 포인트를 사용할 수 없습니다."),
            HttpStatus.BAD_REQUEST
        )
    }
}