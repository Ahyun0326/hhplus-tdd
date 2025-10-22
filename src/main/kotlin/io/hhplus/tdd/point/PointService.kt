package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service

@Service
class PointService(
    private val pointHistoryTable: PointHistoryTable,
    private val userPointTable: UserPointTable
) {

    private val mutex = Mutex()

    suspend fun getPointById(id: Long): UserPoint {
        return mutex.withLock {
            userPointTable.selectById(id)
        }
    }

    suspend fun getPointHistoriesById(id: Long): List<PointHistory> {
        return mutex.withLock {
            pointHistoryTable.selectAllByUserId(id)
        }
    }

    suspend fun charge(id: Long, amount: Long): UserPoint {
        return mutex.withLock {
            val userPoint = userPointTable.selectById(id)

            userPoint.ensurePositiveAmount(amount)

            val totalAmount = amount + userPoint.point
            val result = userPointTable.insertOrUpdate(id, totalAmount)

            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, result.updateMillis)

            result
        }
    }

    suspend fun use(id: Long, amount: Long): UserPoint {
        return mutex.withLock {
            val userPoint = userPointTable.selectById(id)

            userPoint.ensurePositiveAmount(amount)
            userPoint.ensureSufficientPoints(amount)

            val remainedAmount = userPoint.point - amount
            val result = userPointTable.insertOrUpdate(id, remainedAmount)

            pointHistoryTable.insert(id, remainedAmount, TransactionType.USE, result.updateMillis)

            result
        }
    }

}