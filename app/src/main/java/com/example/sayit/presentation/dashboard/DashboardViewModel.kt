package com.example.sayit.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.core.util.DateGroup
import com.example.sayit.core.util.DateUtils
import com.example.sayit.core.util.ExportHelper
import com.example.sayit.core.util.TimePeriod
import com.example.sayit.data.local.ColumnInfo
import com.example.sayit.data.local.DatabaseStats
import com.example.sayit.data.local.QueryResult
import com.example.sayit.domain.repository.DatabaseInspectorRepository
import com.example.sayit.data.local.SayItPreferences
import com.example.sayit.data.sms.SmsInboxReader
import com.example.sayit.data.sms.SmsScanResult
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.domain.repository.CategoryRepository
import com.example.sayit.domain.repository.TransactionRepository
import com.example.sayit.domain.usecase.CalculateRunwayUseCase
import com.example.sayit.domain.usecase.GetTransactionsUseCase
import com.example.sayit.domain.usecase.ProcessBankMessageUseCase
import com.example.sayit.domain.usecase.SaveTransactionUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val databaseInspectorRepository: DatabaseInspectorRepository,
    private val preferences: SayItPreferences? = null
) : ViewModel() {

    private val getTransactionsUseCase = GetTransactionsUseCase(transactionRepository)
    private val saveTransactionUseCase = SaveTransactionUseCase(transactionRepository)
    private val processBankMessageUseCase = ProcessBankMessageUseCase(transactionRepository)
    private val calculateRunwayUseCase = CalculateRunwayUseCase()

    private val _appLanguage = MutableStateFlow(preferences?.appLanguage ?: AppLanguage.AR)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _monthlyBudget = MutableStateFlow(preferences?.monthlyBudget ?: 0.0)
    private val _timePeriod = MutableStateFlow(
        try {
            TimePeriod.valueOf(preferences?.activeTimePeriod ?: "THIS_MONTH")
        } catch (_: Exception) {
            TimePeriod.THIS_MONTH
        }
    )

    private val _isVoiceDialogOpen = MutableStateFlow(false)
    private val _isEditBudgetDialogOpen = MutableStateFlow(false)
    private val _isManualAddDialogOpen = MutableStateFlow(false)
    private val _isEditTransactionDialogOpen = MutableStateFlow(false)
    private val _transactionToEdit = MutableStateFlow<Transaction?>(null)
    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    private val _isDarkMode = MutableStateFlow(preferences?.isDarkMode ?: false)

    // Developer Mode State (Protected by PIN)
    private val _isDeveloperUnlocked = MutableStateFlow(false)
    private val _isDeveloperUnlockDialogOpen = MutableStateFlow(false)
    private val _developerPin = MutableStateFlow("2026")

    // Past SMS Scanning State
    private val _isImportSmsDialogOpen = MutableStateFlow(false)
    private val _isScanningSms = MutableStateFlow(false)
    private val _smsScanResult = MutableStateFlow<SmsScanResult?>(null)

    // SQLite Explorer State
    private val _dbStats = MutableStateFlow<DatabaseStats?>(null)
    private val _sqlQueryResult = MutableStateFlow<QueryResult?>(null)
    private val _isExecutingSql = MutableStateFlow(false)
    private val _activeSqlTable = MutableStateFlow("transactions")
    private val _tableColumns = MutableStateFlow<List<ColumnInfo>>(emptyList())

    private val _effects = MutableSharedFlow<DashboardEffect>()
    val effects = _effects.asSharedFlow()

    init {
        loadDatabaseStats()
        loadTableColumns("transactions")
    }

    // Filter params subflow
    private val filterParamsFlow = combine(
        _searchQuery,
        _selectedCategory,
        _timePeriod,
        _appLanguage,
        _isDarkMode
    ) { query, category, period, language, isDarkMode ->
        FilterParams(query, category, period, language, isDarkMode)
    }

    // 1. Combine core financial data flows
    private val baseDataFlow = combine(
        getTransactionsUseCase(),
        categoryRepository.observeCategories(),
        _monthlyBudget,
        filterParamsFlow
    ) { txList: List<Transaction>, categories: List<Category>, budget: Double, filters ->
        val query = filters.query
        val selectedCat = filters.category
        val period = filters.period
        val language = filters.language

        // 1. Filter by time period
        val periodFiltered = DateUtils.filterByTimePeriod(txList, period)

        // 2. Filter by search query and category
        val filtered = periodFiltered.filter { tx ->
            val matchesQuery = query.isBlank() ||
                tx.merchant.contains(query, ignoreCase = true) ||
                (tx.category?.nameAr?.contains(query, ignoreCase = true) ?: false) ||
                (tx.category?.nameEn?.contains(query, ignoreCase = true) ?: false)

            val matchesCat = selectedCat == null || tx.categoryId == selectedCat
            matchesQuery && matchesCat
        }

        // 3. Group by date with selected language
        val grouped = DateUtils.groupByDate(filtered, language)

        // 4. Cashflow stats for selected period
        val periodIncome = periodFiltered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val periodExpense = periodFiltered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val periodNet = periodIncome - periodExpense

        // 5. Runway forecast (calculated on full month expense data)
        val forecast = calculateRunwayUseCase(budget, txList)

        FinancialDataBundle(
            txList = txList,
            filteredTransactions = filtered,
            groupedTransactions = grouped,
            categories = categories,
            budget = budget,
            forecast = forecast,
            query = query,
            selectedCategory = selectedCat,
            timePeriod = period,
            income = periodIncome,
            expense = periodExpense,
            netSavings = periodNet,
            language = language,
            isDarkMode = filters.isDarkMode
        )
    }

    // 2. Combine with dialog states
    private val dialogsFlow = combine(
        _isVoiceDialogOpen,
        _isEditBudgetDialogOpen,
        _isManualAddDialogOpen,
        _isEditTransactionDialogOpen,
        combine(_selectedTransaction, _transactionToEdit) { sel, edit -> Pair(sel, edit) }
    ) { voiceOpen, budgetOpen, manualAddOpen, editOpen, txPair ->
        DialogsBundle(
            isVoiceOpen = voiceOpen,
            isBudgetOpen = budgetOpen,
            isManualAddOpen = manualAddOpen,
            isEditOpen = editOpen,
            selectedTransaction = txPair.first,
            transactionToEdit = txPair.second
        )
    }

    // 3. Combine with SQL explorer states
    private val sqlExplorerFlow = combine(
        _dbStats,
        _sqlQueryResult,
        _isExecutingSql,
        _activeSqlTable,
        _tableColumns
    ) { stats, result, executing, table, columns ->
        Triple(stats, result, Triple(executing, table, columns))
    }

    // 4. Combine Developer and Past SMS scanning states
    private val devAndSmsFlow = combine(
        _isDeveloperUnlocked,
        _isDeveloperUnlockDialogOpen,
        _isImportSmsDialogOpen,
        _isScanningSms,
        _smsScanResult
    ) { devUnlocked, devDialogOpen, smsDialogOpen, scanningSms, scanResult ->
        Triple(devUnlocked, devDialogOpen, Triple(smsDialogOpen, scanningSms, scanResult))
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        baseDataFlow,
        dialogsFlow,
        sqlExplorerFlow,
        devAndSmsFlow
    ) { financial, dialogs, sqlState, devSms ->
        val stats = sqlState.first
        val queryResult = sqlState.second
        val executing = sqlState.third.first
        val table = sqlState.third.second
        val columns = sqlState.third.third

        val devUnlocked = devSms.first
        val devDialogOpen = devSms.second
        val smsDialogOpen = devSms.third.first
        val scanningSms = devSms.third.second
        val scanResult = devSms.third.third

        DashboardUiState(
            language = financial.language,
            isDarkMode = financial.isDarkMode,
            transactions = financial.txList,
            filteredTransactions = financial.filteredTransactions,
            groupedTransactions = financial.groupedTransactions,
            monthlyBudget = financial.budget,
            forecast = financial.forecast,
            categories = financial.categories,
            isLoading = false,
            isVoiceDialogOpen = dialogs.isVoiceOpen,
            isEditBudgetDialogOpen = dialogs.isBudgetOpen,
            isManualAddDialogOpen = dialogs.isManualAddOpen,
            isEditTransactionDialogOpen = dialogs.isEditOpen,
            selectedTransaction = dialogs.selectedTransaction,
            transactionToEdit = dialogs.transactionToEdit,
            searchQuery = financial.query,
            selectedCategoryId = financial.selectedCategory,
            timePeriod = financial.timePeriod,
            periodIncome = financial.income,
            periodExpense = financial.expense,
            periodNetSavings = financial.netSavings,
            isDeveloperUnlocked = devUnlocked,
            isDeveloperUnlockDialogOpen = devDialogOpen,
            developerPin = _developerPin.value,
            isImportSmsDialogOpen = smsDialogOpen,
            isScanningSms = scanningSms,
            smsScanResult = scanResult,
            dbStats = stats,
            sqlQueryResult = queryResult,
            isExecutingSql = executing,
            activeSqlTable = table,
            tableColumns = columns
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun onLanguageChanged(language: AppLanguage) {
        _appLanguage.value = language
        preferences?.appLanguage = language
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        preferences?.isDarkMode = enabled
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterSelected(categoryId: String?) {
        _selectedCategory.value = if (_selectedCategory.value == categoryId) null else categoryId
    }

    fun onTimePeriodChanged(period: TimePeriod) {
        _timePeriod.value = period
        preferences?.activeTimePeriod = period.name
    }

    fun setVoiceDialogOpen(open: Boolean) {
        _isVoiceDialogOpen.value = open
    }

    fun setEditBudgetDialogOpen(open: Boolean) {
        _isEditBudgetDialogOpen.value = open
    }

    fun setManualAddDialogOpen(open: Boolean) {
        _isManualAddDialogOpen.value = open
    }

    fun setEditTransactionDialog(transaction: Transaction?, open: Boolean) {
        _transactionToEdit.value = transaction
        _isEditTransactionDialogOpen.value = open
    }

    fun selectTransaction(transaction: Transaction?) {
        _selectedTransaction.value = transaction
    }

    fun updateMonthlyBudget(newBudget: Double) {
        if (newBudget > 0) {
            _monthlyBudget.value = newBudget
            preferences?.monthlyBudget = newBudget
            viewModelScope.launch {
                val isEn = _appLanguage.value == AppLanguage.EN
                val msg = if (isEn) "Monthly budget saved: ${newBudget.toInt()} EGP" else "تم حفظ الميزانية الشهرية: ${newBudget.toInt()} ج.م"
                _effects.emit(DashboardEffect.ShowSnackbar(msg))
            }
        }
    }

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            saveTransactionUseCase(transaction)
            loadDatabaseStats()
            val isEn = _appLanguage.value == AppLanguage.EN
            val msg = if (isEn) "Transaction saved: ${transaction.merchant} (${transaction.amount.toInt()} EGP)" else "تم حفظ المعاملة بنجاح: ${transaction.merchant} (${transaction.amount.toInt()} ج.م)"
            _effects.emit(DashboardEffect.ShowSnackbar(msg))
        }
    }

    fun quickLogExpense(amount: Double, merchant: String, categoryId: String) {
        val newTx = Transaction(
            id = java.util.UUID.randomUUID().toString(),
            amount = amount,
            merchant = merchant,
            type = TransactionType.EXPENSE,
            categoryId = categoryId,
            timestamp = System.currentTimeMillis(),
            notes = "Quick Log"
        )
        saveTransaction(newTx)
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
            loadDatabaseStats()
            val isEn = _appLanguage.value == AppLanguage.EN
            val msg = if (isEn) "Transaction updated: ${transaction.merchant}" else "تم تحديث المعاملة: ${transaction.merchant}"
            _effects.emit(DashboardEffect.ShowSnackbar(msg))
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            loadDatabaseStats()
            val isEn = _appLanguage.value == AppLanguage.EN
            val msg = if (isEn) "Transaction deleted from database" else "تم حذف المعاملة من قاعدة البيانات"
            _effects.emit(DashboardEffect.ShowSnackbar(msg))
        }
    }

    fun exportTransactionsCsv(context: Context) {
        val txs = uiState.value.filteredTransactions.ifEmpty { uiState.value.transactions }
        val lang = uiState.value.language
        if (txs.isEmpty()) {
            viewModelScope.launch {
                val msg = if (lang == AppLanguage.EN) "No transactions to export" else "لا توجد معاملات لتصديرها"
                _effects.emit(DashboardEffect.ShowSnackbar(msg))
            }
            return
        }
        try {
            val file = ExportHelper.generateCsv(context, txs, lang)
            ExportHelper.shareCsvFile(context, file, lang)
        } catch (e: Exception) {
            viewModelScope.launch {
                val prefix = if (lang == AppLanguage.EN) "Export failed: " else "تعذر تصدير الملف: "
                _effects.emit(DashboardEffect.ShowSnackbar("$prefix${e.localizedMessage}"))
            }
        }
    }

    fun shareReport(context: Context) {
        val txs = uiState.value.filteredTransactions.ifEmpty { uiState.value.transactions }
        val forecast = uiState.value.forecast
        val lang = uiState.value.language
        val periodTitle = if (lang == AppLanguage.EN) uiState.value.timePeriod.titleEn else uiState.value.timePeriod.titleAr
        try {
            val text = ExportHelper.generateShareableReport(txs, forecast, periodTitle, lang)
            ExportHelper.shareTextReport(context, text, lang)
        } catch (e: Exception) {
            viewModelScope.launch {
                val prefix = if (lang == AppLanguage.EN) "Failed to share report: " else "تعذر مشاركة التقرير: "
                _effects.emit(DashboardEffect.ShowSnackbar("$prefix${e.localizedMessage}"))
            }
        }
    }

    fun setDeveloperUnlockDialogOpen(open: Boolean) {
        _isDeveloperUnlockDialogOpen.value = open
    }

    fun unlockDeveloperMode() {
        _isDeveloperUnlocked.value = true
        _isDeveloperUnlockDialogOpen.value = false
        viewModelScope.launch {
            val isEn = _appLanguage.value == AppLanguage.EN
            val msg = if (isEn) "SQLite Explorer unlocked" else "تم فتح مستكشف SQLite بنجاح"
            _effects.emit(DashboardEffect.ShowSnackbar(msg))
        }
    }

    fun lockDeveloperMode() {
        _isDeveloperUnlocked.value = false
        viewModelScope.launch {
            val isEn = _appLanguage.value == AppLanguage.EN
            val msg = if (isEn) "Developer mode locked" else "تم قفل وضع المطور"
            _effects.emit(DashboardEffect.ShowSnackbar(msg))
        }
    }

    fun setImportSmsDialogOpen(open: Boolean) {
        _isImportSmsDialogOpen.value = open
        if (!open) {
            _smsScanResult.value = null
        }
    }

    fun scanPastSms(context: Context, limit: Int) {
        viewModelScope.launch {
            _isScanningSms.value = true
            val reader = SmsInboxReader(context)
            val currentTx = uiState.value.transactions
            val result = reader.readPastSms(limit = limit, existingTransactions = currentTx)
            _smsScanResult.value = result
            _isScanningSms.value = false
        }
    }

    fun confirmImportPastSms(transactions: List<Transaction>) {
        viewModelScope.launch {
            if (transactions.isNotEmpty()) {
                transactionRepository.insertTransactions(transactions)
                loadDatabaseStats()
                _isImportSmsDialogOpen.value = false
                _smsScanResult.value = null
                val isEn = _appLanguage.value == AppLanguage.EN
                val msg = if (isEn) "Imported ${transactions.size} transactions from SMS" else "تم استيراد ${transactions.size} معاملة من رسائل الهاتف بنجاح"
                _effects.emit(DashboardEffect.ShowSnackbar(msg))
            }
        }
    }

    // SQLite Database Methods
    fun loadDatabaseStats() {
        viewModelScope.launch {
            _dbStats.value = databaseInspectorRepository.getStats()
        }
    }

    fun loadTableColumns(tableName: String) {
        viewModelScope.launch {
            _activeSqlTable.value = tableName
            _tableColumns.value = databaseInspectorRepository.getTableColumns(tableName)
        }
    }

    fun executeSqlQuery(sql: String) {
        viewModelScope.launch {
            _isExecutingSql.value = true
            val result = databaseInspectorRepository.executeRawQuery(sql)
            _sqlQueryResult.value = result
            _isExecutingSql.value = false
            loadDatabaseStats()
            val isEn = _appLanguage.value == AppLanguage.EN
            if (result.errorMessage != null) {
                val prefix = if (isEn) "SQL Error: " else "خطأ SQL: "
                _effects.emit(DashboardEffect.ShowSnackbar("$prefix${result.errorMessage}"))
            } else {
                val msg = if (isEn) "Query executed in ${result.executionTimeMs} ms (${result.rowCount} rows)" else "تم تنفيذ الاستعلام في ${result.executionTimeMs} ms (${result.rowCount} صف)"
                _effects.emit(DashboardEffect.ShowSnackbar(msg))
            }
        }
    }

    fun reseedDatabase() {
        viewModelScope.launch {
            databaseInspectorRepository.clearAndReseed()
            loadDatabaseStats()
            loadTableColumns(_activeSqlTable.value)
            _sqlQueryResult.value = null
            val isEn = _appLanguage.value == AppLanguage.EN
            val msg = if (isEn) "SQLite database reseeded successfully" else "تمت إعادة تهيئة قاعدة بيانات SQLite بنجاح"
            _effects.emit(DashboardEffect.ShowSnackbar(msg))
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            databaseInspectorRepository.clearTransactions()
            loadDatabaseStats()
            loadTableColumns(_activeSqlTable.value)
            _sqlQueryResult.value = null
            val isEn = _appLanguage.value == AppLanguage.EN
            val msg = if (isEn) "Transactions table cleared" else "تم تفريغ جدول المعاملات"
            _effects.emit(DashboardEffect.ShowSnackbar(msg))
        }
    }
}

// Internal bundle data classes for combining StateFlows cleanly
private data class FilterParams(
    val query: String,
    val category: String?,
    val period: TimePeriod,
    val language: AppLanguage,
    val isDarkMode: Boolean
)

private data class FinancialDataBundle(
    val txList: List<Transaction>,
    val filteredTransactions: List<Transaction>,
    val groupedTransactions: List<DateGroup>,
    val categories: List<Category>,
    val budget: Double,
    val forecast: com.example.sayit.domain.model.SpendingForecast?,
    val query: String,
    val selectedCategory: String?,
    val timePeriod: TimePeriod,
    val income: Double,
    val expense: Double,
    val netSavings: Double,
    val language: AppLanguage,
    val isDarkMode: Boolean
)

private data class DialogsBundle(
    val isVoiceOpen: Boolean,
    val isBudgetOpen: Boolean,
    val isManualAddOpen: Boolean,
    val isEditOpen: Boolean,
    val selectedTransaction: Transaction?,
    val transactionToEdit: Transaction?
)
