package com.example.sayit.domain.model

data class SpendingForecast(
    val monthlyBudget: Double,
    val totalSpent: Double,
    val remainingBudget: Double,
    val dailyBurnRate: Double,
    val daysRemainingInMonth: Int,
    val projectedMonthEndTotal: Double,
    val estimatedRunwayDayOfMonth: Int?, // Day of month when budget is projected to hit zero
    val isOverBudget: Boolean,
    val warningTipAr: String,
    val warningTipEn: String
) {
    val projectedMonthTotal: Double get() = projectedMonthEndTotal
    val depletionDateAr: String
        get() = if (estimatedRunwayDayOfMonth != null) "$estimatedRunwayDayOfMonth من الشهر" else "نهاية الشهر"
    val depletionDateEn: String
        get() = if (estimatedRunwayDayOfMonth != null) "Day $estimatedRunwayDayOfMonth" else "End of Month"
}
