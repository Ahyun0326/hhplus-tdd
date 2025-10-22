package io.hhplus.tdd.point

import io.hhplus.tdd.exception.InsufficientPointException
import io.hhplus.tdd.exception.NegativePointException

data class UserPoint(
    val id: Long,
    val point: Long,
    val updateMillis: Long,
) {
    fun ensurePositiveAmount(amount: Long) {
        if (amount <= 0) {
            throw NegativePointException()
        }
    }

    fun ensureSufficientPoints(amount: Long) {
        if (point < amount) {
            throw InsufficientPointException()
        }
    }
}
