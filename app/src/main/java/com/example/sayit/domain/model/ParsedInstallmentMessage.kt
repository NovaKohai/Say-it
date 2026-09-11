package com.example.sayit.domain.model

data class ParsedInstallmentMessage(
    val isDueNotice: Boolean,
    val isPaymentConfirmation: Boolean,
    val provider: String,
    val amount: Double,
    val dueTimestamp: Long? = null,
    val monthYearKey: String? = null,
    val rawMessage: String
)
