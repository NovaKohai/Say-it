package com.example.sayit.domain.repository

import com.example.sayit.domain.model.ColumnInfo
import com.example.sayit.domain.model.DatabaseStats
import com.example.sayit.domain.model.QueryResult

interface DatabaseInspectorRepository {
    suspend fun getStats(): DatabaseStats
    suspend fun getTableColumns(tableName: String): List<ColumnInfo>
    suspend fun executeRawQuery(sql: String): QueryResult
    suspend fun clearAndReseed()
    suspend fun clearTransactions()
}
