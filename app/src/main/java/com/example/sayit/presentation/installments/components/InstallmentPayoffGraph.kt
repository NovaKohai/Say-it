package com.example.sayit.presentation.installments.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.domain.model.InstallmentPayoffForecast
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InstallmentPayoffGraph(
    forecast: InstallmentPayoffForecast,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val isArabic = strings.isArabic
    val points = forecast.monthlyProjection
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.primary.copy(alpha = 0.35f),
                        colors.secondary.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                )
            )
            .padding(1.2.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(23.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header with Payoff Milestone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.primary.copy(alpha = 0.15f))
                                .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.debtPayoffCurve,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = strings.projectedZeroDebt,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    if (forecast.totalRemainingDebt <= 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.primary.copy(alpha = 0.2f))
                                .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.debtFreeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                )
                            }
                        }
                    } else if (points.size > 1) {
                        val finalPoint = points.last()
                        val payoffLabel = if (isArabic) finalPoint.monthLabel else finalPoint.monthLabelEn.ifEmpty { finalPoint.monthLabel }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.secondary.copy(alpha = 0.15f))
                                .border(1.dp, colors.secondary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${strings.payoffLabel}: $payoffLabel",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.secondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (points.isEmpty() || forecast.totalRemainingDebt <= 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (forecast.totalRemainingDebt <= 0) {
                                strings.noDebtCongrats
                            } else {
                                strings.addFirstPlanPayoff
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                } else {
                    // Interactive Bézier Graph
                    val maxDebt = (points.maxOfOrNull { it.remainingDebt } ?: 1.0).coerceAtLeast(1.0)
                    val emeraldGradient = Brush.verticalGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.45f),
                            colors.secondary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                    val lineColor = colors.primary

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .semantics { contentDescription = "${strings.debtPayoffCurve}: ${strings.totalDebtRemaining} ${numberFormat.format(forecast.totalRemainingDebt)} ${strings.currency}" }
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 24.dp.toPx()
                        val graphHeight = height - paddingBottom

                        val stepX = if (points.size > 1) width / (points.size - 1) else width

                        // Horizontal reference grid lines
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = graphHeight * (i.toFloat() / gridLines)
                            drawLine(
                                color = colors.outlineVariant,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        // Build Bézier Curve Path
                        val curvePath = Path()
                        val fillPath = Path()

                        val coordinates = points.mapIndexed { index, pt ->
                            val x = index * stepX
                            val normalized = (pt.remainingDebt / maxDebt).toFloat().coerceIn(0f, 1f)
                            val y = graphHeight - (normalized * (graphHeight - 20f))
                            Offset(x, y)
                        }

                        if (coordinates.isNotEmpty()) {
                            curvePath.moveTo(coordinates[0].x, coordinates[0].y)
                            fillPath.moveTo(coordinates[0].x, graphHeight)
                            fillPath.lineTo(coordinates[0].x, coordinates[0].y)

                            for (i in 0 until coordinates.size - 1) {
                                val p0 = coordinates[i]
                                val p1 = coordinates[i + 1]
                                val controlX1 = (p0.x + p1.x) / 2
                                val controlY1 = p0.y
                                val controlX2 = (p0.x + p1.x) / 2
                                val controlY2 = p1.y

                                curvePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            }

                            val lastCoord = coordinates.last()
                            fillPath.lineTo(lastCoord.x, graphHeight)
                            fillPath.close()

                            // Draw Gradient Fill
                            drawPath(fillPath, brush = emeraldGradient)

                            // Draw Curve Stroke
                            drawPath(
                                path = curvePath,
                                color = lineColor,
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw Nodes
                            coordinates.forEachIndexed { idx, coord ->
                                val isLast = idx == coordinates.size - 1
                                val nodeColor = if (isLast) colors.secondary else colors.primary

                                drawCircle(
                                    color = colors.surface,
                                    radius = 5.dp.toPx(),
                                    center = coord
                                )
                                drawCircle(
                                    color = nodeColor,
                                    radius = 3.5.dp.toPx(),
                                    center = coord
                                )
                            }
                        }
                    }

                    // Month Labels Below Graph
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayedIndices = when {
                            points.size <= 4 -> points.indices.toList()
                            points.size <= 7 -> listOf(0, points.size / 2, points.size - 1)
                            else -> listOf(0, points.size / 3, (points.size * 2) / 3, points.size - 1)
                        }

                        for (idx in displayedIndices) {
                            val pt = points[idx]
                            val label = if (isArabic) pt.monthLabel else pt.monthLabelEn.ifEmpty { pt.monthLabel }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Forecast Quick Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatColumn(
                        label = strings.totalDebtRemaining,
                        value = "${numberFormat.format(forecast.totalRemainingDebt)} ${strings.currency}",
                        valueColor = MaterialTheme.colorScheme.onSurface
                    )
                    StatColumn(
                        label = strings.paidSoFarLabel,
                        value = "${numberFormat.format(forecast.totalPaidSoFar)} ${strings.currency}",
                        valueColor = colors.primary
                    )
                    StatColumn(
                        label = strings.dueThisMonth,
                        value = "${numberFormat.format(forecast.currentMonthDues)} ${strings.currency}",
                        valueColor = colors.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
    }
}
