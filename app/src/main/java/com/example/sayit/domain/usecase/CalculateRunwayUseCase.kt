package com.example.sayit.domain.usecase

import com.example.sayit.domain.model.SpendingForecast
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionType
import java.util.Calendar
import kotlin.math.roundToInt

class CalculateRunwayUseCase {

    operator fun invoke(
        monthlyBudget: Double,
        transactions: List<Transaction>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): SpendingForecast {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysRemaining = (daysInMonth - currentDay).coerceAtLeast(0)

        // Filter transactions for this month and only EXPENSES
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        val monthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && it.timestamp >= startOfMonth
        }

        val totalSpent = monthExpenses.sumOf { it.amount }
        val remainingBudget = (monthlyBudget - totalSpent).coerceAtLeast(0.0)
        val isOverBudget = totalSpent > monthlyBudget && monthlyBudget > 0

        val daysPassed = currentDay.coerceAtLeast(1)
        val dailyBurnRate = if (daysPassed > 0) totalSpent / daysPassed else 0.0
        val projectedMonthEndTotal = totalSpent + (dailyBurnRate * daysRemaining)

        val estimatedRunwayDay: Int? = if (monthlyBudget > 0 && dailyBurnRate > 0) {
            val daysCanSurvive = (remainingBudget / dailyBurnRate).toInt()
            val projectedDay = (daysPassed + daysCanSurvive).coerceIn(1, daysInMonth)
            if (projectedDay < daysInMonth) projectedDay else null
        } else null

        val (warningAr, warningEn) = generateSpendingTips(
            monthlyBudget = monthlyBudget,
            totalSpent = totalSpent,
            dailyBurnRate = dailyBurnRate,
            runwayDay = estimatedRunwayDay
        )

        val healthScore: Int = if (monthlyBudget <= 0) {
            75
        } else if (isOverBudget) {
            val overRatio = (totalSpent - monthlyBudget) / monthlyBudget
            (35 - (overRatio * 50).toInt()).coerceIn(5, 35)
        } else {
            val daysInMonthSafe = daysInMonth.coerceAtLeast(1)
            val expectedSpentRatio = daysPassed.toDouble() / daysInMonthSafe
            val actualSpentRatio = totalSpent / monthlyBudget
            val delta = expectedSpentRatio - actualSpentRatio
            (70 + (delta * 60).toInt()).coerceIn(35, 100)
        }

        return SpendingForecast(
            monthlyBudget = monthlyBudget,
            totalSpent = totalSpent,
            remainingBudget = remainingBudget,
            dailyBurnRate = (dailyBurnRate * 10.0).roundToInt() / 10.0,
            daysRemainingInMonth = daysRemaining,
            projectedMonthEndTotal = (projectedMonthEndTotal * 10.0).roundToInt() / 10.0,
            estimatedRunwayDayOfMonth = estimatedRunwayDay,
            isOverBudget = isOverBudget,
            warningTipAr = warningAr,
            warningTipEn = warningEn,
            healthScore = healthScore
        )
    }

    private fun generateSpendingTips(
        monthlyBudget: Double,
        totalSpent: Double,
        dailyBurnRate: Double,
        runwayDay: Int?
    ): Pair<String, String> {
        if (monthlyBudget <= 0) {
            return Pair(
                "حدد ميزانية شهرية لتفعيل التنبيهات والنصائح الذكية لمصاريفك.",
                "Set a monthly budget to activate smart insights and forecast warnings."
            )
        }

        if (totalSpent > monthlyBudget) {
            val overAmount = (totalSpent - monthlyBudget).toInt()
            return Pair(
                "لقد تجاوزت ميزانيتك الشهرية بمقدار $overAmount جنيه. يُنصح بضبط النفقات للفترة المتبقية من الشهر.",
                "You have exceeded your monthly budget by $overAmount EGP. Please consider restricting further spending."
            )
        }

        if (totalSpent == 0.0) {
            return Pair(
                "لم يتم تسجيل أي مصاريف هذا الشهر حتى الآن. ميزانيتك الشهرية كاملة ومتاحة.",
                "No expenses recorded for this month yet. Your full monthly budget is available."
            )
        }

        if (runwayDay != null) {
            return Pair(
                "بمعدل صرفك الحالي (${dailyBurnRate.toInt()} ج/يوم)، من المتوقع نفاد ميزانيتك يوم $runwayDay من الشهر.",
                "At your current burn rate (${dailyBurnRate.toInt()} EGP/day), your budget is projected to run out on day $runwayDay of the month."
            )
        }

        return Pair(
            "معدل صرفك ممتاز ومستقر حتى نهاية الشهر. المبلغ المتبقي: ${(monthlyBudget - totalSpent).toInt()} جنيه.",
            "Your spending rate is healthy and well-balanced through the month. Remaining: ${(monthlyBudget - totalSpent).toInt()} EGP."
        )
    }
}
