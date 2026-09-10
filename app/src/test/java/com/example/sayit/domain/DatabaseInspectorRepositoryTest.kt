package com.example.sayit.domain

import com.example.sayit.domain.model.ColumnInfo
import com.example.sayit.domain.model.DatabaseStats
import com.example.sayit.domain.model.QueryResult
import com.example.sayit.domain.repository.DatabaseInspectorRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseInspectorRepositoryTest {

    private class FakeDatabaseInspectorRepository : DatabaseInspectorRepository {
        var reseedCalled = false
        var clearCalled = false

        override suspend fun getStats(): DatabaseStats {
            return DatabaseStats(
                dbName = "sayit.db",
                dbVersion = 2,
                dbPath = "/data/data/com.example.sayit/databases/sayit.db",
                dbSizeBytes = 102400L,
                transactionCount = 15L,
                categoryCount = 8L,
                installmentCount = 2L,
                totalSpent = 3500.0,
                lastTransactionTime = 1700000000000L
            )
        }

        override suspend fun getTableColumns(tableName: String): List<ColumnInfo> {
            return listOf(
                ColumnInfo(cid = 0, name = "id", type = "TEXT", notNull = true, defaultValue = null, isPrimaryKey = true),
                ColumnInfo(cid = 1, name = "amount", type = "REAL", notNull = true, defaultValue = null, isPrimaryKey = false)
            )
        }

        override suspend fun executeRawQuery(sql: String): QueryResult {
            return QueryResult(
                sql = sql,
                columns = listOf("id", "amount"),
                rows = listOf(listOf("tx_1", "150.0")),
                rowCount = 1,
                executionTimeMs = 5L
            )
        }

        override suspend fun clearAndReseed() {
            reseedCalled = true
        }

        override suspend fun clearTransactions() {
            clearCalled = true
        }
    }

    @Test
    fun testInspectorRepositoryContract() = runBlocking {
        val repo = FakeDatabaseInspectorRepository()

        val stats = repo.getStats()
        assertEquals("sayit.db", stats.dbName)
        assertEquals(15L, stats.transactionCount)
        assertEquals(2L, stats.installmentCount)

        val columns = repo.getTableColumns("transactions")
        assertEquals(2, columns.size)
        assertEquals("id", columns[0].name)
        assertEquals("amount", columns[1].name)

        val queryResult = repo.executeRawQuery("SELECT * FROM transactions")
        assertEquals(1, queryResult.rowCount)
        assertEquals("tx_1", queryResult.rows[0][0])

        repo.clearAndReseed()
        assertEquals(true, repo.reseedCalled)

        repo.clearTransactions()
        assertEquals(true, repo.clearCalled)
    }
}
