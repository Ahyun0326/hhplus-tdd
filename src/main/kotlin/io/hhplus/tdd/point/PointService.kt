package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Service

@Service
class PointService(
    private val pointHistoryTable: PointHistoryTable,
    private val userPointTable: UserPointTable
) {

    fun getPointById(id: Long): UserPoint {
        return userPointTable.selectById(id)
    }

    fun getPointHistoriesById(id: Long): List<PointHistory> {
        return pointHistoryTable.selectAllByUserId(id)
    }

    fun charge(id: Long, amount: Long): UserPoint {
        val userPoint = userPointTable.selectById(id)

        userPoint.ensurePositiveAmount(amount)

        val totalAmount = amount + userPoint.point
        val result = userPointTable.insertOrUpdate(id, totalAmount)

        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, result.updateMillis)

        return result
    }

    fun use(id: Long, amount: Long): UserPoint {
        val userPoint = userPointTable.selectById(id)

        userPoint.ensurePositiveAmount(amount)
        userPoint.ensureSufficientPoints(amount)

        val remainedAmount = userPoint.point - amount
        val result = userPointTable.insertOrUpdate(id, remainedAmount)

        pointHistoryTable.insert(id, remainedAmount, TransactionType.USE, result.updateMillis)

        return result
    }

}