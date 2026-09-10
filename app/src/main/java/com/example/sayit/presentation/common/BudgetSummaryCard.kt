package com.example.sayit.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.domain.model.SpendingForecast
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald300
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600
import com.example.sayit.theme.GoldWarning
import com.example.sayit.theme.RedExpense

@Composable
fun BudgetSummaryCard(
    forecast: SpendingForecast,
    onEditBudgetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val progress = if (forecast.monthlyBudget > 0) {
        (forecast.totalSpent / forecast.monthlyBudget).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = EmilEasings.StrongEaseOut),
        label = "progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            forecast.isOverBudget -> RedExpense
            progress > 0.80f -> GoldWarning
            else -> Emerald500
        },
        label = "progressColor"
    )

    // Double-Bezel Architecture: Machined Outer Shell with Glowing Gradient Border
    Box(
        modifier = modifier
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
            .padding(1.5.dp)
    ) {
        // Inner Obsidian Core
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (forecast.monthlyBudget <= 0) {
                // Zero-Budget Onboarding State
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Emerald500.copy(alpha = 0.15f))
                                .border(1.dp, Emerald500.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Emerald300)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.monthlyBudget,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.sp
                                    ),
                                    color = Emerald300
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Emerald500.copy(alpha = 0.2f), CyanAccent.copy(alpha = 0.2f)))
                            )
                            .border(1.dp, Emerald500.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = strings.dailyBurnRate,
                                tint = Emerald300,
                                modifier = Modifier.size(28.dp)
                            )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = strings.startSetBudget,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = strings.startSetBudgetDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    if (forecast.totalSpent > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${strings.totalSpentLabel} ${forecast.totalSpent.toInt()} ${strings.currency}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onEditBudgetClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(0.96f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.setBudgetNow,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(22.dp)) {
                    // Header Row: Eyebrow tag + Edit Budget Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Eyebrow Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Emerald500.copy(alpha = 0.15f))
                                .border(1.dp, Emerald500.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Emerald300)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.smartRemainingBudget,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.sp
                                    ),
                                    color = Emerald300
                                )
                            }
                        }

                        // Edit Button with tactile Emil press feedback
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .pressScale(targetScale = 0.90f, onClick = onEditBudgetClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = strings.edit,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Bold Numeric Balance with Emil AnimatedNumericText
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnimatedNumericText(
                            targetValue = forecast.remainingBudget.toInt(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 38.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = if (forecast.isOverBudget) RedExpense else Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.remainingCurrency,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Glowing Progress Bar with Smooth Easing
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Spent vs Total Label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.spent} ${forecast.totalSpent.toInt()} ${strings.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${strings.budgetCap} ${forecast.monthlyBudget.toInt()} ${strings.currency}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Runway & Burn Rate Dual Badges with tactile press response
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Burn Rate Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .pressScale(0.96f)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = GoldWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = strings.dailyBurnRate,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "${forecast.dailyBurnRate.toInt()} ${strings.perDay}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Depletion Date Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .pressScale(0.96f)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = strings.depletionDate,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    val depletionStr = if (strings.currency == "EGP") forecast.depletionDateEn else forecast.depletionDateAr
                                    Text(
                                        text = depletionStr,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
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
