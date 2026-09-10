package com.example.sayit.domain.model

data class ColumnInfo(
    val cid: Int = 0,
    val name: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String? = null,
    val isPrimaryKey: Boolean
)

data class QueryResult(
    val sql: String,
    val columns: List<String>,
    val rows: List<List<String>>,
    val rowCount: Int,
    val executionTimeMs: Long,
    val errorMessage: String? = null
)

data class DatabaseStats(
    val dbName: String,
    val dbVersion: Int,
    val dbPath: String,
    val dbSizeBytes: Long,
    val transactionCount: Long,
    val categoryCount: Long,
    val installmentCount: Long = 0,
    val totalSpent: Double,
    val lastTransactionTime: Long?
)
