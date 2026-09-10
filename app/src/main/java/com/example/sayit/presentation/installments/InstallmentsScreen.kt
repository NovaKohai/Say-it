package com.example.sayit.presentation.installments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.PaymentStatus
import com.example.sayit.presentation.common.pressScale
import com.example.sayit.presentation.installments.components.AddInstallmentDialog
import com.example.sayit.presentation.installments.components.InstallmentPayoffGraph
import com.example.sayit.presentation.installments.components.RecordPaymentDialog
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600
import com.example.sayit.theme.GoldWarning
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun InstallmentsScreen(
    viewModel: InstallmentsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isArabic = strings.isArabic
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isAddDialogOpen by remember { mutableStateOf(false) }
    var payingInstallment by remember { mutableStateOf<Installment?>(null) }
    var deletingInstallmentId by remember { mutableStateOf<String?>(null) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .pressScale(0.92f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backDesc,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = strings.installmentsTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = strings.installmentsSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = { isAddDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .pressScale(0.95f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.addInstallment,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Payoff Forecast Graph
            item {
                InstallmentPayoffGraph(
                    forecast = uiState.forecast,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Top Status KPI Summary Cards (Double-Bezel Doppelrand Architecture)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Month Summary Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Emerald500.copy(alpha = 0.30f),
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(1.2.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(19.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = strings.dueThisMonth,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${numberFormat.format(uiState.forecast.currentMonthDues)} ${strings.currency}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val isSettled = uiState.forecast.isCurrentMonthFullySettled
                                Text(
                                    text = if (isSettled) {
                                        if (isArabic) "تم سداد الشهر بالكامل" else "Month Fully Settled"
                                    } else {
                                        if (isArabic) "تم سداد ${numberFormat.format(uiState.forecast.currentMonthPaid)} ${strings.currency}"
                                        else "Paid ${numberFormat.format(uiState.forecast.currentMonthPaid)} ${strings.currency}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSettled) Emerald500 else GoldWarning,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    // Active Plans Count Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CyanAccent.copy(alpha = 0.30f),
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(1.2.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(19.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = strings.activeInstallments,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.forecast.activeInstallmentsCount} ${if (isArabic) "خطط" else "plans"}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${strings.totalDebtRemaining}: ${numberFormat.format(uiState.forecast.totalRemainingDebt)} ${strings.currency}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstallmentFilter.entries.forEach { filter ->
                        val selected = uiState.activeFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(if (isArabic) filter.labelAr else filter.labelEn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                selectedLabelColor = Emerald500
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Installment Cards List
            if (uiState.filteredInstallments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noInstallmentsFound,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(uiState.filteredInstallments, key = { it.id }) { installment ->
                    InstallmentItemCard(
                        installment = installment,
                        isArabic = isArabic,
                        currency = strings.currency,
                        onRecordPayment = { payingInstallment = installment },
                        onDelete = { deletingInstallmentId = installment.id }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Add Installment Dialog
    if (isAddDialogOpen) {
        AddInstallmentDialog(
            onDismiss = { isAddDialogOpen = false },
            onSave = { newInst ->
                viewModel.addInstallment(newInst)
                isAddDialogOpen = false
            }
        )
    }

    // Record Payment Dialog
    payingInstallment?.let { inst ->
        RecordPaymentDialog(
            installment = inst,
            onDismiss = { payingInstallment = null },
            onConfirmPayment = { id, monthYear, amt ->
                viewModel.recordPayment(id, monthYear, amt)
                payingInstallment = null
            }
        )
    }

    // Delete Confirmation Dialog
    deletingInstallmentId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingInstallmentId = null },
            title = { Text(strings.deleteInstallmentConfirm) },
            text = { Text(strings.deleteInstallmentMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInstallment(id)
                        deletingInstallmentId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingInstallmentId = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun InstallmentItemCard(
    installment: Installment,
    isArabic: Boolean,
    currency: String,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    val curCal = Calendar.getInstance()
    val curMonthKey = String.format(Locale.US, "%04d-%02d", curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH) + 1)
    val curRecord = installment.monthlyRecords.firstOrNull { it.monthYear == curMonthKey }

    val dueAmount = curRecord?.dueAmount ?: installment.monthlyAmount
    val paidAmount = curRecord?.paidAmount ?: 0.0
    val paymentStatus = when {
        curRecord?.status == PaymentStatus.PAID || (paidAmount >= dueAmount && dueAmount > 0) -> PaymentStatus.PAID
        paidAmount > 0 -> PaymentStatus.PARTIALLY_PAID
        else -> PaymentStatus.UNPAID
    }

    // Double-Bezel luxury enclosure
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
            )
            .padding(1.2.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(21.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row: Provider & Title + Delete Icon
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanAccent.copy(alpha = 0.15f))
                                .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = installment.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = if (isArabic) installment.provider.displayNameAr else installment.provider.displayNameEn,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .pressScale(0.92f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isArabic) "حذف القسط" else "Delete Installment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar & Total Debt Overview
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${if (isArabic) "المسدد" else "Paid"}: ${numberFormat.format(installment.totalPaid)} $currency",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Emerald500,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "${if (isArabic) "الإجمالي" else "Total"}: ${numberFormat.format(installment.totalAmount)} $currency",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { installment.progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Emerald500,
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Month Status Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when (paymentStatus) {
                                PaymentStatus.PAID -> Emerald500.copy(alpha = 0.15f)
                                PaymentStatus.PARTIALLY_PAID -> GoldWarning.copy(alpha = 0.15f)
                                PaymentStatus.UNPAID -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            }
                        )
                        .border(
                            1.dp,
                            when (paymentStatus) {
                                PaymentStatus.PAID -> Emerald500.copy(alpha = 0.35f)
                                PaymentStatus.PARTIALLY_PAID -> GoldWarning.copy(alpha = 0.35f)
                                PaymentStatus.UNPAID -> Color.White.copy(alpha = 0.08f)
                            },
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (paymentStatus) {
                                    PaymentStatus.PAID -> Icons.Default.CheckCircle
                                    PaymentStatus.PARTIALLY_PAID -> Icons.Default.Schedule
                                    PaymentStatus.UNPAID -> Icons.Default.Schedule
                                },
                                contentDescription = null,
                                tint = when (paymentStatus) {
                                    PaymentStatus.PAID -> Emerald500
                                    PaymentStatus.PARTIALLY_PAID -> GoldWarning
                                    PaymentStatus.UNPAID -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (paymentStatus) {
                                    PaymentStatus.PAID -> if (isArabic) "تم سداد قسط هذا الشهر" else "Month installment paid"
                                    PaymentStatus.PARTIALLY_PAID -> if (isArabic) {
                                        "مدفوع جزئياً: ${numberFormat.format(paidAmount)} من ${numberFormat.format(dueAmount)} $currency"
                                    } else {
                                        "Partially paid: ${numberFormat.format(paidAmount)} / ${numberFormat.format(dueAmount)} $currency"
                                    }
                                    PaymentStatus.UNPAID -> if (isArabic) {
                                        "مستحق: ${numberFormat.format(dueAmount)} $currency (يوم ${installment.dueDayOfMonth})"
                                    } else {
                                        "Due: ${numberFormat.format(dueAmount)} $currency (Day ${installment.dueDayOfMonth})"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (paymentStatus) {
                                        PaymentStatus.PAID -> Emerald500
                                        PaymentStatus.PARTIALLY_PAID -> GoldWarning
                                        PaymentStatus.UNPAID -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            )
                        }

                        if (paymentStatus != PaymentStatus.PAID) {
                            OutlinedButton(
                                onClick = onRecordPayment,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Emerald500
                                ),
                                modifier = Modifier.pressScale(0.95f)
                            ) {
                                Text(
                                    text = if (isArabic) "تسجيل سداد" else "Pay",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Emerald500, CircleShape)
                                    .size(26.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
