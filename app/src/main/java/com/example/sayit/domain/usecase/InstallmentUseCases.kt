package com.example.sayit.domain.usecase

import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.InstallmentPayoffForecast
import com.example.sayit.domain.model.MonthlyInstallmentRecord
import com.example.sayit.domain.model.ParsedInstallmentMessage
import com.example.sayit.domain.repository.InstallmentRepository
import kotlinx.coroutines.flow.Flow

class GetInstallmentsUseCase(private val repository: InstallmentRepository) {
    operator fun invoke(): Flow<List<Installment>> = repository.observeInstallments()
}

class GetInstallmentForecastUseCase(private val repository: InstallmentRepository) {
    operator fun invoke(): Flow<InstallmentPayoffForecast> = repository.observeForecast()
}

class SaveInstallmentUseCase(private val repository: InstallmentRepository) {
    suspend operator fun invoke(installment: Installment) {
        repository.saveInstallment(installment)
    }
}

class RecordInstallmentPaymentUseCase(private val repository: InstallmentRepository) {
    suspend operator fun invoke(
        installmentId: String,
        monthYear: String,
        paidAmount: Double,
        paidAt: Long = System.currentTimeMillis()
    ): MonthlyInstallmentRecord? {
        return repository.recordPayment(installmentId, monthYear, paidAmount, paidAt)
    }
}

class DeleteInstallmentUseCase(private val repository: InstallmentRepository) {
    suspend operator fun invoke(id: String) {
        repository.deleteInstallment(id)
    }
}

class ProcessInstallmentSmsUseCase(private val repository: InstallmentRepository) {
    suspend operator fun invoke(parsed: ParsedInstallmentMessage, timestamp: Long = System.currentTimeMillis()): Boolean {
        return if (parsed.isPaymentConfirmation) {
            repository.recordSmsPayment(parsed.provider, parsed.amount, timestamp)
        } else if (parsed.isDueNotice) {
            repository.recordDueNotice(
                providerOrName = parsed.provider,
                dueAmount = parsed.amount,
                monthYear = parsed.monthYearKey,
                dueTimestamp = parsed.dueTimestamp ?: timestamp
            )
        } else {
            false
        }
    }
}
