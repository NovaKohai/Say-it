package com.example.sayit.data.local.mapper

import com.example.sayit.data.local.entity.CategoryEntity
import com.example.sayit.data.local.entity.InstallmentEntity
import com.example.sayit.data.local.entity.InstallmentRecordEntity
import com.example.sayit.data.local.entity.TransactionEntity
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.InstallmentProvider
import com.example.sayit.domain.model.InstallmentStatus
import com.example.sayit.domain.model.MonthlyInstallmentRecord
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.PaymentStatus
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.model.TransactionType

fun TransactionEntity.toDomain(category: Category? = null): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        currency = currency,
        type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.EXPENSE },
        categoryId = categoryId,
        category = category ?: Category.findDefault(categoryId),
        merchant = merchant,
        paymentSource = try { PaymentSource.valueOf(paymentSource) } catch (_: Exception) { PaymentSource.CASH },
        timestamp = timestamp,
        rawText = rawText,
        source = try { TransactionSource.valueOf(source) } catch (_: Exception) { TransactionSource.MANUAL },
        notes = notes
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        currency = currency,
        type = type.name,
        categoryId = categoryId,
        merchant = merchant,
        paymentSource = paymentSource.name,
        timestamp = timestamp,
        rawText = rawText,
        source = source.name,
        notes = notes
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        nameAr = nameAr,
        nameEn = nameEn,
        iconName = iconName,
        colorHex = colorHex,
        monthlyBudget = monthlyBudget
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        nameAr = nameAr,
        nameEn = nameEn,
        iconName = iconName,
        colorHex = colorHex,
        monthlyBudget = monthlyBudget
    )
}

fun InstallmentEntity.toDomain(records: List<MonthlyInstallmentRecord> = emptyList()): Installment {
    return Installment(
        id = id,
        name = name,
        provider = InstallmentProvider.fromString(provider),
        totalAmount = totalAmount,
        monthlyAmount = monthlyAmount,
        startDate = startDate,
        endDate = endDate,
        totalMonths = totalMonths,
        dueDayOfMonth = dueDayOfMonth,
        status = try { InstallmentStatus.valueOf(status) } catch (_: Exception) { InstallmentStatus.ACTIVE },
        notes = notes,
        createdAt = createdAt,
        monthlyRecords = records
    )
}

fun Installment.toEntity(): InstallmentEntity {
    return InstallmentEntity(
        id = id,
        name = name,
        provider = provider.name,
        totalAmount = totalAmount,
        monthlyAmount = monthlyAmount,
        startDate = startDate,
        endDate = endDate,
        totalMonths = totalMonths,
        dueDayOfMonth = dueDayOfMonth,
        status = status.name,
        notes = notes,
        createdAt = createdAt
    )
}

fun InstallmentRecordEntity.toDomain(): MonthlyInstallmentRecord {
    return MonthlyInstallmentRecord(
        id = id,
        installmentId = installmentId,
        monthYear = monthYear,
        dueAmount = dueAmount,
        paidAmount = paidAmount,
        dueDate = dueDate,
        status = try { PaymentStatus.valueOf(status) } catch (_: Exception) { PaymentStatus.UNPAID },
        paidAt = paidAt,
        linkedTransactionId = linkedTransactionId
    )
}

fun MonthlyInstallmentRecord.toEntity(): InstallmentRecordEntity {
    return InstallmentRecordEntity(
        id = id,
        installmentId = installmentId,
        monthYear = monthYear,
        dueAmount = dueAmount,
        paidAmount = paidAmount,
        dueDate = dueDate,
        status = status.name,
        paidAt = paidAt,
        linkedTransactionId = linkedTransactionId
    )
}
