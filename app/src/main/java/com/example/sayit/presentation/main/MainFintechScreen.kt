package com.example.sayit.presentation.main

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.TextButton
import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.core.localization.AppStrings
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.core.util.TimePeriod
import com.example.sayit.presentation.analytics.AnalyticsScreen
import com.example.sayit.presentation.common.AddManualTransactionDialog
import com.example.sayit.presentation.common.AppErrorBoundary
import com.example.sayit.presentation.common.AppGuideTourDialog
import com.example.sayit.presentation.update.AppUpdateDialog
import com.example.sayit.presentation.update.AppUpdateViewModel
import com.example.sayit.data.util.InstallResult
import com.example.sayit.data.util.UpdateNotificationHelper
import com.example.sayit.presentation.dashboard.components.DashboardSkeleton
import com.example.sayit.presentation.common.BudgetSummaryCard
import com.example.sayit.presentation.common.CashflowSummaryRow
import com.example.sayit.presentation.common.DateHeaderSeparator
import com.example.sayit.presentation.common.DeveloperUnlockDialog
import com.example.sayit.presentation.common.EditBudgetDialog
import com.example.sayit.presentation.common.EditTransactionDialog
import com.example.sayit.presentation.common.EmilEasings
import com.example.sayit.presentation.common.ImportPastSmsDialog
import com.example.sayit.presentation.common.TransactionDetailDialog
import com.example.sayit.presentation.common.TransactionItemCard
import com.example.sayit.presentation.common.pressScale
import com.example.sayit.presentation.dashboard.DashboardEffect
import com.example.sayit.presentation.dashboard.DashboardUiState
import com.example.sayit.presentation.dashboard.DashboardViewModel
import com.example.sayit.presentation.database.SqlExplorerScreen
import com.example.sayit.presentation.settings.BankHubScreen
import com.example.sayit.presentation.voice.VoiceInputDialog
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import com.example.sayit.presentation.dashboard.components.RealtimeSpendingGraph
import com.example.sayit.data.ai.GeminiAiClient
import com.example.sayit.data.ai.LocalToolsExecutor
import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.local.SayItPreferences
import com.example.sayit.data.repository.AiCopilotRepositoryImpl
import com.example.sayit.domain.model.NavigationTarget
import com.example.sayit.domain.usecase.AskAiCopilotUseCase
import com.example.sayit.presentation.ai.AiCopilotScreen
import com.example.sayit.presentation.ai.AiCopilotViewModel

import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.example.sayit.presentation.installments.InstallmentsScreen
import com.example.sayit.presentation.installments.InstallmentsViewModel

sealed class NavTab(val icon: ImageVector) {
    data object Dashboard : NavTab(Icons.Default.AccountBalanceWallet)
    data object Installments : NavTab(Icons.Default.CreditCard)
    data object Analytics : NavTab(Icons.AutoMirrored.Filled.ShowChart)
    data object AiCopilot : NavTab(Icons.Default.SupportAgent)
    data object SqlDb : NavTab(Icons.Default.Storage)
    data object Settings : NavTab(Icons.Default.Tune)

