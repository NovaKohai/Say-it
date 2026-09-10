package com.example.sayit.domain.usecase

import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return transactionRepository.observeTransactions()
    }

    fun byRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionRepository.observeTransactionsByRange(startTime, endTime)
    }
}
