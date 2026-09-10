package com.example.sayit.domain.model

data class ParsedBankTransaction(
    val bankName: String,
    val amount: Double,
    val currency: String = "EGP",
    val merchant: String?,
    val type: TransactionType = TransactionType.EXPENSE,
    val paymentSource: PaymentSource = PaymentSource.BANK_CARD,
    val cardLast4: String? = null,
    val remainingBalance: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rawMessage: String
)

data class ParsedVoiceTransaction(
    val amount: Double,
    val currency: String = "EGP",
    val merchant: String,
    val categoryId: String,
    val paymentSource: PaymentSource = PaymentSource.CASH,
    val type: TransactionType = TransactionType.EXPENSE,
    val rawSpeech: String
)
