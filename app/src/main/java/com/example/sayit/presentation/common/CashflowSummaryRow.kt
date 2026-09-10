package com.example.sayit.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.GreenIncome
import com.example.sayit.theme.RedExpense

@Composable
fun CashflowSummaryRow(
    totalIncome: Double,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) {
        ((netSavings / totalIncome) * 100).toInt()
    } else 0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Income Card
        CashflowMetricCard(
            title = strings.income,
            numericValue = totalIncome.toInt(),
            prefix = "+",
            accentColor = GreenIncome,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            subLabel = strings.totalInflow,
            modifier = Modifier.weight(1f)
        )

        // 2. Expense Card
        CashflowMetricCard(
            title = strings.expenses,
            numericValue = totalExpense.toInt(),
            prefix = "-",
            accentColor = RedExpense,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            subLabel = strings.totalOutflow,
            modifier = Modifier.weight(1f)
        )

        // 3. Net Savings Card
        val savingsColor = if (netSavings >= 0) CyanAccent else RedExpense
        val savingsSign = if (netSavings > 0) "+" else ""
        CashflowMetricCard(
            title = strings.netSavings,
            numericValue = netSavings.toInt(),
            prefix = savingsSign,
            accentColor = savingsColor,
            icon = Icons.Default.Savings,
            subLabel = if (totalIncome > 0) "$savingsRate% ${strings.savingsRate}" else strings.savings,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
private fun CashflowMetricCard(
    title: String,
    numericValue: Int,
    prefix: String,
    accentColor: Color,
    icon: ImageVector,
    subLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .pressScale(targetScale = 0.96f)
            .border(
                1.dp,
                accentColor.copy(alpha = 0.22f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedNumericText(
                targetValue = numericValue,
                prefix = prefix,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = accentColor,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
