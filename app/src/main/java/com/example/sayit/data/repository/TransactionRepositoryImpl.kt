package com.example.sayit.data.repository

import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.local.mapper.toDomain
import com.example.sayit.data.local.mapper.toEntity
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val database: SayItDatabase
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> {
        return combine(
            database.observeTransactions(),
            database.observeCategories()
        ) { txEntities, catEntities ->
            val catMap = catEntities.map { it.toDomain() }.associateBy { it.id }
            txEntities.map { it.toDomain(catMap[it.categoryId] ?: Category.findDefault(it.categoryId)) }
        }
    }

    override fun observeTransactionsByRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return combine(
            database.observeTransactionsByRange(startTime, endTime),
            database.observeCategories()
        ) { txEntities, catEntities ->
            val catMap = catEntities.map { it.toDomain() }.associateBy { it.id }
            txEntities.map { it.toDomain(catMap[it.categoryId] ?: Category.findDefault(it.categoryId)) }
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        val entity = database.getTransactionById(id) ?: return null
        val cat = Category.findDefault(entity.categoryId)
        return entity.toDomain(cat)
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        database.insertTransaction(transaction.toEntity())
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        database.insertTransactions(transactions.map { it.toEntity() })
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        database.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String) {
        database.deleteTransaction(id)
    }

    override fun observeTotalSpentByRange(startTime: Long, endTime: Long): Flow<Double> {
        return observeTransactionsByRange(startTime, endTime).map { list ->
            list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        }
    }
}
