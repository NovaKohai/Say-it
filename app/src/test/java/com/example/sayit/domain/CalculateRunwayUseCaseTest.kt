package com.example.sayit.domain

import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.domain.usecase.CalculateRunwayUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalculateRunwayUseCaseTest {

    private val calculateRunwayUseCase = CalculateRunwayUseCase()

    @Test
    fun testHealthyRunway() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 12, 0, 0)
        }
        val currentTime = calendar.timeInMillis

        val transactions = listOf(
            Transaction(
                id = "1",
                amount = 1000.0,
                type = TransactionType.EXPENSE,
                categoryId = "cat_food",
                merchant = "Food",
                timestamp = currentTime - 86400000L
            )
        )

        val forecast = calculateRunwayUseCase(
            monthlyBudget = 10000.0,
            transactions = transactions,
            currentTimeMillis = currentTime
        )

        assertEquals(1000.0, forecast.totalSpent, 0.001)
        assertEquals(9000.0, forecast.remainingBudget, 0.001)
        assertFalse(forecast.isOverBudget)
        assertTrue(forecast.warningTipAr.contains("ممتاز ومستقر"))
        assertTrue(forecast.warningTipEn.contains("healthy and well-balanced"))
        assertEquals("End of Month", forecast.depletionDateEn)
        assertEquals("نهاية الشهر", forecast.depletionDateAr)
    }

    @Test
    fun testOverBudgetWarning() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
        }
        val currentTime = calendar.timeInMillis

        val transactions = listOf(
            Transaction(
                id = "1",
                amount = 12500.0,
                type = TransactionType.EXPENSE,
                categoryId = "cat_shopping",
                merchant = "Shopping",
                timestamp = currentTime
            )
        )

        val forecast = calculateRunwayUseCase(
            monthlyBudget = 10000.0,
            transactions = transactions,
            currentTimeMillis = currentTime
        )

        assertTrue(forecast.isOverBudget)
        assertTrue(forecast.warningTipAr.contains("لقد تجاوزت ميزانيتك"))
        assertTrue(forecast.warningTipEn.contains("exceeded your monthly budget"))
    }

    @Test
    fun testRunwayBurnRateCalculation() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 12, 0, 0)
        }
        val currentTime = calendar.timeInMillis

        // 10 days passed, 5000 spent out of 6000 budget => burn rate = 500/day
        // remaining = 1000 => days left = 1000 / 500 = 2 days => will run out on day 12!
        val transactions = listOf(
            Transaction(
                id = "1",
                amount = 5000.0,
                type = TransactionType.EXPENSE,
                categoryId = "cat_bills",
                merchant = "Bills",
                timestamp = currentTime
            )
        )

        val forecast = calculateRunwayUseCase(
            monthlyBudget = 6000.0,
            transactions = transactions,
            currentTimeMillis = currentTime
        )

        assertNotNull(forecast.estimatedRunwayDayOfMonth)
        assertEquals(12, forecast.estimatedRunwayDayOfMonth)
        assertTrue(forecast.warningTipAr.contains("يوم 12"))
        assertTrue(forecast.warningTipEn.contains("day 12"))
        assertEquals("Day 12", forecast.depletionDateEn)
        assertEquals("12 من الشهر", forecast.depletionDateAr)
    }
}
