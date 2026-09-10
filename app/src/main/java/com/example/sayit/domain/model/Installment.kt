package com.example.sayit.domain.model

enum class InstallmentStatus {
    ACTIVE,
    COMPLETED,
    PAUSED
}

enum class PaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID
}

enum class InstallmentProvider(val displayNameAr: String, val displayNameEn: String) {
    VALU("فاليو (ValU)", "ValU"),
    SOUHOOLA("سهولة (Souhoola)", "Souhoola"),
    SYMPL("سمبل (Sympl)", "Sympl"),
    AMAN("أمان (Aman)", "Aman"),
    FORSA("فرصة (Forsa)", "Forsa"),
    CIB("البنك التجاري الدولي (CIB)", "CIB Bank"),
    NBE("البنك الأهلي المصري (NBE)", "National Bank of Egypt"),
    BANQUE_MISR("بنك مصر (BM)", "Banque Misr"),
    QNB("بنك QNB الأهلي", "QNB Alahli"),
    ADIB("مصرف أبوظبي الإسلامي (ADIB)", "ADIB Egypt"),
    PERSONAL("شخصي / نقدي", "Personal / Cash"),
    OTHER("أخرى", "Other");

    companion object {
        fun fromString(value: String?): InstallmentProvider {
            if (value.isNullOrBlank()) return OTHER
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: when {
                    value.contains("valu", true) || value.contains("فاليو", true) -> VALU
                    value.contains("souhoola", true) || value.contains("سهولة", true) -> SOUHOOLA
                    value.contains("sympl", true) || value.contains("سمبل", true) -> SYMPL
                    value.contains("aman", true) || value.contains("أمان", true) -> AMAN
                    value.contains("forsa", true) || value.contains("فرصة", true) -> FORSA
                    value.contains("cib", true) || value.contains("التجاري الدولي", true) -> CIB
                    value.contains("ahli", true) || value.contains("الأهلي", true) || value.contains("nbe", true) -> NBE
                    value.contains("misr", true) || value.contains("مصر", true) -> BANQUE_MISR
                    value.contains("qnb", true) -> QNB
                    value.contains("adib", true) || value.contains("أبوظبي", true) -> ADIB
                    value.contains("شخصي", true) || value.contains("كاش", true) -> PERSONAL
                    else -> OTHER
                }
        }
    }
}

data class MonthlyInstallmentRecord(
    val id: String,
    val installmentId: String,
    val monthYear: String, // format "yyyy-MM" e.g. "2026-09"
    val dueAmount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: Long,
    val status: PaymentStatus = PaymentStatus.UNPAID,
    val paidAt: Long? = null,
    val linkedTransactionId: String? = null
) {
    val remainingAmount: Double
        get() = (dueAmount - paidAmount).coerceAtLeast(0.0)

    val isFullyPaid: Boolean
        get() = status == PaymentStatus.PAID || (dueAmount > 0 && paidAmount >= dueAmount)
}

data class Installment(
    val id: String,
    val name: String,
    val provider: InstallmentProvider = InstallmentProvider.OTHER,
    val totalAmount: Double,
    val monthlyAmount: Double,
    val startDate: Long,
    val endDate: Long,
    val totalMonths: Int,
    val dueDayOfMonth: Int = 1,
    val status: InstallmentStatus = InstallmentStatus.ACTIVE,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val monthlyRecords: List<MonthlyInstallmentRecord> = emptyList()
) {
    val totalPaid: Double
        get() = monthlyRecords.sumOf { it.paidAmount }

    val remainingTotalDebt: Double
        get() = (totalAmount - totalPaid).coerceAtLeast(0.0)

    val progressRatio: Float
        get() = if (totalAmount > 0) (totalPaid / totalAmount).coerceIn(0.0, 1.0).toFloat() else 0f

    val currentMonthRecord: MonthlyInstallmentRecord?
        get() {
            val curCal = java.util.Calendar.getInstance()
            val curKey = String.format(java.util.Locale.US, "%04d-%02d", curCal.get(java.util.Calendar.YEAR), curCal.get(java.util.Calendar.MONTH) + 1)
            return monthlyRecords.firstOrNull { it.monthYear == curKey }
        }
}

data class MonthForecastPoint(
    val monthLabel: String,
    val monthLabelEn: String = "",
    val monthYearKey: String,
    val remainingDebt: Double,
    val monthlyPaymentDue: Double
)

data class InstallmentPayoffForecast(
    val totalOriginalDebt: Double,
    val totalRemainingDebt: Double,
    val totalPaidSoFar: Double,
    val currentMonthDues: Double,
    val currentMonthPaid: Double,
    val currentMonthRemaining: Double,
    val isCurrentMonthFullySettled: Boolean,
    val activeInstallmentsCount: Int,
    val estimatedPayoffDate: Long?,
    val monthlyProjection: List<MonthForecastPoint>
)
