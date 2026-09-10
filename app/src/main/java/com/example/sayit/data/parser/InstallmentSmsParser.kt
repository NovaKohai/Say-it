package com.example.sayit.data.parser

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedInstallmentSms(
    val isDueNotice: Boolean,
    val isPaymentConfirmation: Boolean,
    val provider: String,
    val amount: Double,
    val dueTimestamp: Long? = null,
    val monthYearKey: String? = null,
    val rawMessage: String
)

object InstallmentSmsParser {

    private val providerKeywords = listOf(
        "valu" to "VALU",
        "فاليو" to "VALU",
        "souhoola" to "SOUHOOLA",
        "سهولة" to "SOUHOOLA",
        "sympl" to "SYMPL",
        "سمبل" to "SYMPL",
        "aman" to "AMAN",
        "أمان" to "AMAN",
        "forsa" to "FORSA",
        "فرصة" to "FORSA",
        "cib" to "CIB",
        "البنك التجاري" to "CIB",
        "nbe" to "NBE",
        "الأهلي" to "NBE",
        "banque misr" to "BANQUE_MISR",
        "بنك مصر" to "BANQUE_MISR",
        "qnb" to "QNB",
        "adib" to "ADIB"
    )

    private val paymentKeywords = listOf(
        "تم سداد", "سداد قسط", "تم استلام دفعة", "دفعة مستلمة",
        "تم دفع", "تم خصم دفعة", "سداد بطاقة", "سداد لبطاقتكم",
        "payment received", "installment paid", "payment successful",
        "payment of egp", "successfully paid"
    )

    private val dueKeywords = listOf(
        "قسط", "أقساط", "اقساط", "قسطك", "المستحق", "المستحقة", "استحقاق", "مديونية",
        "المبلغ المستحق", "الحد الأدنى للسداد", "إجمالي المديونية",
        "ميعاد السداد", "تاريخ الاستحقاق", "مستحقة في", "مستحق في",
        "installment", "due", "statement due", "minimum payment",
        "total amount due", "due date"
    )

    fun isInstallmentOrDebtMessage(sender: String, message: String): Boolean {
        val lower = (sender + " " + message).lowercase(Locale.ROOT)
        val matchesProvider = providerKeywords.any { (kw, _) -> lower.contains(kw) }
        val matchesPayment = paymentKeywords.any { lower.contains(it) }
        val matchesDue = dueKeywords.any { lower.contains(it) }

        return matchesProvider && (matchesPayment || matchesDue)
    }

    fun parse(sender: String, message: String): ParsedInstallmentSms? {
        val lower = (sender + " " + message).lowercase(Locale.ROOT)
        val isPayment = paymentKeywords.any { lower.contains(it) }
        val isDue = dueKeywords.any { lower.contains(it) }

        if (!isPayment && !isDue) return null

        val provider = detectProvider(sender, message) ?: return null
        val amount = extractAmount(message) ?: return null

        val dueTimestamp = if (isDue) extractDueDate(message) else null
        val monthYearKey = extractMonthYearKey(message, dueTimestamp)

        return ParsedInstallmentSms(
            isDueNotice = isDue && !isPayment,
            isPaymentConfirmation = isPayment,
            provider = provider,
            amount = amount,
            dueTimestamp = dueTimestamp,
            monthYearKey = monthYearKey,
            rawMessage = message
        )
    }

    private fun detectProvider(sender: String, message: String): String? {
        val lower = (sender + " " + message).lowercase(Locale.ROOT)
        for ((kw, providerName) in providerKeywords) {
            if (lower.contains(kw)) {
                return providerName
            }
        }
        return null
    }

    private fun extractAmount(text: String): Double? {
        // Clean Arabic comma / thousands separator
        val normalized = text.replace(",", "").replace("،", "")

        // Prioritize numbers explicitly followed or preceded by currency symbol
        val currencyPatterns = listOf(
            Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:جم|ج\.م|egp|le)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(?:egp|le|جم|ج\.م)\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in currencyPatterns) {
            val match = pattern.find(normalized)
            if (match != null) {
                val amt = match.groupValues[1].toDoubleOrNull()
                if (amt != null && amt > 0) return amt
            }
        }

        // Secondary: keyword-anchored patterns (excluding "شهر X")
        val keywordPatterns = listOf(
            Regex("""(?:بمبلغ|بقيمة|مبلغ|دفعة|سداد)\s*(?:هي|هو|:)?\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
            Regex("""(?:قسطك|المديونية)\s*(?:هي|هو|:)?\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
            Regex("""(?:المستحق|المستحقة)\s*(?:هي|هو|:)\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
            Regex("""(?:amount|due|payment of)\s*(?:egp)?\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in keywordPatterns) {
            val match = pattern.find(normalized)
            if (match != null) {
                val amt = match.groupValues[1].toDoubleOrNull()
                if (amt != null && amt > 0) return amt
            }
        }
        return null
    }

    private fun extractDueDate(text: String): Long? {
        // Patterns like 2026/09/10, 10-09-2026, 25/09, etc.
        val datePatterns = listOf(
            Regex("""(\d{4})[/-](\d{1,2})[/-](\d{1,2})"""),
            Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{4})""")
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    val cal = Calendar.getInstance()
                    if (match.groupValues[1].length == 4) {
                        val year = match.groupValues[1].toInt()
                        val month = match.groupValues[2].toInt() - 1
                        val day = match.groupValues[3].toInt()
                        cal.set(year, month, day, 12, 0, 0)
                        return cal.timeInMillis
                    } else {
                        val day = match.groupValues[1].toInt()
                        val month = match.groupValues[2].toInt() - 1
                        val year = match.groupValues[3].toInt()
                        cal.set(year, month, day, 12, 0, 0)
                        return cal.timeInMillis
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun extractMonthYearKey(text: String, dueTimestamp: Long?): String {
        if (dueTimestamp != null && dueTimestamp > 0) {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(dueTimestamp)
        }

        val cal = Calendar.getInstance()
        // Check for named Arabic months
        val arabicMonths = listOf(
            "يناير" to 1, "فبراير" to 2, "مارس" to 3, "أبريل" to 4,
            "مايو" to 5, "يونيو" to 6, "يوليو" to 7, "أغسطس" to 8,
            "سبتمبر" to 9, "أكتوبر" to 10, "نوفمبر" to 11, "ديسمبر" to 12
        )
        for ((name, monthNum) in arabicMonths) {
            if (text.contains(name)) {
                return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), monthNum)
            }
        }

        // Default to current month
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
}
