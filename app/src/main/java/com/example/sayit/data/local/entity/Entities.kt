package com.example.sayit.data.local.entity

data class TransactionEntity(
    val id: String,
    val amount: Double,
    val currency: String,
    val type: String,
    val categoryId: String,
    val merchant: String,
    val paymentSource: String,
    val timestamp: Long,
    val rawText: String?,
    val source: String,
    val notes: String?
)

data class CategoryEntity(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val iconName: String,
    val colorHex: Long,
    val monthlyBudget: Double?
)

data class InstallmentEntity(
    val id: String,
    val name: String,
    val provider: String,
    val totalAmount: Double,
    val monthlyAmount: Double,
    val startDate: Long,
    val endDate: Long,
    val totalMonths: Int,
    val dueDayOfMonth: Int,
    val status: String,
    val notes: String?,
    val createdAt: Long
)

data class InstallmentRecordEntity(
    val id: String,
    val installmentId: String,
    val monthYear: String,
    val dueAmount: Double,
    val paidAmount: Double,
    val dueDate: Long,
    val status: String,
    val paidAt: Long?,
    val linkedTransactionId: String?
)
