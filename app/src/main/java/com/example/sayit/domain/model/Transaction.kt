package com.example.sayit.domain.model

data class Transaction(
    val id: String,
    val amount: Double,
    val currency: String = "EGP",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: String,
    val category: Category? = null,
    val merchant: String,
    val paymentSource: PaymentSource = PaymentSource.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
    val notes: String? = null
)
