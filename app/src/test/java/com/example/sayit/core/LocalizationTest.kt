package com.example.sayit.core

import androidx.compose.ui.unit.LayoutDirection
import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.core.localization.ArabicStrings
import com.example.sayit.core.localization.EnglishStrings
import com.example.sayit.core.util.DateUtils
import com.example.sayit.core.util.ExportHelper
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LocalizationTest {

    @Test
    fun testAppLanguageDirections() {
        assertEquals(LayoutDirection.Rtl, AppLanguage.AR.layoutDirection)
        assertEquals(LayoutDirection.Ltr, AppLanguage.EN.layoutDirection)
        assertEquals("ar", AppLanguage.AR.code)
        assertEquals("en", AppLanguage.EN.code)
    }

    @Test
    fun testStringsDictionariesIntegrity() {
        // Verify key string properties exist and are non-empty
        assertNotNull(EnglishStrings.appName)
        assertNotNull(ArabicStrings.appName)

        assertEquals("EGP", EnglishStrings.currency)
        assertEquals("ج.م", ArabicStrings.currency)

        assertEquals("Say It", EnglishStrings.appName)
        assertEquals("Say It", ArabicStrings.appName)

        assertTrue(EnglishStrings.monthlyBudget.isNotBlank())
        assertTrue(ArabicStrings.monthlyBudget.isNotBlank())
    }

    @Test
    fun testExportCsvLocalization() {
        val sampleTransaction = Transaction(
            id = "tx_1",
            amount = 250.0,
            currency = "EGP",
            type = TransactionType.EXPENSE,
            categoryId = "cat_food",
            category = Category.findDefault("cat_food"),
            merchant = "Starbucks",
            paymentSource = PaymentSource.BANK_CARD,
            timestamp = 1725800000000L,
            source = TransactionSource.SMS,
            notes = "Morning coffee"
        )

        val transactions = listOf(sampleTransaction)

        // Arabic CSV Export
        val csvAr = ExportHelper.generateCsvContent(transactions, AppLanguage.AR)
        assertTrue(csvAr.startsWith("\uFEFF")) // BOM check
        assertTrue(csvAr.contains("المعرف,التاريخ والوقت,المتجر أو الجهة,المبلغ,العملة,النوع,الفئة,طريقة الدفع,المصدر,الملاحظات"))
        assertTrue(csvAr.contains("بطاقة بنكية"))
        assertTrue(csvAr.contains("مصروف"))

        // English CSV Export
        val csvEn = ExportHelper.generateCsvContent(transactions, AppLanguage.EN)
        assertTrue(csvEn.startsWith("\uFEFF")) // BOM check
        assertTrue(csvEn.contains("ID,Date & Time,Merchant,Amount,Currency,Type,Category,Payment Method,Source,Notes"))
        assertTrue(csvEn.contains("Bank Card"))
        assertTrue(csvEn.contains("Expense"))
        assertTrue(csvEn.contains("Starbucks"))
    }

    @Test
    fun testShareableReportLocalization() {
        val transactions = listOf(
            Transaction(
                id = "1",
                amount = 500.0,
                type = TransactionType.EXPENSE,
                categoryId = "cat_food",
                category = Category.findDefault("cat_food"),
                merchant = "Groceries",
                timestamp = System.currentTimeMillis()
            )
        )

        val reportAr = ExportHelper.generateShareableReport(
            transactions = transactions,
            forecast = null,
            language = AppLanguage.AR
        )
        assertTrue(reportAr.contains("تقرير المصاريف المالية"))
        assertTrue(reportAr.contains("إجمالي المصروفات"))
        assertTrue(reportAr.contains("ج.م"))

        val reportEn = ExportHelper.generateShareableReport(
            transactions = transactions,
            forecast = null,
            language = AppLanguage.EN
        )
        assertTrue(reportEn.contains("Financial Summary Report"))
        assertTrue(reportEn.contains("Total Expenses"))
        assertTrue(reportEn.contains("EGP"))
    }

    @Test
    fun testDateGroupingLocalization() {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterday = calendar.timeInMillis

        val txToday = Transaction(id = "1", amount = 100.0, categoryId = "cat_food", merchant = "Today", timestamp = now)
        val txYesterday = Transaction(id = "2", amount = 50.0, categoryId = "cat_food", merchant = "Yesterday", timestamp = yesterday)

        val groupedAr = DateUtils.groupByDate(listOf(txToday, txYesterday), AppLanguage.AR, currentTimeMillis = now)
        val titlesAr = groupedAr.map { it.title }
        assertTrue(titlesAr.contains("اليوم"))
        assertTrue(titlesAr.contains("أمس"))

        val groupedEn = DateUtils.groupByDate(listOf(txToday, txYesterday), AppLanguage.EN, currentTimeMillis = now)
        val titlesEn = groupedEn.map { it.title }
        assertTrue(titlesEn.contains("Today"))
        assertTrue(titlesEn.contains("Yesterday"))
    }
}
