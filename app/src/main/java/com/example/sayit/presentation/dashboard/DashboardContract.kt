package com.example.sayit.presentation.dashboard

import com.example.sayit.data.local.ColumnInfo
import com.example.sayit.data.local.DatabaseStats
import com.example.sayit.data.local.QueryResult
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.SpendingForecast
import com.example.sayit.domain.model.Transaction

import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.core.util.DateGroup
import com.example.sayit.core.util.TimePeriod
import com.example.sayit.data.sms.SmsScanResult

data class DashboardUiState(
    val language: AppLanguage = AppLanguage.AR,
    val isDarkMode: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val groupedTransactions: List<DateGroup> = emptyList(),
    val monthlyBudget: Double = 0.0,
    val forecast: SpendingForecast? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isVoiceDialogOpen: Boolean = false,
    val isEditBudgetDialogOpen: Boolean = false,
    val isManualAddDialogOpen: Boolean = false,
    val isEditTransactionDialogOpen: Boolean = false,
    val transactionToEdit: Transaction? = null,
    val selectedTransaction: Transaction? = null,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val timePeriod: TimePeriod = TimePeriod.THIS_MONTH,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val periodNetSavings: Double = 0.0,
    // Developer Mode State
    val isDeveloperUnlocked: Boolean = false,
    val isDeveloperUnlockDialogOpen: Boolean = false,
    val developerPin: String = "2026",
    // Past SMS Import State
    val isImportSmsDialogOpen: Boolean = false,
    val isScanningSms: Boolean = false,
    val smsScanResult: SmsScanResult? = null,
    // Database Explorer State
    val dbStats: DatabaseStats? = null,
    val sqlQueryResult: QueryResult? = null,
    val isExecutingSql: Boolean = false,
    val activeSqlTable: String = "transactions",
    val tableColumns: List<ColumnInfo> = emptyList()
)

sealed interface DashboardEffect {
    data class ShowSnackbar(val message: String) : DashboardEffect
}
