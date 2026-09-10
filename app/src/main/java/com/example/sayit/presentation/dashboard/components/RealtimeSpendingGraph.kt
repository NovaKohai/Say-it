package com.example.sayit.presentation.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.domain.model.Transaction
import com.example.sayit.presentation.common.EmilEasings
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class DailySpendingPoint(
    val dayLabel: String,
    val fullDateLabel: String,
    val amount: Double,
    val timestamp: Long
)

@Composable
fun RealtimeSpendingGraph(
    transactions: List<Transaction>,
    monthlyBudget: Double,
    language: AppLanguage,
    currency: String,
    modifier: Modifier = Modifier
) {
    val isEn = language == AppLanguage.EN
    val locale = remember(language) {
        if (isEn) Locale.ENGLISH else Locale.forLanguageTag("ar")
    }

    // Process transactions into daily spending points
    val spendingPoints = remember(transactions, language) {
        calculateDailySpendingPoints(transactions, locale, isEn)
    }

    val totalSpent = remember(spendingPoints) { spendingPoints.sumOf { it.amount } }
    val maxSpentPoint = remember(spendingPoints) { spendingPoints.maxByOrNull { it.amount } }
    val avgSpent = remember(spendingPoints) {
        if (spendingPoints.isNotEmpty()) totalSpent / spendingPoints.size else 0.0
    }

    // Interactive point selection
    var selectedIndex by remember(spendingPoints) { mutableIntStateOf(-1) }

    // Live breathing animation for the live pulse dot
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Chart entrance animation (snappy sub-300ms curve)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(280, easing = EmilEasings.StrongEaseOut),
        label = "graph_draw"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title and Live Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Emerald500.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isEn) "Realtime Spending Flow" else "حركة المصروفات اللحظية",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn) "Live daily outflow activity" else "معدل الصرف اليومي المباشر",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live Indicator Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Emerald500.copy(alpha = 0.12f))
                        .border(1.dp, Emerald500.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .graphicsLayer { alpha = pulseAlpha }
                                .background(Emerald500)
                        )
                        Text(
                            text = if (isEn) "LIVE" else "تحديث لحظي",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.sp
                            ),
                            color = Emerald500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Total
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = if (isEn) "Total Spent" else "إجمالي المنفق",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.5.sp,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${totalSpent.toInt()} $currency",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )

                // Metric 2: Daily Avg
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEn) "Daily Avg" else "متوسط اليوم",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.5.sp,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${avgSpent.toInt()} $currency",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )

                // Metric 3: Peak Day
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isEn) "Peak Day" else "أعلى يوم",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.5.sp,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(maxSpentPoint?.amount ?: 0.0).toInt()} $currency",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.sp
                        ),
                        color = Emerald500
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tooltip preview if point is selected
            if (selectedIndex in spendingPoints.indices) {
                val pt = spendingPoints[selectedIndex]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${pt.fullDateLabel}: ${pt.amount.toInt()} $currency",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Realtime Curved Area Graph Canvas
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            val maxAmount = remember(spendingPoints) {
                (spendingPoints.maxOfOrNull { it.amount } ?: 100.0).coerceAtLeast(100.0) * 1.15
            }
            val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .pointerInput(spendingPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            if (spendingPoints.size > 1 && width > 0) {
                                val stepX = width / (spendingPoints.size - 1)
                                val idx = (offset.x / stepX + 0.5f).toInt().coerceIn(0, spendingPoints.lastIndex)
                                selectedIndex = if (selectedIndex == idx) -1 else idx
                            }
                        }
                    }
                    .pointerInput(spendingPoints) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                val width = size.width
                                if (spendingPoints.size > 1 && width > 0) {
                                    val stepX = width / (spendingPoints.size - 1)
                                    val idx = (change.position.x / stepX + 0.5f).toInt().coerceIn(0, spendingPoints.lastIndex)
                                    selectedIndex = idx
                                }
                            },
                            onDragEnd = {
                                // Keep selected or reset after short delay if desired
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height
                    if (spendingPoints.isEmpty() || width <= 0f || height <= 0f) return@Canvas

                    // Draw 3 subtle horizontal guide lines
                    val steps = 3
                    for (i in 1..steps) {
                        val y = height * (i.toFloat() / (steps + 1))
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    // Compute point coordinates
                    val count = spendingPoints.size
                    val stepX = if (count > 1) width / (count - 1) else width
                    val coords = spendingPoints.mapIndexed { index, pt ->
                        val x = if (count > 1) index * stepX else width / 2f
                        val normalizedVal = (pt.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                        val y = height - (normalizedVal * height * animatedProgress).coerceAtLeast(6.dp.toPx())
                        Offset(x, y)
                    }

                    // Build smooth Bézier curve path
                    val curvePath = Path()
                    val fillPath = Path()

                    curvePath.moveTo(coords.first().x, coords.first().y)
                    fillPath.moveTo(coords.first().x, height)
                    fillPath.lineTo(coords.first().x, coords.first().y)

                    for (i in 0 until coords.size - 1) {
                        val p0 = coords[i]
                        val p1 = coords[i + 1]
                        val cx1 = p0.x + (p1.x - p0.x) / 2f
                        val cy1 = p0.y
                        val cx2 = p0.x + (p1.x - p0.x) / 2f
                        val cy2 = p1.y
                        curvePath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                        fillPath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                    }

                    fillPath.lineTo(coords.last().x, height)
                    fillPath.close()

                    // Draw Area Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Emerald500.copy(alpha = 0.32f),
                                CyanAccent.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw Main Curve Line Stroke
                    drawPath(
                        path = curvePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Emerald500, CyanAccent)
                        ),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw active points or selected point
                    coords.forEachIndexed { i, offset ->
                        val isSelected = i == selectedIndex
                        val isLast = i == coords.lastIndex

                        if (isSelected) {
                            // Vertical guide line
                            drawLine(
                                color = CyanAccent.copy(alpha = 0.6f),
                                start = Offset(offset.x, 0f),
                                end = Offset(offset.x, height),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = dashEffect
                            )
                            // Outer glowing circle
                            drawCircle(
                                color = CyanAccent.copy(alpha = 0.35f),
                                radius = 12.dp.toPx(),
                                center = offset
                            )
                            // Core point
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = offset
                            )
                            drawCircle(
                                color = Emerald500,
                                radius = 3.5.dp.toPx(),
                                center = offset
                            )
                        } else if (isLast) {
                            // Subtle breathing pulse for latest point
                            drawCircle(
                                color = Emerald500.copy(alpha = 0.25f * pulseAlpha),
                                radius = 8.dp.toPx(),
                                center = offset
                            )
                            drawCircle(
                                color = Emerald500,
                                radius = 4.dp.toPx(),
                                center = offset
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = offset
                            )
                        }
                    }
                }
            }

            // Bottom X-Axis Day Labels
            if (spendingPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelIndices = getStepIndices(spendingPoints.size, maxLabels = 5)
                    labelIndices.forEach { idx ->
                        val point = spendingPoints[idx]
                        Text(
                            text = point.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.sp
                            ),
                            color = if (idx == selectedIndex) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Groups transactions into daily buckets for the active period.
 * If empty, generates the last 7 calendar days with 0 expenses to render an elegant baseline.
 */
private fun calculateDailySpendingPoints(
    transactions: List<Transaction>,
    locale: Locale,
    isEn: Boolean
): List<DailySpendingPoint> {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val dayFormat = SimpleDateFormat("d MMM", locale)
    val shortDayFormat = SimpleDateFormat("E", locale)

    val calendar = Calendar.getInstance()

    if (expenses.isEmpty()) {
        // Fallback to last 7 days baseline
        val list = mutableListOf<DailySpendingPoint>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 12)
            }
            list.add(
                DailySpendingPoint(
                    dayLabel = shortDayFormat.format(cal.time),
                    fullDateLabel = dayFormat.format(cal.time),
                    amount = 0.0,
                    timestamp = cal.timeInMillis
                )
            )
        }
        return list
    }

    // Group expenses by calendar date (yyyy-MM-dd)
    val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val grouped = expenses.groupBy { keyFormat.format(Date(it.timestamp)) }

    // Find min and max dates
    val minTimestamp = expenses.minOf { it.timestamp }
    val maxTimestamp = expenses.maxOf { it.timestamp }.coerceAtLeast(System.currentTimeMillis())

    val startCal = Calendar.getInstance().apply {
        timeInMillis = minTimestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val endCal = Calendar.getInstance().apply {
        timeInMillis = maxTimestamp
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
    }

    val result = mutableListOf<DailySpendingPoint>()
    val currentCal = startCal.clone() as Calendar

    // Limit points to at most 31 days to ensure beautiful curve density
    val dayDiff = ((endCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt().coerceIn(0, 31)
    if (dayDiff > 30) {
        startCal.timeInMillis = endCal.timeInMillis - (30L * 24 * 60 * 60 * 1000)
    }

    while (!currentCal.after(endCal) && result.size < 31) {
        val key = keyFormat.format(currentCal.time)
        val dayTxs = grouped[key] ?: emptyList()
        val totalAmount = dayTxs.sumOf { it.amount }

        result.add(
            DailySpendingPoint(
                dayLabel = shortDayFormat.format(currentCal.time),
                fullDateLabel = dayFormat.format(currentCal.time),
                amount = totalAmount,
                timestamp = currentCal.timeInMillis
            )
        )
        currentCal.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Ensure at least 3 points for a smooth curve
    if (result.size == 1) {
        val single = result.first()
        val prevCal = Calendar.getInstance().apply {
            timeInMillis = single.timestamp
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return listOf(
            DailySpendingPoint(shortDayFormat.format(prevCal.time), dayFormat.format(prevCal.time), 0.0, prevCal.timeInMillis),
            single
        )
    }

    return result
}

private fun getStepIndices(totalCount: Int, maxLabels: Int): List<Int> {
    if (totalCount <= maxLabels) return (0 until totalCount).toList()
    val step = (totalCount - 1).toFloat() / (maxLabels - 1)
    return (0 until maxLabels).map { (it * step + 0.5f).toInt().coerceIn(0, totalCount - 1) }.distinct()
}