    fun getTitle(strings: AppStrings): String = when (this) {
        Dashboard -> strings.tabDashboard
        Installments -> strings.tabInstallments
        Analytics -> strings.tabAnalytics
        AiCopilot -> strings.tabAiCopilot
        SqlDb -> strings.tabSqlDb
        Settings -> strings.tabSettings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFintechScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember { SayItPreferences(context) }
    val updateViewModel = remember { AppUpdateViewModel(context) }
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf<NavTab>(NavTab.Dashboard) }
    val tabBackStack = remember { mutableStateListOf<NavTab>(NavTab.Dashboard) }
    var logoTapCount by remember { mutableIntStateOf(0) }
    var isGuideTourOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!prefs.hasCompletedTour && prefs.isPrivacyDisclosureAccepted) {
            isGuideTourOpen = true
        }

        val activity = context as? android.app.Activity
        if (activity?.intent?.getBooleanExtra(UpdateNotificationHelper.EXTRA_LAUNCH_UPDATE_POPUP, false) == true) {
            updateViewModel.openDialog()
        }

        val now = System.currentTimeMillis()
        if (now - prefs.lastUpdateCheckTime > 24 * 60 * 60 * 1000L) {
            prefs.lastUpdateCheckTime = now
            updateViewModel.checkForUpdate(isManual = false, isArabic = uiState.language == AppLanguage.AR)
        }
    }

    fun navigateToTab(tab: NavTab) {
        if (currentTab != tab) {
            tabBackStack.remove(tab)
            tabBackStack.add(tab)
            currentTab = tab
        }
    }

    fun goBack() {
        if (tabBackStack.size > 1) {
            tabBackStack.removeAt(tabBackStack.lastIndex)
            currentTab = tabBackStack.last()
        } else {
            currentTab = NavTab.Dashboard
        }
    }

    BackHandler(enabled = true) {
        when {
            updateUiState.isDialogVisible -> {
                updateViewModel.dismissDialog()
            }
            isGuideTourOpen -> {
                isGuideTourOpen = false
                prefs.hasCompletedTour = true
            }
            uiState.isVoiceDialogOpen -> viewModel.setVoiceDialogOpen(false)
            uiState.isEditBudgetDialogOpen -> viewModel.setEditBudgetDialogOpen(false)
            uiState.isManualAddDialogOpen -> viewModel.setManualAddDialogOpen(false)
            uiState.isEditTransactionDialogOpen -> viewModel.setEditTransactionDialog(null, false)
            uiState.selectedTransaction != null -> viewModel.selectTransaction(null)
            uiState.isDeveloperUnlockDialogOpen -> viewModel.setDeveloperUnlockDialogOpen(false)
            uiState.isImportSmsDialogOpen -> viewModel.setImportSmsDialogOpen(false)
            currentTab != NavTab.Dashboard || tabBackStack.size > 1 -> {
                goBack()
            }
            else -> {
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    val copilotViewModel = remember {
        val db = SayItDatabase.getInstance(context)
        val prefs = SayItPreferences(context)
        val localTools = LocalToolsExecutor(db, prefs)
        val geminiClient = GeminiAiClient()
        val repo = AiCopilotRepositoryImpl(localTools, geminiClient, prefs, db)
        val useCase = AskAiCopilotUseCase(repo)
        AiCopilotViewModel(useCase, prefs, db)
    }

    val installmentsViewModel = remember {
        val db = SayItDatabase.getInstance(context)
        InstallmentsViewModel.Factory(db).create(InstallmentsViewModel::class.java)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(uiState.isDeveloperUnlocked) {
        if (!uiState.isDeveloperUnlocked && currentTab == NavTab.SqlDb) {
            navigateToTab(NavTab.Dashboard)
        }
    }

    val isArabic = uiState.language == AppLanguage.AR

    AppErrorBoundary(isArabic = isArabic) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.pressScale(0.95f, onClick = {
                                logoTapCount++
                                if (logoTapCount >= 5) {
                                    logoTapCount = 0
                                    viewModel.setDeveloperUnlockDialogOpen(true)
                                }
                            })
                        ) {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Emerald500.copy(alpha = 0.15f))
                                    .border(1.dp, Emerald500.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = strings.localOfflineBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Emerald500
                                )
                            }
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val nextLang = if (uiState.language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
                                viewModel.onLanguageChanged(nextLang)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (uiState.language == AppLanguage.AR) "EN" else "عربي",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.shareReport(context) },
                            modifier = Modifier.pressScale(0.9f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = strings.shareReport,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.exportTransactionsCsv(context) },
                            modifier = Modifier.pressScale(0.9f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = strings.exportCsv,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setVoiceDialogOpen(true) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = 0.15f))
                                .pressScale(0.9f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = strings.voiceRecord,
                                tint = Emerald500
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setManualAddDialogOpen(true) },
                            modifier = Modifier.pressScale(0.9f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addManual,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val tabs = if (uiState.isDeveloperUnlocked) {
                            listOf(NavTab.Dashboard, NavTab.Installments, NavTab.Analytics, NavTab.AiCopilot, NavTab.SqlDb, NavTab.Settings)
                        } else {
                            listOf(NavTab.Dashboard, NavTab.Installments, NavTab.Analytics, NavTab.AiCopilot, NavTab.Settings)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEach { tab ->
                                NavTabItem(
                                    tab = tab,
                                    isSelected = currentTab == tab,
                                    strings = strings,
                                    onClick = { navigateToTab(tab) }
                                )
                            }
                        }
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentTab) {
                    NavTab.Dashboard -> DashboardTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        installmentsViewModel = installmentsViewModel,
                        onOpenAiCopilot = { navigateToTab(NavTab.AiCopilot) },
                        onOpenInstallments = { navigateToTab(NavTab.Installments) }
                    )
                    NavTab.Installments -> InstallmentsScreen(
                        viewModel = installmentsViewModel,
                        onNavigateBack = { goBack() }
                    )
                    NavTab.Analytics -> AnalyticsScreen(uiState = uiState)
                    NavTab.AiCopilot -> AiCopilotScreen(
                        viewModel = copilotViewModel,
                        onNavigateBack = { goBack() },
                        onNavigateTarget = { target ->
                            when (target) {
                                NavigationTarget.DASHBOARD -> navigateToTab(NavTab.Dashboard)
                                NavigationTarget.ANALYTICS -> navigateToTab(NavTab.Analytics)
                                NavigationTarget.INSTALLMENTS -> navigateToTab(NavTab.Installments)
                                NavigationTarget.SETTINGS -> navigateToTab(NavTab.Settings)
                                NavigationTarget.BANKS_SYNC -> navigateToTab(NavTab.Settings)
                                NavigationTarget.SMS_IMPORT -> {
                                    navigateToTab(NavTab.Settings)
                                    viewModel.setImportSmsDialogOpen(true)
                                }
                                NavigationTarget.BUDGET_EDIT -> {
                                    viewModel.setEditBudgetDialogOpen(true)
                                }
                                NavigationTarget.EXPORT_REPORT -> {
                                    viewModel.exportTransactionsCsv(context)
                                }
                            }
                        },
                        onStartVoiceInput = { viewModel.setVoiceDialogOpen(true) }
                    )
                    NavTab.SqlDb -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.devModeActive,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CyanAccent
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.lockDeveloperMode()
                                        navigateToTab(NavTab.Dashboard)
                                    }
                                ) {
                                    Text(strings.lockDevMode, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            SqlExplorerScreen(
                                uiState = uiState,
                                onExecuteQuery = viewModel::executeSqlQuery,
                                onSelectTable = viewModel::loadTableColumns,
                                onReseedDatabase = viewModel::reseedDatabase,
                                onClearDatabase = viewModel::clearDatabase
                            )
                        }
                    }
                    NavTab.Settings -> BankHubScreen(
                        uiState = uiState,
                        onUpdateBudget = viewModel::updateMonthlyBudget,
                        onOpenImportSmsDialog = { viewModel.setImportSmsDialogOpen(true) },
                        onOpenDeveloperUnlock = { viewModel.setDeveloperUnlockDialogOpen(true) },
                        onLanguageChanged = viewModel::onLanguageChanged,
                        onOpenAiConfig = {
                            navigateToTab(NavTab.AiCopilot)
                        },
                        onToggleDarkMode = viewModel::toggleDarkMode,
                        onRestartGuideTour = {
                            isGuideTourOpen = true
                        },
                        onCheckForUpdate = {
                            updateViewModel.checkForUpdate(isManual = true, isArabic = isArabic)
                        },
                        isCheckingUpdate = updateUiState.isChecking,
                        isUpToDateFeedback = updateUiState.isUpToDateFeedback,
                        updateErrorMessage = updateUiState.userErrorMessage
                    )
                }
            }
        }
    }

    // Developer PIN Unlock Dialog
    if (uiState.isDeveloperUnlockDialogOpen) {
        DeveloperUnlockDialog(
            correctPin = uiState.developerPin,
            onDismiss = { viewModel.setDeveloperUnlockDialogOpen(false) },
            onUnlockSuccess = {
                viewModel.unlockDeveloperMode()
                navigateToTab(NavTab.SqlDb)
            }
        )
    }

    // Past SMS Inbox Scanner Dialog
    if (uiState.isImportSmsDialogOpen) {
        ImportPastSmsDialog(
            isScanning = uiState.isScanningSms,
            scanResult = uiState.smsScanResult,
            onDismiss = { viewModel.setImportSmsDialogOpen(false) },
            onStartScan = { limit -> viewModel.scanPastSms(context, limit) },
            onConfirmImport = { transactions -> viewModel.confirmImportPastSms(transactions) }
        )
    }

    // Voice Dialog BottomSheet
    if (uiState.isVoiceDialogOpen) {
        VoiceInputDialog(
            onDismiss = { viewModel.setVoiceDialogOpen(false) },
            onSaveTransaction = { tx -> viewModel.saveTransaction(tx) }
        )
    }

    // Edit Budget Dialog
    if (uiState.isEditBudgetDialogOpen) {
        EditBudgetDialog(
            currentBudget = uiState.monthlyBudget,
            onDismiss = { viewModel.setEditBudgetDialogOpen(false) },
            onConfirm = { newBudget ->
                viewModel.updateMonthlyBudget(newBudget)
                viewModel.setEditBudgetDialogOpen(false)
            }
        )
    }



    // Transaction Details & Delete Dialog
    uiState.selectedTransaction?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            onDismiss = { viewModel.selectTransaction(null) },
            onDelete = { id -> viewModel.deleteTransaction(id) },
            onEdit = { toEdit ->
                viewModel.selectTransaction(null)
                viewModel.setEditTransactionDialog(toEdit, true)
            }
        )
    }

    // Edit Transaction Dialog
    if (uiState.isEditTransactionDialogOpen && uiState.transactionToEdit != null) {
        EditTransactionDialog(
            transaction = uiState.transactionToEdit!!,
            categories = uiState.categories,
            onDismiss = { viewModel.setEditTransactionDialog(null, false) },
            onUpdateTransaction = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                viewModel.setEditTransactionDialog(null, false)
            }
        )
    }

    // Manual Add Transaction Dialog
    if (uiState.isManualAddDialogOpen) {
        AddManualTransactionDialog(
            categories = uiState.categories,
            onDismiss = { viewModel.setManualAddDialogOpen(false) },
            onSaveTransaction = { tx ->
                viewModel.saveTransaction(tx)
                viewModel.setManualAddDialogOpen(false)
            }
        )
    }

    // Interactive App Guide Tour
    if (isGuideTourOpen) {
        AppGuideTourDialog(
            isArabic = isArabic,
            onDismiss = {
                prefs.hasCompletedTour = true
                isGuideTourOpen = false
            }
        )
    }

    // App Update Dialog (download progress + install prompt)
    AppUpdateDialog(
        uiState = updateUiState,
        isArabic = isArabic,
        onStartDownload = { updateViewModel.startDownload() },
        onCancelDownload = { updateViewModel.cancelDownload() },
        onInstallUpdate = {
            val result = updateViewModel.installUpdate(context)
            when (result) {
                is InstallResult.Success -> { /* PackageInstaller launched */ }
                is InstallResult.NeedsUnknownSourcePermission -> {
                    context.startActivity(result.intent)
                }
                is InstallResult.Failure -> {
                    // Error is already shown in dialog via uiState
                }
            }
        },
        onDismiss = { updateViewModel.dismissDialog() }
    )
}

