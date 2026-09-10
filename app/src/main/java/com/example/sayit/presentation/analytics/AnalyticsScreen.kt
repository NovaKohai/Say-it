package com.example.sayit.presentation.analytics

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.presentation.common.getCategoryIcon
import com.example.sayit.presentation.dashboard.DashboardUiState
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.GoldWarning
import com.example.sayit.theme.RedExpense
import java.util.Calendar

@Composable
fun AnalyticsScreen(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isArabic = strings.isArabic
    val isEn = !isArabic
    val forecast = uiState.forecast

    // Current month expenses vs last month expenses calculation
    val nowCal = Calendar.getInstance()
    val curYear = nowCal.get(Calendar.YEAR)
    val curMonth = nowCal.get(Calendar.MONTH)

    val lastMonthCal = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1)
    }
    val lastYear = lastMonthCal.get(Calendar.YEAR)
    val lastMonth = lastMonthCal.get(Calendar.MONTH)

    val currentMonthExpenses = remember(uiState.transactions) {
        uiState.transactions.filter { tx ->
            if (tx.type != TransactionType.EXPENSE) return@filter false
            val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            c.get(Calendar.YEAR) == curYear && c.get(Calendar.MONTH) == curMonth
        }
    }

    val lastMonthExpenses = remember(uiState.transactions) {
        uiState.transactions.filter { tx ->
            if (tx.type != TransactionType.EXPENSE) return@filter false
            val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            c.get(Calendar.YEAR) == lastYear && c.get(Calendar.MONTH) == lastMonth
        }
    }

    val totalExpense = currentMonthExpenses.sumOf { it.amount }
    val lastMonthTotal = lastMonthExpenses.sumOf { it.amount }

    val monthOverMonthDelta = if (lastMonthTotal > 0) {
        ((totalExpense - lastMonthTotal) / lastMonthTotal) * 100
    } else 0.0

    // Group expenses by category
    val categoryTotals = currentMonthExpenses
        .groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Group expenses by merchant (Top places)
    val merchantTotals = currentMonthExpenses
        .groupBy { it.merchant }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    val progress = if ((forecast?.monthlyBudget ?: 0.0) > 0) {
        ((forecast?.totalSpent ?: 0.0) / (forecast?.monthlyBudget ?: 1.0)).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "progress"
    )

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Spending Runway Hero Card (Double-Bezel Architecture)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Emerald500.copy(alpha = 0.35f),
                                CyanAccent.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(1.4.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if ((forecast?.monthlyBudget ?: 0.0) <= 0) {
                        // Zero Budget Notice
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = 0.15f))
                                    .border(1.dp, Emerald500.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = strings.runwayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = strings.zeroBudgetNotice,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = strings.runwayTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.runwaySubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val statusColor = when {
                                    forecast?.isOverBudget == true -> RedExpense
                                    progress > 0.8f -> GoldWarning
                                    else -> Emerald500
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = when {
                                            forecast?.isOverBudget == true -> strings.overBudget
                                            totalExpense == 0.0 -> strings.fullBudget
                                            else -> "${(progress * 100).toInt()}% ${strings.budgetConsumed}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Glowing Progress Bar
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = if (forecast?.isOverBudget == true) RedExpense else Emerald500,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Key Runway Metrics Grid
                            val depletionText = when {
                                forecast?.isOverBudget == true -> strings.overBudget
                                totalExpense == 0.0 -> strings.fullBudget
                                else -> if (isEn) (forecast?.depletionDateEn ?: "End of Month") else (forecast?.depletionDateAr ?: "نهاية الشهر")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RunwayMetricBox(
                                    title = strings.dailyBurnRate,
                                    value = "${(forecast?.dailyBurnRate ?: 0.0).toInt()} ${strings.perDay}",
                                    modifier = Modifier.weight(1f)
                                )
                                RunwayMetricBox(
                                    title = strings.projectedMonthTotal,
                                    value = "${(forecast?.projectedMonthTotal ?: 0.0).toInt()} ${strings.currency}",
                                    modifier = Modifier.weight(1f)
                                )
                                RunwayMetricBox(
                                    title = strings.depletionDate,
                                    value = depletionText,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Month-Over-Month Comparison Indicator
                            if (lastMonthTotal > 0) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (monthOverMonthDelta > 0) RedExpense.copy(alpha = 0.12f)
                                            else Emerald500.copy(alpha = 0.12f)
                                        )
                                        .border(
                                            1.dp,
                                            if (monthOverMonthDelta > 0) RedExpense.copy(alpha = 0.3f)
                                            else Emerald500.copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (monthOverMonthDelta > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = if (monthOverMonthDelta > 0) RedExpense else Emerald500,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${strings.vsLastMonth}: ${if (monthOverMonthDelta > 0) strings.spendingIncrease else strings.spendingDecrease}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "${if (monthOverMonthDelta > 0) "+" else ""}${monthOverMonthDelta.toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (monthOverMonthDelta > 0) RedExpense else Emerald500
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // AI Advice Pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF090D16))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isEn) (forecast?.warningTipEn ?: "No expenses recorded this month yet. Your monthly budget is fully available.") else (forecast?.warningTipAr ?: "لم تسجل أي مصاريف هذا الشهر بعد. ميزانيتك الشهرية كاملة ومتاحة."),
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Spending by Category Breakdown (Double-Bezel Architecture)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.03f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(1.2.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(23.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.spendingByCategory,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = Emerald500
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (categoryTotals.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.noExpensesForCategories,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                categoryTotals.forEach { (catId, amount) ->
                                    val category = uiState.categories.find { it.id == catId }
                                    val percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                                    val catColor = Color(category?.colorHex ?: 0xFF10B981)

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(catColor.copy(alpha = 0.2f))
                                                        .border(1.dp, catColor.copy(alpha = 0.35f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = getCategoryIcon(category?.iconName),
                                                        contentDescription = null,
                                                        tint = catColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = if (isEn) (category?.nameEn ?: "General") else (category?.nameAr ?: "عام"),
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Text(
                                                text = "${amount.toInt()} ${strings.currency} (${(percentage * 100).toInt()}%)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                            progress = { percentage },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = catColor,
                                            trackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Top Spending Merchants (Double-Bezel Architecture)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                CyanAccent.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(1.2.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(23.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.topMerchants,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = CyanAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (merchantTotals.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.noMerchantsYet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                merchantTotals.forEachIndexed { index, (merchant, spent) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF090D16))
                                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(CyanAccent.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = CyanAccent
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = merchant,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = "${spent.toInt()} ${strings.currency}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = RedExpense
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunwayMetricBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF090D16))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
        .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        }
    }
}
