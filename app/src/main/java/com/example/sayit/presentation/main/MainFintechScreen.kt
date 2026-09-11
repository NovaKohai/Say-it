package com.example.sayit.presentation.main

import androidx.lifecycle.viewmodel.initializer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.TextButton
import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.core.localization.AppStrings
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.presentation.analytics.AnalyticsScreen
import com.example.sayit.presentation.common.AddManualTransactionDialog
import com.example.sayit.presentation.common.AppErrorBoundary
import com.example.sayit.presentation.common.AppGuideTourDialog
import com.example.sayit.presentation.update.AppUpdateDialog
import com.example.sayit.presentation.update.AppUpdateViewModel
import com.example.sayit.data.util.InstallResult
import com.example.sayit.data.util.UpdateNotificationHelper
import com.example.sayit.presentation.common.DeveloperUnlockDialog
import com.example.sayit.presentation.common.EditBudgetDialog
import com.example.sayit.presentation.common.EditTransactionDialog
import com.example.sayit.presentation.common.ImportPastSmsDialog
import com.example.sayit.presentation.common.TransactionDetailDialog
import com.example.sayit.presentation.common.pressScale
import com.example.sayit.presentation.common.EmilEasings
import com.example.sayit.presentation.common.motionEnabled
import com.example.sayit.presentation.dashboard.DashboardEffect
import com.example.sayit.presentation.dashboard.DashboardViewModel
import com.example.sayit.presentation.database.SqlExplorerScreen
import com.example.sayit.presentation.settings.BankHubScreen
import com.example.sayit.presentation.voice.VoiceInputDialog
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import androidx.compose.material.icons.filled.SupportAgent
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
    prefs: SayItPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val updateViewModel: AppUpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer { AppUpdateViewModel(context.applicationContext) }
        }
    )
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf<NavTab>(NavTab.Dashboard) }
    var isTopBarMenuOpen by remember { mutableStateOf(false) }
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

    val copilotViewModel: AiCopilotViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer {
        val db = SayItDatabase.getInstance(context)
        val prefs = SayItPreferences(context)
        val localTools = LocalToolsExecutor(db, prefs)
        val geminiClient = GeminiAiClient()
        val repo = AiCopilotRepositoryImpl(localTools, geminiClient, prefs, db)
        val useCase = AskAiCopilotUseCase(repo)
        AiCopilotViewModel(useCase, prefs, db)
    } })

    val installmentsViewModel: InstallmentsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = InstallmentsViewModel.Factory(SayItDatabase.getInstance(context.applicationContext))
    )

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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = strings.localOfflineBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
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
                        val pressInteraction21 = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        IconButton(interactionSource = pressInteraction21,
                            onClick = { viewModel.setVoiceDialogOpen(true) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .pressScale(0.9f, interactionSource = pressInteraction21)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = strings.voiceRecord,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        val pressInteraction22 = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        IconButton(interactionSource = pressInteraction22,
                            onClick = { viewModel.setManualAddDialogOpen(true) },
                            modifier = Modifier.pressScale(0.9f, interactionSource = pressInteraction22)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addManual,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box {
                            IconButton(onClick = { isTopBarMenuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = if (uiState.language == AppLanguage.AR) "المزيد" else "More actions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = isTopBarMenuOpen,
                                onDismissRequest = { isTopBarMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.shareReport) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        isTopBarMenuOpen = false
                                        viewModel.shareReport(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.exportCsv) },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                    onClick = {
                                        isTopBarMenuOpen = false
                                        viewModel.exportTransactionsCsv(context)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                BottomNavBar(currentTab, uiState.isDeveloperUnlocked, strings, ::navigateToTab)
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            val allowMotion = motionEnabled()
            val slideDistancePx = with(LocalDensity.current) { 8.dp.roundToPx() }
            val tabOrder = listOf(
                NavTab.Dashboard,
                NavTab.Installments,
                NavTab.Analytics,
                NavTab.AiCopilot,
                NavTab.SqlDb,
                NavTab.Settings
            )

            AnimatedContent(
                targetState = currentTab,
                modifier = Modifier.padding(innerPadding),
                transitionSpec = {
                    if (!allowMotion) {
                        fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                    } else {
                        val movingForward = tabOrder.indexOf(targetState) > tabOrder.indexOf(initialState)
                        val enterOffset = if (movingForward) slideDistancePx else -slideDistancePx
                        val exitOffset = -enterOffset

                        (fadeIn(tween(180, easing = EmilEasings.StrongEaseOut)) +
                            slideInHorizontally(
                                animationSpec = tween(180, easing = EmilEasings.StrongEaseOut)
                            ) { enterOffset }) togetherWith
                            (fadeOut(tween(180, easing = EmilEasings.StrongEaseOut)) +
                                slideOutHorizontally(
                                    animationSpec = tween(180, easing = EmilEasings.StrongEaseOut)
                                ) { exitOffset })
                    }
                },
                label = "tabTransition"
            ) { visibleTab ->
                when (visibleTab) {
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
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.devModeActive,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
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

