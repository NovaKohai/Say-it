package com.example.sayit.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.sayit.data.local.entity.CategoryEntity
import com.example.sayit.data.local.entity.InstallmentEntity
import com.example.sayit.data.local.entity.InstallmentRecordEntity
import com.example.sayit.data.local.entity.TransactionEntity
import com.example.sayit.domain.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

typealias ColumnInfo = com.example.sayit.domain.model.ColumnInfo
typealias QueryResult = com.example.sayit.domain.model.QueryResult
typealias DatabaseStats = com.example.sayit.domain.model.DatabaseStats

class SayItDatabase(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "sayit.db"
        const val DATABASE_VERSION = 2

        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_TX_ID = "id"
        const val COL_TX_AMOUNT = "amount"
        const val COL_TX_CURRENCY = "currency"
        const val COL_TX_TYPE = "type"
        const val COL_TX_CATEGORY_ID = "category_id"
        const val COL_TX_MERCHANT = "merchant"
        const val COL_TX_PAYMENT_SOURCE = "payment_source"
        const val COL_TX_TIMESTAMP = "timestamp"
        const val COL_TX_RAW_TEXT = "raw_text"
        const val COL_TX_SOURCE = "source"
        const val COL_TX_NOTES = "notes"

        const val TABLE_CATEGORIES = "categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME_AR = "name_ar"
        const val COL_CAT_NAME_EN = "name_en"
        const val COL_CAT_ICON = "icon_name"
        const val COL_CAT_COLOR = "color_hex"
        const val COL_CAT_BUDGET = "monthly_budget"

        const val TABLE_INSTALLMENTS = "installments"
        const val COL_INST_ID = "id"
        const val COL_INST_NAME = "name"
        const val COL_INST_PROVIDER = "provider"
        const val COL_INST_TOTAL_AMOUNT = "total_amount"
        const val COL_INST_MONTHLY_AMOUNT = "monthly_amount"
        const val COL_INST_START_DATE = "start_date"
        const val COL_INST_END_DATE = "end_date"
        const val COL_INST_TOTAL_MONTHS = "total_months"
        const val COL_INST_DUE_DAY = "due_day_of_month"
        const val COL_INST_STATUS = "status"
        const val COL_INST_NOTES = "notes"
        const val COL_INST_CREATED_AT = "created_at"

        const val TABLE_INSTALLMENT_RECORDS = "installment_records"
        const val COL_REC_ID = "id"
        const val COL_REC_INSTALMENT_ID = "installment_id"
        const val COL_REC_MONTH_YEAR = "month_year"
        const val COL_REC_DUE_AMOUNT = "due_amount"
        const val COL_REC_PAID_AMOUNT = "paid_amount"
        const val COL_REC_DUE_DATE = "due_date"
        const val COL_REC_STATUS = "status"
        const val COL_REC_PAID_AT = "paid_at"
        const val COL_REC_LINKED_TX_ID = "linked_transaction_id"

        @Volatile
        private var INSTANCE: SayItDatabase? = null

        fun getInstance(context: Context): SayItDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SayItDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Invalidation signals for reactive Flows
    private val _txInvalidationSignal = MutableStateFlow(System.currentTimeMillis())
    val txInvalidationSignal = _txInvalidationSignal.asStateFlow()

    private val _catInvalidationSignal = MutableStateFlow(System.currentTimeMillis())
    val catInvalidationSignal = _catInvalidationSignal.asStateFlow()

    private val _instInvalidationSignal = MutableStateFlow(System.currentTimeMillis())
    val instInvalidationSignal = _instInvalidationSignal.asStateFlow()

    fun notifyTxChanged() {
        _txInvalidationSignal.value = System.currentTimeMillis()
    }

    fun notifyCatChanged() {
        _catInvalidationSignal.value = System.currentTimeMillis()
    }

    fun notifyInstChanged() {
        _instInvalidationSignal.value = System.currentTimeMillis()
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create transactions table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TRANSACTIONS (
                $COL_TX_ID TEXT PRIMARY KEY,
                $COL_TX_AMOUNT REAL NOT NULL,
                $COL_TX_CURRENCY TEXT NOT NULL,
                $COL_TX_TYPE TEXT NOT NULL,
                $COL_TX_CATEGORY_ID TEXT NOT NULL,
                $COL_TX_MERCHANT TEXT NOT NULL,
                $COL_TX_PAYMENT_SOURCE TEXT NOT NULL,
                $COL_TX_TIMESTAMP INTEGER NOT NULL,
                $COL_TX_RAW_TEXT TEXT,
                $COL_TX_SOURCE TEXT NOT NULL,
                $COL_TX_NOTES TEXT
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tx_timestamp ON $TABLE_TRANSACTIONS($COL_TX_TIMESTAMP DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tx_category ON $TABLE_TRANSACTIONS($COL_TX_CATEGORY_ID)")

        // Create categories table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                $COL_CAT_ID TEXT PRIMARY KEY,
                $COL_CAT_NAME_AR TEXT NOT NULL,
                $COL_CAT_NAME_EN TEXT NOT NULL,
                $COL_CAT_ICON TEXT NOT NULL,
                $COL_CAT_COLOR INTEGER NOT NULL,
                $COL_CAT_BUDGET REAL
            )
            """.trimIndent()
        )

        // Create installments table
        createInstallmentTables(db)

        // Prepopulate default categories
        prepopulateCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createInstallmentTables(db)
        }
        if (oldVersion < 3 && newVersion >= 3) {
            // Add migration logic here for v3 changes
            // Example: add new columns, tables, or constraints
        }
    }

    private fun createInstallmentTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_INSTALLMENTS (
                $COL_INST_ID TEXT PRIMARY KEY,
                $COL_INST_NAME TEXT NOT NULL,
                $COL_INST_PROVIDER TEXT NOT NULL,
                $COL_INST_TOTAL_AMOUNT REAL NOT NULL,
                $COL_INST_MONTHLY_AMOUNT REAL NOT NULL,
                $COL_INST_START_DATE INTEGER NOT NULL,
                $COL_INST_END_DATE INTEGER NOT NULL,
                $COL_INST_TOTAL_MONTHS INTEGER NOT NULL,
                $COL_INST_DUE_DAY INTEGER NOT NULL,
                $COL_INST_STATUS TEXT NOT NULL,
                $COL_INST_NOTES TEXT,
                $COL_INST_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_INSTALLMENT_RECORDS (
                $COL_REC_ID TEXT PRIMARY KEY,
                $COL_REC_INSTALMENT_ID TEXT NOT NULL,
                $COL_REC_MONTH_YEAR TEXT NOT NULL,
                $COL_REC_DUE_AMOUNT REAL NOT NULL,
                $COL_REC_PAID_AMOUNT REAL NOT NULL DEFAULT 0,
                $COL_REC_DUE_DATE INTEGER NOT NULL,
                $COL_REC_STATUS TEXT NOT NULL,
                $COL_REC_PAID_AT INTEGER,
                $COL_REC_LINKED_TX_ID TEXT,
                FOREIGN KEY ($COL_REC_INSTALMENT_ID) REFERENCES $TABLE_INSTALLMENTS($COL_INST_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_inst_records_inst ON $TABLE_INSTALLMENT_RECORDS($COL_REC_INSTALMENT_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_inst_records_month ON $TABLE_INSTALLMENT_RECORDS($COL_REC_MONTH_YEAR)")
    }

    private fun prepopulateCategories(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            for (cat in Category.DEFAULT_CATEGORIES) {
                val cv = ContentValues().apply {
                    put(COL_CAT_ID, cat.id)
                    put(COL_CAT_NAME_AR, cat.nameAr)
                    put(COL_CAT_NAME_EN, cat.nameEn)
                    put(COL_CAT_ICON, cat.iconName)
                    put(COL_CAT_COLOR, cat.colorHex)
                    put(COL_CAT_BUDGET, cat.monthlyBudget)
                }
                db.insertWithOnConflict(TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // DAO Methods for Transactions
    suspend fun insertTransaction(entity: TransactionEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_TX_ID, entity.id)
            put(COL_TX_AMOUNT, entity.amount)
            put(COL_TX_CURRENCY, entity.currency)
            put(COL_TX_TYPE, entity.type)
            put(COL_TX_CATEGORY_ID, entity.categoryId)
            put(COL_TX_MERCHANT, entity.merchant)
            put(COL_TX_PAYMENT_SOURCE, entity.paymentSource)
            put(COL_TX_TIMESTAMP, entity.timestamp)
            put(COL_TX_RAW_TEXT, entity.rawText)
            put(COL_TX_SOURCE, entity.source)
            put(COL_TX_NOTES, entity.notes)
        }
        db.insertWithOnConflict(TABLE_TRANSACTIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        notifyTxChanged()
    }

    suspend fun insertTransactions(entities: List<TransactionEntity>) = withContext(Dispatchers.IO) {
        if (entities.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (entity in entities) {
                val cv = ContentValues().apply {
                    put(COL_TX_ID, entity.id)
                    put(COL_TX_AMOUNT, entity.amount)
                    put(COL_TX_CURRENCY, entity.currency)
                    put(COL_TX_TYPE, entity.type)
                    put(COL_TX_CATEGORY_ID, entity.categoryId)
                    put(COL_TX_MERCHANT, entity.merchant)
                    put(COL_TX_PAYMENT_SOURCE, entity.paymentSource)
                    put(COL_TX_TIMESTAMP, entity.timestamp)
                    put(COL_TX_RAW_TEXT, entity.rawText)
                    put(COL_TX_SOURCE, entity.source)
                    put(COL_TX_NOTES, entity.notes)
                }
                db.insertWithOnConflict(TABLE_TRANSACTIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyTxChanged()
    }

    suspend fun updateTransaction(entity: TransactionEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_TX_AMOUNT, entity.amount)
            put(COL_TX_CURRENCY, entity.currency)
            put(COL_TX_TYPE, entity.type)
            put(COL_TX_CATEGORY_ID, entity.categoryId)
            put(COL_TX_MERCHANT, entity.merchant)
            put(COL_TX_PAYMENT_SOURCE, entity.paymentSource)
            put(COL_TX_TIMESTAMP, entity.timestamp)
            put(COL_TX_RAW_TEXT, entity.rawText)
            put(COL_TX_SOURCE, entity.source)
            put(COL_TX_NOTES, entity.notes)
        }
        db.update(TABLE_TRANSACTIONS, cv, "$COL_TX_ID = ?", arrayOf(entity.id))
        notifyTxChanged()
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_TRANSACTIONS, "$COL_TX_ID = ?", arrayOf(id))
        notifyTxChanged()
    }

    suspend fun getTransactionById(id: String): TransactionEntity? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TX_ID = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) mapCursorToTx(it) else null
        }
    }

    fun observeTransactions(): Flow<List<TransactionEntity>> {
        return txInvalidationSignal.map {
            withContext(Dispatchers.IO) {
                getAllTransactions()
            }
        }
    }

    fun observeTransactionsByRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return txInvalidationSignal.map {
            withContext(Dispatchers.IO) {
                getTransactionsByRange(startTime, endTime)
            }
        }
    }

    private fun getAllTransactions(): List<TransactionEntity> {
        val db = readableDatabase
        val list = mutableListOf<TransactionEntity>()
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TX_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(mapCursorToTx(it))
            }
        }
        return list
    }

    private fun getTransactionsByRange(startTime: Long, endTime: Long): List<TransactionEntity> {
        val db = readableDatabase
        val list = mutableListOf<TransactionEntity>()
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TX_TIMESTAMP BETWEEN ? AND ?",
            arrayOf(startTime.toString(), endTime.toString()),
            null,
            null,
            "$COL_TX_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(mapCursorToTx(it))
            }
        }
        return list
    }

    private fun mapCursorToTx(cursor: Cursor): TransactionEntity {
        return TransactionEntity(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_ID)),
            amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TX_AMOUNT)),
            currency = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_CURRENCY)),
            type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_TYPE)),
            categoryId = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_CATEGORY_ID)),
            merchant = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_MERCHANT)),
            paymentSource = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_PAYMENT_SOURCE)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TX_TIMESTAMP)),
            rawText = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_RAW_TEXT)),
            source = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_SOURCE)),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_TX_NOTES))
        )
    }

    // Category DAO Methods
    fun observeCategories(): Flow<List<CategoryEntity>> {
        return catInvalidationSignal.map {
            withContext(Dispatchers.IO) {
                getAllCategories()
            }
        }
    }

    private fun getAllCategories(): List<CategoryEntity> {
        val db = readableDatabase
        val list = mutableListOf<CategoryEntity>()
        val cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    CategoryEntity(
                        id = it.getString(it.getColumnIndexOrThrow(COL_CAT_ID)),
                        nameAr = it.getString(it.getColumnIndexOrThrow(COL_CAT_NAME_AR)),
                        nameEn = it.getString(it.getColumnIndexOrThrow(COL_CAT_NAME_EN)),
                        iconName = it.getString(it.getColumnIndexOrThrow(COL_CAT_ICON)),
                        colorHex = it.getLong(it.getColumnIndexOrThrow(COL_CAT_COLOR)),
                        monthlyBudget = if (it.isNull(it.getColumnIndexOrThrow(COL_CAT_BUDGET))) null else it.getDouble(it.getColumnIndexOrThrow(COL_CAT_BUDGET))
                    )
                )
            }
        }
        return list
    }

    suspend fun updateBudget(categoryId: String, budget: Double?) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            if (budget != null) put(COL_CAT_BUDGET, budget) else putNull(COL_CAT_BUDGET)
        }
        db.update(TABLE_CATEGORIES, cv, "$COL_CAT_ID = ?", arrayOf(categoryId))
        notifyCatChanged()
    }

    // ==========================================
    // Installment & Monthly Records DAO
    // ==========================================

    suspend fun insertInstallment(entity: InstallmentEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_INST_ID, entity.id)
            put(COL_INST_NAME, entity.name)
            put(COL_INST_PROVIDER, entity.provider)
            put(COL_INST_TOTAL_AMOUNT, entity.totalAmount)
            put(COL_INST_MONTHLY_AMOUNT, entity.monthlyAmount)
            put(COL_INST_START_DATE, entity.startDate)
            put(COL_INST_END_DATE, entity.endDate)
            put(COL_INST_TOTAL_MONTHS, entity.totalMonths)
            put(COL_INST_DUE_DAY, entity.dueDayOfMonth)
            put(COL_INST_STATUS, entity.status)
            put(COL_INST_NOTES, entity.notes)
            put(COL_INST_CREATED_AT, entity.createdAt)
        }
        db.insertWithOnConflict(TABLE_INSTALLMENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        notifyInstChanged()
    }

    suspend fun updateInstallment(entity: InstallmentEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_INST_NAME, entity.name)
            put(COL_INST_PROVIDER, entity.provider)
            put(COL_INST_TOTAL_AMOUNT, entity.totalAmount)
            put(COL_INST_MONTHLY_AMOUNT, entity.monthlyAmount)
            put(COL_INST_START_DATE, entity.startDate)
            put(COL_INST_END_DATE, entity.endDate)
            put(COL_INST_TOTAL_MONTHS, entity.totalMonths)
            put(COL_INST_DUE_DAY, entity.dueDayOfMonth)
            put(COL_INST_STATUS, entity.status)
            put(COL_INST_NOTES, entity.notes)
        }
        db.update(TABLE_INSTALLMENTS, cv, "$COL_INST_ID = ?", arrayOf(entity.id))
        notifyInstChanged()
    }

    suspend fun deleteInstallment(id: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_INSTALLMENT_RECORDS, "$COL_REC_INSTALMENT_ID = ?", arrayOf(id))
        db.delete(TABLE_INSTALLMENTS, "$COL_INST_ID = ?", arrayOf(id))
        notifyInstChanged()
    }

    suspend fun getInstallmentById(id: String): InstallmentEntity? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(TABLE_INSTALLMENTS, null, "$COL_INST_ID = ?", arrayOf(id), null, null, null)
        cursor.use {
            if (it.moveToFirst()) mapCursorToInstallment(it) else null
        }
    }

    fun observeInstallments(): Flow<List<InstallmentEntity>> {
        return instInvalidationSignal.map {
            withContext(Dispatchers.IO) {
                getAllInstallments()
            }
        }
    }

    fun getAllInstallments(): List<InstallmentEntity> {
        val db = readableDatabase
        val list = mutableListOf<InstallmentEntity>()
        val cursor = db.query(TABLE_INSTALLMENTS, null, null, null, null, null, "$COL_INST_CREATED_AT DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(mapCursorToInstallment(it))
            }
        }
        return list
    }

    private fun mapCursorToInstallment(c: Cursor): InstallmentEntity {
        return InstallmentEntity(
            id = c.getString(c.getColumnIndexOrThrow(COL_INST_ID)),
            name = c.getString(c.getColumnIndexOrThrow(COL_INST_NAME)),
            provider = c.getString(c.getColumnIndexOrThrow(COL_INST_PROVIDER)),
            totalAmount = c.getDouble(c.getColumnIndexOrThrow(COL_INST_TOTAL_AMOUNT)),
            monthlyAmount = c.getDouble(c.getColumnIndexOrThrow(COL_INST_MONTHLY_AMOUNT)),
            startDate = c.getLong(c.getColumnIndexOrThrow(COL_INST_START_DATE)),
            endDate = c.getLong(c.getColumnIndexOrThrow(COL_INST_END_DATE)),
            totalMonths = c.getInt(c.getColumnIndexOrThrow(COL_INST_TOTAL_MONTHS)),
            dueDayOfMonth = c.getInt(c.getColumnIndexOrThrow(COL_INST_DUE_DAY)),
            status = c.getString(c.getColumnIndexOrThrow(COL_INST_STATUS)),
            notes = c.getString(c.getColumnIndexOrThrow(COL_INST_NOTES)),
            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_INST_CREATED_AT))
        )
    }

    // Installment Record DAO Methods
    suspend fun insertRecord(entity: InstallmentRecordEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_REC_ID, entity.id)
            put(COL_REC_INSTALMENT_ID, entity.installmentId)
            put(COL_REC_MONTH_YEAR, entity.monthYear)
            put(COL_REC_DUE_AMOUNT, entity.dueAmount)
            put(COL_REC_PAID_AMOUNT, entity.paidAmount)
            put(COL_REC_DUE_DATE, entity.dueDate)
            put(COL_REC_STATUS, entity.status)
            put(COL_REC_PAID_AT, entity.paidAt)
            put(COL_REC_LINKED_TX_ID, entity.linkedTransactionId)
        }
        db.insertWithOnConflict(TABLE_INSTALLMENT_RECORDS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        notifyInstChanged()
    }

    suspend fun insertRecords(entities: List<InstallmentRecordEntity>) = withContext(Dispatchers.IO) {
        if (entities.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (entity in entities) {
                val cv = ContentValues().apply {
                    put(COL_REC_ID, entity.id)
                    put(COL_REC_INSTALMENT_ID, entity.installmentId)
                    put(COL_REC_MONTH_YEAR, entity.monthYear)
                    put(COL_REC_DUE_AMOUNT, entity.dueAmount)
                    put(COL_REC_PAID_AMOUNT, entity.paidAmount)
                    put(COL_REC_DUE_DATE, entity.dueDate)
                    put(COL_REC_STATUS, entity.status)
                    put(COL_REC_PAID_AT, entity.paidAt)
                    put(COL_REC_LINKED_TX_ID, entity.linkedTransactionId)
                }
                db.insertWithOnConflict(TABLE_INSTALLMENT_RECORDS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyInstChanged()
    }

    suspend fun updateRecord(entity: InstallmentRecordEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_REC_PAID_AMOUNT, entity.paidAmount)
            put(COL_REC_STATUS, entity.status)
            put(COL_REC_PAID_AT, entity.paidAt)
            put(COL_REC_LINKED_TX_ID, entity.linkedTransactionId)
        }
        db.update(TABLE_INSTALLMENT_RECORDS, cv, "$COL_REC_ID = ?", arrayOf(entity.id))
        notifyInstChanged()
    }

    fun observeAllRecords(): Flow<List<InstallmentRecordEntity>> {
        return instInvalidationSignal.map {
            withContext(Dispatchers.IO) {
                getAllRecords()
            }
        }
    }

    fun getAllRecords(): List<InstallmentRecordEntity> {
        val db = readableDatabase
        val list = mutableListOf<InstallmentRecordEntity>()
        val cursor = db.query(TABLE_INSTALLMENT_RECORDS, null, null, null, null, null, "$COL_REC_DUE_DATE ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(mapCursorToRecord(it))
            }
        }
        return list
    }

    suspend fun getRecordsForInstallment(installmentId: String): List<InstallmentRecordEntity> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val list = mutableListOf<InstallmentRecordEntity>()
        val cursor = db.query(
            TABLE_INSTALLMENT_RECORDS,
            null,
            "$COL_REC_INSTALMENT_ID = ?",
            arrayOf(installmentId),
            null,
            null,
            "$COL_REC_DUE_DATE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(mapCursorToRecord(it))
            }
        }
        return@withContext list
    }

    suspend fun getRecordByMonth(installmentId: String, monthYear: String): InstallmentRecordEntity? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_INSTALLMENT_RECORDS,
            null,
            "$COL_REC_INSTALMENT_ID = ? AND $COL_REC_MONTH_YEAR = ?",
            arrayOf(installmentId, monthYear),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) mapCursorToRecord(it) else null
        }
    }

    private fun mapCursorToRecord(c: Cursor): InstallmentRecordEntity {
        return InstallmentRecordEntity(
            id = c.getString(c.getColumnIndexOrThrow(COL_REC_ID)),
            installmentId = c.getString(c.getColumnIndexOrThrow(COL_REC_INSTALMENT_ID)),
            monthYear = c.getString(c.getColumnIndexOrThrow(COL_REC_MONTH_YEAR)),
            dueAmount = c.getDouble(c.getColumnIndexOrThrow(COL_REC_DUE_AMOUNT)),
            paidAmount = c.getDouble(c.getColumnIndexOrThrow(COL_REC_PAID_AMOUNT)),
            dueDate = c.getLong(c.getColumnIndexOrThrow(COL_REC_DUE_DATE)),
            status = c.getString(c.getColumnIndexOrThrow(COL_REC_STATUS)),
            paidAt = if (c.isNull(c.getColumnIndexOrThrow(COL_REC_PAID_AT))) null else c.getLong(c.getColumnIndexOrThrow(COL_REC_PAID_AT)),
            linkedTransactionId = if (c.isNull(c.getColumnIndexOrThrow(COL_REC_LINKED_TX_ID))) null else c.getString(c.getColumnIndexOrThrow(COL_REC_LINKED_TX_ID))
        )
    }

    // Database Inspection and Management for the User
    suspend fun getStats(): DatabaseStats = withContext(Dispatchers.IO) {
        val db = readableDatabase
        var txCount = 0L
        var totalSpent = 0.0
        var lastTxTime: Long? = null

        val cursor = db.rawQuery("SELECT COUNT(*), SUM(CASE WHEN $COL_TX_TYPE = 'EXPENSE' THEN $COL_TX_AMOUNT ELSE 0 END), MAX($COL_TX_TIMESTAMP) FROM $TABLE_TRANSACTIONS", null)
        cursor.use {
            if (it.moveToFirst()) {
                txCount = it.getLong(0)
                totalSpent = it.getDouble(1)
                lastTxTime = if (it.isNull(2)) null else it.getLong(2)
            }
        }

        var catCount = 0L
        val catCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CATEGORIES", null)
        catCursor.use {
            if (it.moveToFirst()) {
                catCount = it.getLong(0)
            }
        }

        var instCount = 0L
        val instCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_INSTALLMENTS", null)
        instCursor.use {
            if (it.moveToFirst()) {
                instCount = it.getLong(0)
            }
        }

        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val fileSizeBytes = if (dbFile != null && dbFile.exists()) dbFile.length() else 0L

        DatabaseStats(
            dbName = DATABASE_NAME,
            dbVersion = DATABASE_VERSION,
            dbPath = dbFile?.absolutePath ?: "sayit.db",
            dbSizeBytes = fileSizeBytes,
            transactionCount = txCount,
            categoryCount = catCount,
            installmentCount = instCount,
            totalSpent = totalSpent,
            lastTransactionTime = lastTxTime
        )
    }

    suspend fun getTableColumns(tableName: String): List<ColumnInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ColumnInfo>()
        val db = readableDatabase
        val safeName = when (tableName) {
            TABLE_CATEGORIES -> TABLE_CATEGORIES
            TABLE_INSTALLMENTS -> TABLE_INSTALLMENTS
            TABLE_INSTALLMENT_RECORDS -> TABLE_INSTALLMENT_RECORDS
            else -> TABLE_TRANSACTIONS
        }
        val cursor = db.rawQuery("PRAGMA table_info($safeName)", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ColumnInfo(
                        cid = it.getInt(0),
                        name = it.getString(1),
                        type = it.getString(2),
                        notNull = it.getInt(3) == 1,
                        defaultValue = it.getString(4),
                        isPrimaryKey = it.getInt(5) == 1
                    )
                )
            }
        }
        list
    }

    suspend fun executeRawQuery(sql: String): QueryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val trimmed = sql.trim()
        if (trimmed.isBlank()) {
            return@withContext QueryResult(
                sql = sql,
                columns = emptyList(),
                rows = emptyList(),
                rowCount = 0,
                executionTimeMs = 0,
                errorMessage = "الاستعلام فارغ"
            )
        }

        try {
            val isRead = trimmed.startsWith("SELECT", ignoreCase = true) ||
                         trimmed.startsWith("PRAGMA", ignoreCase = true) ||
                         trimmed.startsWith("EXPLAIN", ignoreCase = true)

            if (isRead) {
                val db = readableDatabase
                val cursor = db.rawQuery(trimmed, null)
                cursor.use { c ->
                    val columns = c.columnNames.toList()
                    val rows = mutableListOf<List<String>>()
                    while (c.moveToNext() && rows.size < 200) {
                        val row = mutableListOf<String>()
                        for (i in 0 until c.columnCount) {
                            row.add(if (c.isNull(i)) "NULL" else c.getString(i))
                        }
                        rows.add(row)
                    }
                    QueryResult(
                        sql = trimmed,
                        columns = columns,
                        rows = rows,
                        rowCount = c.count,
                        executionTimeMs = System.currentTimeMillis() - start
                    )
                }
            } else {
                val db = writableDatabase
                db.execSQL(trimmed)
                notifyTxChanged()
                notifyCatChanged()
                notifyInstChanged()
                QueryResult(
                    sql = trimmed,
                    columns = listOf("Status"),
                    rows = listOf(listOf("تم تنفيذ الأمر بنجاح")),
                    rowCount = 1,
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
        } catch (e: Exception) {
            QueryResult(
                sql = trimmed,
                columns = emptyList(),
                rows = emptyList(),
                rowCount = 0,
                executionTimeMs = System.currentTimeMillis() - start,
                errorMessage = e.message ?: "خطأ غير معروف في SQL"
            )
        }
    }

    suspend fun clearAndReseed() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_INSTALLMENT_RECORDS, null, null)
            db.delete(TABLE_INSTALLMENTS, null, null)
            db.delete(TABLE_TRANSACTIONS, null, null)
            db.delete(TABLE_CATEGORIES, null, null)
            prepopulateCategories(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyTxChanged()
        notifyCatChanged()
        notifyInstChanged()
    }
}
