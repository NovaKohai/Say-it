package com.example.sayit.core

import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.core.util.ExportHelper
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.SpendingForecast
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportHelperTest {

    private val sampleTransactions = listOf(
        Transaction(
            id = "tx-1",
            amount = 150.0,
            currency = "EGP",
            type = TransactionType.EXPENSE,
            merchant = "Starbucks",
            categoryId = "cat_food",
            category = Category("cat_food", "مطاعم وكافيهات", "Food & Dining", "Restaurant", 0xFF10B981),
            paymentSource = PaymentSource.INSTAPAY,
            timestamp = 1710000000000L,
            source = TransactionSource.VOICE,
            notes = "Morning coffee"
        ),
        Transaction(
            id = "tx-2",
            amount = 5000.0,
            currency = "EGP",
            type = TransactionType.INCOME,
            merchant = "Salary Client",
            categoryId = "cat_other",
            category = Category("cat_other", "عام", "General", "Category", 0xFF6B7280),
            paymentSource = PaymentSource.BANK_CARD,
            timestamp = 1710003600000L,
            source = TransactionSource.SMS,
            notes = "Transfer received"
        )
    )

    private val sampleForecast = SpendingForecast(
        monthlyBudget = 8000.0,
        totalSpent = 150.0,
        remainingBudget = 7850.0,
        dailyBurnRate = 15.0,
        daysRemainingInMonth = 20,
        projectedMonthEndTotal = 450.0,
        estimatedRunwayDayOfMonth = null,
        isOverBudget = false,
        warningTipAr = "معدل صرفك ممتاز ومستقر.",
        warningTipEn = "Your spending rate is healthy and well-balanced."
    )

    @Test
    fun testGenerateCsvContent_arabicHasUtf8BomAndArabicHeaders() {
        val csv = ExportHelper.generateCsvContent(sampleTransactions, AppLanguage.AR)

        // Must start with UTF-8 BOM so Excel displays Arabic correctly
        assertTrue("CSV must start with UTF-8 BOM", csv.startsWith("\uFEFF"))
        assertTrue("CSV must contain Arabic header", csv.contains("المعرف,التاريخ والوقت,المتجر أو الجهة,المبلغ"))
        assertTrue("CSV must contain transaction merchant", csv.contains("Starbucks"))
        assertTrue("CSV must contain category name in Arabic", csv.contains("مطاعم وكافيهات"))
        assertTrue("CSV must contain payment source in Arabic", csv.contains("إنستاباي"))
    }

    @Test
    fun testGenerateCsvContent_englishHasEnglishHeaders() {
        val csv = ExportHelper.generateCsvContent(sampleTransactions, AppLanguage.EN)

        assertTrue("CSV must start with UTF-8 BOM", csv.startsWith("\uFEFF"))
        assertTrue("CSV must contain English header", csv.contains("ID,Date & Time,Merchant,Amount,Currency,Type"))
        assertTrue("CSV must contain Expense and Income", csv.contains("Expense") && csv.contains("Income"))
        assertTrue("CSV must contain category name in English", csv.contains("Food & Dining"))
        assertTrue("CSV must contain payment source in English", csv.contains("InstaPay"))
    }

    @Test
    fun testGenerateShareableReport_arabicCorrectMetrics() {
        val report = ExportHelper.generateShareableReport(
            transactions = sampleTransactions,
            forecast = sampleForecast,
            periodTitle = "الشهر الحالي",
            language = AppLanguage.AR
        )

        assertTrue(report.contains("تقرير المصاريف المالية - تطبيق Say It"))
        assertTrue(report.contains("إجمالي الدخل: 5000 ج.م"))
        assertTrue(report.contains("إجمالي المصروفات: 150 ج.م"))
        assertTrue(report.contains("صافي التوفير: +4850 ج.م"))
        assertTrue(report.contains("الميزانية الشهرية: 8000 ج.م"))
        assertTrue(report.contains("Starbucks: 150 ج.م"))
    }

    @Test
    fun testGenerateShareableReport_englishCorrectMetrics() {
        val report = ExportHelper.generateShareableReport(
            transactions = sampleTransactions,
            forecast = sampleForecast,
            periodTitle = "This Month",
            language = AppLanguage.EN
        )

        assertTrue(report.contains("Financial Summary Report - Say It"))
        assertTrue(report.contains("Total Income: 5000 EGP"))
        assertTrue(report.contains("Total Expenses: 150 EGP"))
        assertTrue(report.contains("Net Savings: +4850 EGP"))
        assertTrue(report.contains("Monthly Budget: 8000 EGP"))
        assertTrue(report.contains("Starbucks: 150 EGP"))
    }
}
