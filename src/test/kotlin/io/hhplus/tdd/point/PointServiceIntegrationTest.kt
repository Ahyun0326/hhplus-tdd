package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.exception.InsufficientPointException
import io.hhplus.tdd.exception.NegativePointException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedDeque

class PointServiceIntegrationTest : BehaviorSpec({
    val pointHistoryTable = PointHistoryTable()
    val userPointTable = UserPointTable()

    val pointService = PointService(pointHistoryTable, userPointTable)

    given("포인트 서비스와 동시에 실행될 100개의 100포인트 충전 요청이 주어졌을 때") {
        val id = 1L
        val numOfRequests = 100
        val amount = 100L
        val expectedTotalPoints = numOfRequests * amount

        userPointTable.clear()
        pointHistoryTable.clear()

        `when`("100개의 요청이 동시에 포인트를 충전하면") {
            // runBlocking: 테스트 스레드가 내부 코루틴 완료까지 대기
            runBlocking {
                withContext(Dispatchers.Default) {
                    val jobs = List(numOfRequests) {
                        launch {
                            pointService.charge(id, amount)
                        }
                    }
                    // 100개의 모든 작업이 완료될 때까지 대기
                    jobs.joinAll()
                }
            }

            then("모든 요청이 순차적으로 처리되어 최종 포인트가 정확히 계산된다") {
                runBlocking {
                    val userPoint = pointService.getPointById(id)
                    userPoint.point shouldBe expectedTotalPoints
                }
            }
        }
    }

    given("포인트 서비스와 동시에 실행될 100개의 -100포인트 충전 요청이 주어졌을 때") {
        val id = 1L
        val numOfRequests = 100
        val amount = -100L
        val exceptions = ConcurrentLinkedDeque<Throwable>()  // 동시성 환경에서 예외를 안전하게 수집하기 위한 큐

        userPointTable.clear()
        pointHistoryTable.clear()

        `when`("100개의 요청이 동시에 포인트를 충전하면") {
            runBlocking {
                withContext(Dispatchers.Default) {
                    val jobs = List(numOfRequests) {
                        launch {
                            try {
                                pointService.charge(id, amount)
                            } catch (e: NegativePointException) {
                                exceptions.add(e)
                            } catch (e: Exception) {
                                exceptions.add(e)
                            }
                        }
                    }
                    jobs.joinAll()
                }
            }

            then("모든 요청이 NegativePointException 예외를 발생시키고 최종 포인트는 그대로여야 한다") {
                runBlocking {
                    val userPoint = pointService.getPointById(id)
                    userPoint.point shouldBe 0L
                }
            }
        }
    }

    given("포인트 서비스와 10000 포인트, 그리고 동시에 실행될 100개의 100포인트 사용 요청이 주어졌을 때") {
        val id = 1L
        val point = 10000L
        val numOfRequests = 100
        val amount = 100L

        userPointTable.clear()
        pointHistoryTable.clear()
        pointService.charge(id, point)

        val expectedTotalPoints = point - (numOfRequests * amount)

        `when`("100개의 요청이 동시에 포인트를 사용하면") {
            runBlocking {
                withContext(Dispatchers.Default) {
                    val jobs = List(numOfRequests) {
                        launch {
                            pointService.use(id, amount)
                        }
                    }
                    jobs.joinAll()
                }
            }

            then("모든 요청이 처리되고 최종 포인트는 0이 된다") {
                runBlocking {
                    val userPoint = pointService.getPointById(id)
                    userPoint.point shouldBe expectedTotalPoints
                }
            }
        }
    }

    given("포인트 서비스와 5000 포인트, 그리고 동시에 실행될 100개의 100포인트 사용 요청이 주어졌을 때") {
        val id = 1L
        val point = 5000L
        val numOfRequests = 100
        val amount = 100L
        val exceptions = ConcurrentLinkedDeque<Throwable>()

        userPointTable.clear()
        pointHistoryTable.clear()
        pointService.charge(id, point)

        `when`("100개의 요청이 동시에 포인트를 사용하면") {
            runBlocking {
                withContext(Dispatchers.Default) {
                    val jobs = List(numOfRequests) {
                        launch {
                            try {
                                pointService.use(id, amount)
                            } catch (e: InsufficientPointException) {
                                exceptions.add(e)
                            } catch (e: Exception) {
                                exceptions.add(e)
                            }
                        }
                    }
                    jobs.joinAll()
                }
            }

            then("일부 요청은 InsufficientPointException 예외를 발생시키고 최종 포인트는 0이 된다") {
                exceptions.any { it is InsufficientPointException } shouldBe true   // 최소 1번 이상 InsufficientPointException이 발생했는지 확인

                runBlocking {
                    val userPoint = pointService.getPointById(id)
                    userPoint.point shouldBe 0L
                }
            }
        }
    }

    given("포인트 서비스와 5000 포인트, 그리고 동시에 실행될 100개의 -100포인트 사용 요청이 주어졌을 때") {
        val id = 1L
        val point = 5000L
        val numOfRequests = 100
        val amount = -100L
        val exceptions = ConcurrentLinkedDeque<Throwable>() // 동시성 환경에서 예외를 안전하게 수집하기 위한 큐

        userPointTable.clear()
        pointHistoryTable.clear()
        pointService.charge(id, point)

        `when`("100개의 요청이 동시에 포인트를 사용하면") {
            runBlocking {
                withContext(Dispatchers.Default) {
                    val jobs = List(numOfRequests) {
                        launch {
                            try {
                                pointService.use(id, amount)
                            } catch (e: NegativePointException) {
                                exceptions.add(e)
                            } catch (e: Exception) {
                                exceptions.add(e)
                            }
                        }
                    }
                    jobs.joinAll()
                }
            }

            then("모든 요청이 NegativePointException 예외를 발생시키고 최종 포인트는 그대로여야 한다.") {
                exceptions.all { it is NegativePointException } shouldBe true   // 모든 예외가 NegativePointException인지 확인

                runBlocking {
                    val userPoint = pointService.getPointById(id)
                    userPoint.point shouldBe point
                }
            }
        }
    }
})

