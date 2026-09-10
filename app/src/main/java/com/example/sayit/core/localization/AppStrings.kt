package com.example.sayit.core.localization

import androidx.compose.runtime.staticCompositionLocalOf

data class AppStrings(
    // TopBar & Header
    val appName: String,
    val localOfflineBadge: String,
    val morningGreeting: String,
    val eveningGreeting: String,
    val dashboardSubtitle: String,
    val shareReport: String,
    val exportCsv: String,
    val languageToggle: String,

    // Navigation Tabs
    val tabDashboard: String,
    val tabAnalytics: String,
    val tabInstallments: String,
    val tabAiCopilot: String = "المساعد",
    val tabSqlDb: String,
    val tabSettings: String,

    // Actions & Common
    val addManual: String,
    val voiceRecord: String,
    val edit: String,
    val delete: String,
    val close: String,
    val cancel: String,
    val save: String,
    val saveChanges: String,
    val confirm: String,
    val apply: String,
    val currency: String,
    val perDay: String,
    val all: String,

    // Budget Summary Card
    val monthlyBudget: String,
    val smartRemainingBudget: String,
    val remainingCurrency: String,
    val dailyBurnRate: String,
    val depletionDate: String,
    val spent: String,
    val budgetCap: String,
    val startSetBudget: String,
    val startSetBudgetDesc: String,
    val setBudgetNow: String,
    val totalSpentLabel: String,

    // Cashflow Row
    val income: String,
    val expenses: String,
    val netSavings: String,
    val totalInflow: String,
    val totalOutflow: String,
    val savings: String,
    val savingsRate: String,

    // Time Periods
    val periodThisMonth: String,
    val periodLastMonth: String,
    val periodLast30Days: String,
    val periodAllTime: String,

    // Transactions List & Empty State
    val searchPlaceholder: String,
    val transactionsHistory: String,
    val noTransactions: String,
    val noTransactionsDesc: String,
    val today: String,
    val yesterday: String,
    val transactionSingle: String,
    val transactionPlural: String,

    // Transaction Details & Dialogs
    val transactionDetails: String,
    val editTransaction: String,
    val addTransactionTitle: String,
    val expenseType: String,
    val incomeType: String,
    val amountLabel: String,
    val amountPlaceholder: String,
    val merchantLabel: String,
    val merchantPlaceholder: String,
    val notesLabel: String,
    val categoryLabel: String,
    val paymentMethodLabel: String,
    val sourceLabel: String,
    val dateTimeLabel: String,
    val rawTextLabel: String,

    // Analytics Screen
    val runwayTitle: String,
    val runwaySubtitle: String,
    val zeroBudgetNotice: String,
    val overBudget: String,
    val fullBudget: String,
    val budgetConsumed: String,
    val projectedMonthTotal: String,
    val spendingByCategory: String,
    val noExpensesForCategories: String,
    val topMerchants: String,
    val noMerchantsYet: String,

    // Settings & Bank Hub
    val monthlyBudgetHeading: String,
    val budgetHeadingDesc: String,
    val notSpecified: String,
    val customBudgetLabel: String,
    val customBudgetPlaceholder: String,
    val smsSyncTitle: String,
    val smsSyncDesc: String,
    val smsSyncButton: String,
    val permissionsTitle: String,
    val smsPermissionTitle: String,
    val smsPermissionDesc: String,
    val notifPermissionTitle: String,
    val notifPermissionDesc: String,
    val openNotifSettings: String,
    val devOptionsTitle: String,
    val devOptionsDesc: String,
    val devModeActive: String,
    val lockDevMode: String,
    val languageTitle: String,
    val languageDesc: String
) {
    val isArabic: Boolean
        get() = currency != "EGP"
}

val LocalStrings = staticCompositionLocalOf<AppStrings> {
    error("No AppStrings provided. Please provide via CompositionLocalProvider.")
}
