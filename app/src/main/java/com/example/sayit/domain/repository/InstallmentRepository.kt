package com.example.sayit.domain.repository

import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.InstallmentPayoffForecast
import com.example.sayit.domain.model.MonthlyInstallmentRecord
import kotlinx.coroutines.flow.Flow

interface InstallmentRepository {
    fun observeInstallments(): Flow<List<Installment>>
    fun observeForecast(): Flow<InstallmentPayoffForecast>
    suspend fun getInstallmentById(id: String): Installment?
    suspend fun saveInstallment(installment: Installment)
    suspend fun updateInstallment(installment: Installment)
    suspend fun deleteInstallment(id: String)
    suspend fun recordPayment(
        installmentId: String,
        monthYear: String,
        paidAmount: Double,
        paidAt: Long = System.currentTimeMillis(),
        linkedTransactionId: String? = null
    ): MonthlyInstallmentRecord?
    suspend fun recordDueNotice(
        providerOrName: String,
        dueAmount: Double,
        monthYear: String? = null,
        dueTimestamp: Long? = null
    ): Boolean
    suspend fun recordSmsPayment(
        providerOrName: String,
        paidAmount: Double,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean
}
