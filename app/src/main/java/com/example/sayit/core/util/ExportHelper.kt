package com.example.sayit.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.domain.model.SpendingForecast
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val arLocale = Locale.forLanguageTag("ar")
    private val enLocale = Locale.ENGLISH
    private val dateTimeFormatAr = SimpleDateFormat("yyyy-MM-dd HH:mm", arLocale)
    private val dateTimeFormatEn = SimpleDateFormat("yyyy-MM-dd HH:mm", enLocale)
    private val monthFormatAr = SimpleDateFormat("MMMM yyyy", arLocale)
    private val monthFormatEn = SimpleDateFormat("MMMM yyyy", enLocale)

    fun generateCsvContent(
        transactions: List<Transaction>,
        language: AppLanguage = AppLanguage.AR
    ): String {
        val sb = java.lang.StringBuilder()
        // Write UTF-8 BOM so Excel opens Arabic correctly
        sb.append("\uFEFF")

        val isAr = language == AppLanguage.AR
        val dateTimeFormat = if (isAr) dateTimeFormatAr else dateTimeFormatEn

        // CSV Header
        if (isAr) {
            sb.appendLine("المعرف,التاريخ والوقت,المتجر أو الجهة,المبلغ,العملة,النوع,الفئة,طريقة الدفع,المصدر,الملاحظات")
        } else {
            sb.appendLine("ID,Date & Time,Merchant,Amount,Currency,Type,Category,Payment Method,Source,Notes")
        }

        // Rows
        for (tx in transactions) {
            val dateStr = dateTimeFormat.format(Date(tx.timestamp))
            val typeStr = if (isAr) {
                if (tx.type == TransactionType.EXPENSE) "مصروف" else "دخل"
            } else {
                if (tx.type == TransactionType.EXPENSE) "Expense" else "Income"
            }
            val catStr = escapeCsv(if (isAr) (tx.category?.nameAr ?: "عام") else (tx.category?.nameEn ?: "General"))
            val merchantStr = escapeCsv(tx.merchant)
            val paymentSourceStr = if (isAr) tx.paymentSource.titleAr else tx.paymentSource.titleEn
            val sourceStr = if (isAr) tx.source.labelAr else tx.source.labelEn
            val notesStr = escapeCsv(tx.notes ?: "")

            sb.appendLine("\"${tx.id}\",\"$dateStr\",\"$merchantStr\",${tx.amount},\"${tx.currency}\",\"$typeStr\",\"$catStr\",\"$paymentSourceStr\",\"$sourceStr\",\"$notesStr\"")
        }

        return sb.toString()
    }

    fun generateCsv(
        context: Context,
        transactions: List<Transaction>,
        language: AppLanguage = AppLanguage.AR
    ): File {
        val file = File(context.cacheDir, "sayit_transactions_${System.currentTimeMillis()}.csv")
        file.writeText(generateCsvContent(transactions, language), Charsets.UTF_8)
        return file
    }

    fun shareCsvFile(
        context: Context,
        file: File,
        language: AppLanguage = AppLanguage.AR
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val isAr = language == AppLanguage.AR
        val subject = if (isAr) "سجل معاملات Say It" else "Say It Transaction History"
        val chooserTitle = if (isAr) "تصدير المعاملات (CSV)" else "Export Transactions (CSV)"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Guard against no app capable of handling CSV sharing or missing Activity context
        }
    }

    fun generateShareableReport(
        transactions: List<Transaction>,
        forecast: SpendingForecast?,
        periodTitle: String = "الشهر الحالي",
        language: AppLanguage = AppLanguage.AR
    ): String {
        val isAr = language == AppLanguage.AR
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val income = transactions.filter { it.type == TransactionType.INCOME }

        val totalExpense = expenses.sumOf { it.amount }
        val totalIncome = income.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0

        val currentMonthName = if (isAr) monthFormatAr.format(Date()) else monthFormatEn.format(Date())

        val topMerchants = expenses
            .groupBy { it.merchant }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        val sb = StringBuilder()
        val curr = if (isAr) "ج.م" else "EGP"

        if (isAr) {
            sb.appendLine("[تقرير المصاريف المالية - تطبيق Say It]")
            sb.appendLine("الفترة: $periodTitle ($currentMonthName)")
            sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

            if (totalIncome > 0) {
                sb.appendLine("إجمالي الدخل: ${totalIncome.toInt()} $curr")
            }
            sb.appendLine("إجمالي المصروفات: ${totalExpense.toInt()} $curr (${expenses.size} معاملة)")

            if (totalIncome > 0) {
                val sign = if (netSavings >= 0) "+" else ""
                sb.appendLine("صافي التوفير: $sign${netSavings.toInt()} $curr (نسبة الادخار: $savingsRate%)")
            }

            if (forecast != null && forecast.monthlyBudget > 0) {
                sb.appendLine("الميزانية الشهرية: ${forecast.monthlyBudget.toInt()} $curr")
                sb.appendLine("المتبقي من الميزانية: ${forecast.remainingBudget.toInt()} $curr")
                sb.appendLine("معدل الحرق اليومي: ${forecast.dailyBurnRate.toInt()} $curr/يوم")
            }

            if (topMerchants.isNotEmpty()) {
                sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
                sb.appendLine("أعلى الأماكن إنفاقاً:")
                topMerchants.forEachIndexed { i, (merchant, amount) ->
                    sb.appendLine("${i + 1}. $merchant: ${amount.toInt()} $curr")
                }
            }

            sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
            sb.appendLine("Say It: تتبع المصاريف محلياً 100% بدون إنترنت")
        } else {
            sb.appendLine("[Financial Summary Report - Say It]")
            sb.appendLine("Period: $periodTitle ($currentMonthName)")
            sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

            if (totalIncome > 0) {
                sb.appendLine("Total Income: ${totalIncome.toInt()} $curr")
            }
            sb.appendLine("Total Expenses: ${totalExpense.toInt()} $curr (${expenses.size} transactions)")

            if (totalIncome > 0) {
                val sign = if (netSavings >= 0) "+" else ""
                sb.appendLine("Net Savings: $sign${netSavings.toInt()} $curr (Savings rate: $savingsRate%)")
            }

            if (forecast != null && forecast.monthlyBudget > 0) {
                sb.appendLine("Monthly Budget: ${forecast.monthlyBudget.toInt()} $curr")
                sb.appendLine("Remaining Budget: ${forecast.remainingBudget.toInt()} $curr")
                sb.appendLine("Daily Burn Rate: ${forecast.dailyBurnRate.toInt()} $curr/day")
            }

            if (topMerchants.isNotEmpty()) {
                sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
                sb.appendLine("Top Spending Outlets:")
                topMerchants.forEachIndexed { i, (merchant, amount) ->
                    sb.appendLine("${i + 1}. $merchant: ${amount.toInt()} $curr")
                }
            }

            sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
            sb.appendLine("Say It: 100% Offline & Private Expense Tracker")
        }

        return sb.toString()
    }

    fun shareTextReport(
        context: Context,
        reportText: String,
        language: AppLanguage = AppLanguage.AR
    ) {
        val isAr = language == AppLanguage.AR
        val subject = if (isAr) "تقرير مالي - Say It" else "Financial Report - Say It"
        val chooserTitle = if (isAr) "مشاركة التقرير المالي" else "Share Financial Report"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reportText)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Guard against ActivityNotFoundException
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "")
    }
}
