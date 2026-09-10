package com.example.sayit.domain.usecase

import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.repository.TransactionRepository
import java.util.UUID

class SaveTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        return runCatching {
            require(transaction.amount > 0) { "المبلغ يجب أن يكون أكبر من الصفر" }
            val finalTx = if (transaction.id.isBlank()) {
                transaction.copy(id = UUID.randomUUID().toString())
            } else {
                transaction
            }
            transactionRepository.insertTransaction(finalTx)
        }
    }
}