@Composable
private fun NavTabItem(
    tab: NavTab,
    isSelected: Boolean,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val tabTitle = tab.getTitle(strings)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .pressScale(0.96f, onClick = onClick)
            .animateContentSize()
            .padding(
                horizontal = if (isSelected) 12.dp else 8.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tabTitle,
            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tabTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DashboardTabContent(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    installmentsViewModel: InstallmentsViewModel,
    onOpenAiCopilot: () -> Unit = {},
    onOpenInstallments: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val greeting = remember(strings) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour in 5..14) strings.morningGreeting else strings.eveningGreeting
    }

    if (uiState.isLoading) {
        DashboardSkeleton()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        // 1. Dynamic Greeting & Interactive AI Companion Banner
        item {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.dashboardSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dynamic Conversational Assistant Pill
                val assistantMessage = remember(uiState.forecast, uiState.language, uiState.filteredTransactions.size) {
                    val isEn = uiState.language == AppLanguage.EN
                    val f = uiState.forecast
                    when {
                        f == null || f.monthlyBudget <= 0 ->
                            if (isEn) "Tap to set a monthly budget for smart financial forecasts."
                            else "انقر هنا لتحديد ميزانية شهرية وتفعيل التنبيهات الذكية."
                        f.isOverBudget ->
                            if (isEn) "Budget exceeded by ${(f.totalSpent - f.monthlyBudget).toInt()} EGP. Tap to review."
                            else "تجاوزت الميزانية بمقدار ${(f.totalSpent - f.monthlyBudget).toInt()} ج.م. انقر للمراجعة."
                        f.estimatedRunwayDayOfMonth != null ->
                            if (isEn) "Runway alert: projected to run out on day ${f.estimatedRunwayDayOfMonth}."
                            else "تنبيه مدرج الصرف: متوقع نفاد الميزانية يوم ${f.estimatedRunwayDayOfMonth}."
                        uiState.filteredTransactions.isEmpty() ->
                            if (isEn) "Ready to log. Tap here to record your first expense by voice."
                            else "جاهز لتسجيل المصروفات. انقر هنا للتحدث بالصوت فوراً."
                        else ->
                            if (isEn) "Healthy spending rate! Remaining: ${f.remainingBudget.toInt()} EGP."
                            else "معدل صرفك ممتاز ومستقر. المتبقي: ${f.remainingBudget.toInt()} ج.م."
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .border(1.dp, CyanAccent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .pressScale(0.97f, onClick = {
                            onOpenAiCopilot()
                        })
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Text(
                            text = assistantMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Hero Luxury Budget & Runway Summary Card
        item {
            uiState.forecast?.let { forecast ->
                BudgetSummaryCard(
                    forecast = forecast,
                    onEditBudgetClick = { viewModel.setEditBudgetDialogOpen(true) }
                )
            }
        }

        // 3. Cashflow Summary Glance (Income, Expense, Net Savings)
        item {
            CashflowSummaryRow(
                totalIncome = uiState.periodIncome,
                totalExpense = uiState.periodExpense
            )
        }

        // 3b. Realtime Spending Flow Curve Graph
        item {
            RealtimeSpendingGraph(
                transactions = if (uiState.filteredTransactions.isNotEmpty()) uiState.filteredTransactions else uiState.transactions,
                monthlyBudget = uiState.monthlyBudget,
                language = uiState.language,
                currency = strings.currency
            )
        }

        // 3c. Monthly Installments & Dues Overview Card
        item {
            val instState by installmentsViewModel.uiState.collectAsStateWithLifecycle()
            InstallmentsDashboardCard(
                uiState = instState,
                onClick = onOpenInstallments
            )
        }

        // 4. Time Period Filter Selector Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimePeriod.values().forEach { period ->
                    val isSelected = uiState.timePeriod == period
                    val periodTitle = if (uiState.language == AppLanguage.EN) period.titleEn else period.titleAr
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                            .pressScale(0.96f, onClick = { viewModel.onTimePeriodChanged(period) })
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = periodTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                letterSpacing = 0.sp
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5. Search & Category Filters
        item {
            Column {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = strings.close)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val isAllSelected = uiState.selectedCategoryId == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isAllSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .pressScale(0.96f, onClick = { viewModel.onCategoryFilterSelected(null) })
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = strings.all,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    items(uiState.categories, key = { it.id }) { cat ->
                        val isSelected = uiState.selectedCategoryId == cat.id
                        val catName = if (uiState.language == AppLanguage.EN) cat.nameEn else cat.nameAr
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .pressScale(0.96f, onClick = { viewModel.onCategoryFilterSelected(cat.id) })
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 6. Transactions Header with Quick Count and Add action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.transactionsHistory} (${uiState.filteredTransactions.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Emerald500.copy(alpha = 0.15f))
                        .pressScale(0.96f, onClick = { viewModel.setManualAddDialogOpen(true) })
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.addManual,
                        tint = Emerald500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.addManual,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Emerald500
                    )
                }
            }
        }

        // 7. Modern Empty State (Refactoring UI 07-design-empty-states)
        if (uiState.filteredTransactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Emerald500.copy(alpha = 0.25f), CyanAccent.copy(alpha = 0.2f))
                                    )
                                )
                                .border(1.dp, Emerald500.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = strings.noTransactions,
                                tint = Emerald500,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = strings.noTransactions,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = strings.noTransactionsDesc,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Emerald600)
                                    .pressScale(0.96f, onClick = { viewModel.setVoiceDialogOpen(true) })
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        strings.voiceRecord,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .pressScale(0.96f, onClick = { viewModel.setManualAddDialogOpen(true) })
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        strings.addManual,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            uiState.groupedTransactions.forEach { dateGroup ->
                item(key = "group_header_${dateGroup.dateMillis}_${dateGroup.title}") {
                    DateHeaderSeparator(
                        title = dateGroup.title,
                        txCount = dateGroup.transactions.size,
                        totalExpense = dateGroup.totalExpense,
                        totalIncome = dateGroup.totalIncome
                    )
                }

                items(
                    items = dateGroup.transactions,
                    key = { it.id }
                ) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onClick = {
                            viewModel.selectTransaction(tx)
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun InstallmentsDashboardCard(
    uiState: com.example.sayit.presentation.installments.InstallmentsUiState,
    onClick: () -> Unit
) {
    val numberFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply { maximumFractionDigits = 0 } }
    val forecast = uiState.forecast

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.97f, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF06B6D4).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "الأقساط والمديونيات الشهرية",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (forecast.activeInstallmentsCount > 0) "${forecast.activeInstallmentsCount} خطط نشطة | مسار التصفير التلقائي" else "تتبع أقساطك والمديونيات بذكاء",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (forecast.activeInstallmentsCount == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أضف خطتك الأولى لحساب مسار التصفير ورصد الـ SMS",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "+ إضافة",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "المتبقي هذا الشهر",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "${numberFormat.format(forecast.currentMonthRemaining)} ج.م",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (forecast.isCurrentMonthFullySettled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (forecast.isCurrentMonthFullySettled) Color(0xFF10B981).copy(alpha = 0.18f)
                                else Color(0xFFF59E0B).copy(alpha = 0.18f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (forecast.isCurrentMonthFullySettled) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تم سداد قسط هذا الشهر",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                )
                            } else {
                                Text(
                                    text = "مسدد: ${numberFormat.format(forecast.currentMonthPaid)} ج.م",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
