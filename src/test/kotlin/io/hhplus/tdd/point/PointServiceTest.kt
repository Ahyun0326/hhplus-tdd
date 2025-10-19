package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.exception.InsufficientPointException
import io.hhplus.tdd.exception.NegativePointException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class PointServiceTest : BehaviorSpec({

    val pointHistoryTable = mockk<PointHistoryTable>()
    val userPointTable = mockk<UserPointTable>()

    val pointService = PointService(pointHistoryTable, userPointTable)

    given("회원 id로") {
        val id = 1L
        val userPoint = UserPoint(id, 1000L, 1704067200000L)

        every { userPointTable.selectById(id) } returns userPoint

        `when`("포인트를 조회하면") {
            val result = pointService.getPointById(id)

            then("조회된 회원의 UserPoint가 반환된다.") {
                result shouldBe userPoint
            }
        }
    }

    given("사용 내역이 없는") {
        val id = 1L
        val pointHistories = mutableListOf<PointHistory>()

        every { pointHistoryTable.selectAllByUserId(id) } returns pointHistories

        `when`("회원의 포인트 내역을 조회하면") {
            val result = pointService.getPointHistoriesById(id)

            then("빈 PointHistory 리스트가 반환된다") {
                result shouldBe pointHistories
                result.size shouldBe 0
            }
        }
    }

    given("사용 내역이 있는") {
        val id = 1L

        val pointHistory1 = PointHistory(1L, id, TransactionType.CHARGE, 2000L, 1704067200000L)
        val pointHistory2 = PointHistory(2L, id, TransactionType.USE, 1000L, 1704067200000L)

        every { pointHistoryTable.selectAllByUserId(id) } returns mutableListOf(pointHistory1, pointHistory2)

        `when`("회원의 포인트 내역을 조회하면") {
            val result = pointService.getPointHistoriesById(id)

            then("비어있지 않은 PointHistory 리스트가 반환된다.") {
                result.size shouldBe 2
                result[0] shouldBe pointHistory1
                result[1] shouldBe pointHistory2
            }
        }
    }

    given("포인트 충전을 위해 회원의 id와 amount가 주어졌을 때") {
        val id = 1L
        val amount = 2000L
        val initialPoint = 1000L
        val totalPoint = initialPoint + amount

        val initialUserPoint = UserPoint(id, initialPoint, 1704067200000L)
        val updatedUserPoint = UserPoint(id, totalPoint, 1704067200000L)
        val pointHistory = PointHistory(1L, id, TransactionType.CHARGE, amount, 1704067200000L)

        every { userPointTable.selectById(id) } returns initialUserPoint
        every { userPointTable.insertOrUpdate(id, totalPoint) } returns updatedUserPoint
        every { pointHistoryTable.insert(id, amount, TransactionType.CHARGE, 1704067200000L) } returns pointHistory

        `when`("회원의 포인트를 충전하면") {
            val result = pointService.charge(id, amount)

            then("포인트가 올바르게 증가하고, 충전 내역이 기록된다.") {
                result shouldBe updatedUserPoint
                result.point shouldBe totalPoint

                verify(exactly = 1) {
                    pointHistoryTable.insert(
                        id, amount, TransactionType.CHARGE, 1704067200000L
                    )
                }
            }
        }
    }

    given("amount가 음수일 때") {
        val id = 1L
        val amount = -1000L
        val userPoint = UserPoint(id, 1000L, 1704067200000L)

        every { userPointTable.selectById(id) } returns userPoint

        `when`("회원의 포인트를 충전하면") {
            then("NegativePointException이 발생한다") {
                shouldThrow<NegativePointException> {
                    pointService.charge(id, amount)
                }
            }
        }

        `when`("회원의 포인트를 사용하면") {
            then("NegativePointException이 발생한다") {
                shouldThrow<NegativePointException> {
                    pointService.use(id, amount)
                }
            }
        }
    }

    given("amount가 0일 때") {
        val id = 1L
        val amount = 0L
        val userPoint = UserPoint(id, 1000L, 1704067200000L)

        every { userPointTable.selectById(id) } returns userPoint

        `when`("회원의 포인트를 충전하면") {
            then("NegativePointException이 발생한다") {
                shouldThrow<NegativePointException> {
                    pointService.charge(id, amount)
                }
            }
        }

        `when`("회원의 포인트를 사용하면") {
            then("NegativePointException이 발생한다") {
                shouldThrow<NegativePointException> {
                    pointService.use(id, amount)
                }
            }
        }
    }

    given("포인트 사용을 위해 회원의 id와 amount가 주어졌을 때") {
        val id = 1L
        val amount = 1000L
        val initialPoint = 2000L
        val totalPoint = initialPoint - amount

        val initialUserPoint = UserPoint(id, initialPoint, 1704067200000L)
        val updatedUserPoint = UserPoint(id, totalPoint, 1704067200000L)
        val pointHistory = PointHistory(1L, id, TransactionType.CHARGE, amount, 1704067200000L)

        every { userPointTable.selectById(id) } returns initialUserPoint
        every { userPointTable.insertOrUpdate(id, totalPoint) } returns updatedUserPoint
        every { pointHistoryTable.insert(id, amount, TransactionType.USE, 1704067200000L) } returns pointHistory

        `when`("포인트를 사용하면") {
            val result = pointService.use(id, amount)

            then("포인트가 올바르게 감소하고, 사용 내역이 기록된다.") {
                result shouldBe updatedUserPoint
                result.point shouldBe totalPoint

                verify(exactly = 1) {
                    pointHistoryTable.insert(
                        id, amount, TransactionType.USE, 1704067200000L
                    )
                }
            }
        }
    }

    given("사용하려는 포인트보다 보유한 포인트가 부족할 때") {
        val id = 1L
        val amount = 2000L
        val userPoint = UserPoint(id, 1000L, 1704067200000L)

        every { userPointTable.selectById(id) } returns userPoint

        `when`("포인트를 사용하면") {
            then("InsufficientPointException이 발생한다") {
                shouldThrow<InsufficientPointException> {
                    pointService.use(id, amount)
                }
            }
        }
    }

})
