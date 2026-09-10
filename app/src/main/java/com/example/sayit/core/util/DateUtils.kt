package com.example.sayit.core.util

import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimePeriod(val titleAr: String, val titleEn: String) {
    THIS_MONTH("هذا الشهر", "This Month"),
    LAST_MONTH("الشهر السابق", "Last Month"),
    LAST_30_DAYS("آخر 30 يوم", "Last 30 Days"),
    ALL_TIME("الكل", "All Time")
}

data class DateGroup(
    val title: String,
    val dateMillis: Long,
    val totalExpense: Double,
    val totalIncome: Double,
    val transactions: List<Transaction>
)

object DateUtils {

    private val arLocale = Locale.forLanguageTag("ar")
    private val enLocale = Locale.ENGLISH
    private val dayFormatAr = SimpleDateFormat("EEEE، d MMMM yyyy", arLocale)
    private val shortDateFormatAr = SimpleDateFormat("d MMMM yyyy", arLocale)
    private val dayFormatEn = SimpleDateFormat("EEEE, MMMM d, yyyy", enLocale)
    private val shortDateFormatEn = SimpleDateFormat("MMMM d, yyyy", enLocale)

    fun filterByTimePeriod(
        transactions: List<Transaction>,
        period: TimePeriod,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<Transaction> {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }

        return when (period) {
            TimePeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfMonth = cal.timeInMillis
                transactions.filter { it.timestamp >= startOfMonth }
            }
            TimePeriod.LAST_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfThisMonth = cal.timeInMillis

                cal.add(Calendar.MONTH, -1)
                val startOfLastMonth = cal.timeInMillis

                transactions.filter { it.timestamp in startOfLastMonth until startOfThisMonth }
            }
            TimePeriod.LAST_30_DAYS -> {
                val thirtyDaysAgo = currentTimeMillis - (30L * 24 * 60 * 60 * 1000)
                transactions.filter { it.timestamp >= thirtyDaysAgo }
            }
            TimePeriod.ALL_TIME -> transactions
        }
    }

    fun groupByDate(
        transactions: List<Transaction>,
        language: com.example.sayit.core.localization.AppLanguage = com.example.sayit.core.localization.AppLanguage.AR,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<DateGroup> {
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val todayYear = nowCal.get(Calendar.YEAR)
        val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        val itemCal = Calendar.getInstance()

        // Group by calendar day (year * 1000 + dayOfYear)
        val grouped = transactions
            .sortedByDescending { it.timestamp }
            .groupBy { tx ->
                itemCal.timeInMillis = tx.timestamp
                val y = itemCal.get(Calendar.YEAR)
                val d = itemCal.get(Calendar.DAY_OF_YEAR)
                y * 1000 + d
            }

        val isEn = language == com.example.sayit.core.localization.AppLanguage.EN

        return grouped.map { (_, txList) ->
            val firstTxTime = txList.first().timestamp
            itemCal.timeInMillis = firstTxTime
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemDayOfYear = itemCal.get(Calendar.DAY_OF_YEAR)

            val title = when {
                itemYear == todayYear && itemDayOfYear == todayDayOfYear -> if (isEn) "Today" else "اليوم"
                itemYear == todayYear && itemDayOfYear == todayDayOfYear - 1 -> if (isEn) "Yesterday" else "أمس"
                itemYear == todayYear -> if (isEn) dayFormatEn.format(Date(firstTxTime)) else dayFormatAr.format(Date(firstTxTime))
                else -> if (isEn) shortDateFormatEn.format(Date(firstTxTime)) else shortDateFormatAr.format(Date(firstTxTime))
            }

            val totalExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val totalIncome = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

            DateGroup(
                title = title,
                dateMillis = firstTxTime,
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                transactions = txList
            )
        }
    }
}
