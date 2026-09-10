package com.example.sayit.data.repository

import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.domain.model.ColumnInfo
import com.example.sayit.domain.model.DatabaseStats
import com.example.sayit.domain.model.QueryResult
import com.example.sayit.domain.repository.DatabaseInspectorRepository

class DatabaseInspectorRepositoryImpl(
    private val database: SayItDatabase
) : DatabaseInspectorRepository {

    override suspend fun getStats(): DatabaseStats = database.getStats()

    override suspend fun getTableColumns(tableName: String): List<ColumnInfo> =
        database.getTableColumns(tableName)

    override suspend fun executeRawQuery(sql: String): QueryResult =
        database.executeRawQuery(sql)

    override suspend fun clearAndReseed() =
        database.clearAndReseed()

    override suspend fun clearTransactions() {
        database.executeRawQuery("DELETE FROM transactions")
    }
}
