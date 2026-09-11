package com.example.sayit.presentation.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.core.util.TimePeriod
import com.example.sayit.presentation.common.BudgetSummaryCard
import com.example.sayit.presentation.common.CashflowSummaryRow
import com.example.sayit.presentation.common.DateHeaderSeparator
import com.example.sayit.presentation.common.EmilEasings
import com.example.sayit.presentation.common.TransactionItemCard
import com.example.sayit.presentation.common.motionEnabled
import com.example.sayit.presentation.common.pressScale
import com.example.sayit.presentation.dashboard.DashboardUiState
import com.example.sayit.presentation.dashboard.DashboardViewModel
import com.example.sayit.presentation.dashboard.components.DashboardSkeleton
import com.example.sayit.presentation.dashboard.components.RealtimeSpendingGraph
import com.example.sayit.presentation.installments.InstallmentsViewModel
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500

@Composable
internal fun DashboardTabContent(
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
    val allowMotion = motionEnabled()

    AnimatedContent(
        targetState = uiState.isLoading,
        transitionSpec = {
            if (!allowMotion) {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            } else {
                fadeIn(
                    animationSpec = tween(280, easing = EmilEasings.StrongEaseOut)
                ) togetherWith fadeOut(
                    animationSpec = tween(180, easing = EmilEasings.StrongEaseOut)
                )
            }
        },
        label = "dashboardLoadingTransition"
    ) { loading ->
        if (loading) {
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
                        val assistantMessage = remember(uiState.forecast, uiState.language, uiState.transactions.size) {
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
                                uiState.transactions.isEmpty() ->
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
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .heightIn(min = 48.dp)
                                .pressScale(0.97f, haptic = false, onClick = {
                                    when {
                                        uiState.forecast == null || uiState.monthlyBudget <= 0 ->
                                            viewModel.setEditBudgetDialogOpen(true)
                                        uiState.transactions.isEmpty() ->
                                            viewModel.setVoiceDialogOpen(true)
                                        else -> onOpenAiCopilot()
                                    }
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
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                Text(
                                    text = assistantMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
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

                if (uiState.transactions.isEmpty() && uiState.monthlyBudget > 0) {
                    item {
                        FirstTransactionCard(
                            isArabic = uiState.language == AppLanguage.AR,
                            onVoiceClick = { viewModel.setVoiceDialogOpen(true) },
                            onManualClick = { viewModel.setManualAddDialogOpen(true) }
                        )
                    }
                }

                // 3b. Realtime Spending Flow Curve Graph
                if (uiState.transactions.isNotEmpty()) {
                    item {
                        RealtimeSpendingGraph(
                            transactions = uiState.filteredTransactions,
                            monthlyBudget = uiState.monthlyBudget,
                            language = uiState.language,
                            currency = strings.currency
                        )
                    }
                }

                // 3c. Monthly Installments & Dues Overview Card
                item {
                    val instState by installmentsViewModel.uiState.collectAsStateWithLifecycle()
                    InstallmentsDashboardCard(
                        uiState = instState,
                        onClick = onOpenInstallments
                    )
                }

                // 4. Transactions Section Header with Quick Count and Add action
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.transactionsHistory,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${uiState.filteredTransactions.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .heightIn(min = 40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .pressScale(0.96f, haptic = false, onClick = { viewModel.setManualAddDialogOpen(true) })
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addManual,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.addManual,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 5. Time Period Filter Selector Chips
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
                                    .heightIn(min = 42.dp)
                                    .semantics { selected = isSelected }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    )
                                    .pressScale(0.96f, haptic = false, onClick = { viewModel.onTimePeriodChanged(period) })
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = periodTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 6. Search & Category Filters
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                val isAllSelected = uiState.selectedCategoryId == null
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 42.dp)
                                        .semantics { selected = isAllSelected }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isAllSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .pressScale(0.96f, haptic = false, onClick = { viewModel.onCategoryFilterSelected(null) })
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.all,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            items(uiState.categories, key = { it.id }) { cat ->
                                val isSelected = uiState.selectedCategoryId == cat.id
                                val catName = if (uiState.language == AppLanguage.EN) cat.nameEn else cat.nameAr
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 42.dp)
                                        .semantics { selected = isSelected }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .pressScale(0.96f, haptic = false, onClick = { viewModel.onCategoryFilterSelected(cat.id) })
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Modern Empty State (Refactoring UI 07-design-empty-states)
                if (uiState.transactions.isNotEmpty() && uiState.filteredTransactions.isEmpty()) {
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
                                                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                            )
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (uiState.language == AppLanguage.AR) "لا توجد نتائج مطابقة" else "No matching transactions",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (uiState.language == AppLanguage.AR) "غيّر كلمات البحث أو امسح فلاتر الفئة لعرض معاملاتك." else "Try another search or clear the category filter to see your transactions.",
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
                                            .background(MaterialTheme.colorScheme.primary)
                                            .heightIn(min = 48.dp)
                                            .pressScale(0.96f, haptic = false, onClick = {
                                                viewModel.onSearchQueryChanged("")
                                                viewModel.onCategoryFilterSelected(null)
                                            })
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                if (uiState.language == AppLanguage.AR) "مسح الفلاتر" else "Clear filters",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .heightIn(min = 48.dp)
                                            .pressScale(0.96f, haptic = false, onClick = { viewModel.setManualAddDialogOpen(true) })
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
}

@Composable
private fun FirstTransactionCard(
    isArabic: Boolean,
    onVoiceClick: () -> Unit,
    onManualClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isArabic) "سجّل أول معاملة" else "Log your first transaction",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isArabic) "ابدأ بالصوت أو أضف البيانات بنفسك. ستظهر التحليلات بعد أول معاملة." else "Use your voice or enter the details yourself. Insights appear after your first transaction.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.Button(
                    onClick = onVoiceClick,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabic) "سجّل بالصوت" else "Use voice")
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onManualClick,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabic) "إضافة يدوية" else "Add manually")
                }
            }
        }
    }
}
