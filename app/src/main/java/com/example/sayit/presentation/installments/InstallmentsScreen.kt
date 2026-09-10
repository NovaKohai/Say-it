package com.example.sayit.presentation.installments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
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
import com.example.sayit.theme.Emerald600
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.InstallmentStatus
import com.example.sayit.domain.model.PaymentStatus
import com.example.sayit.presentation.installments.components.AddInstallmentDialog
import com.example.sayit.presentation.installments.components.InstallmentPayoffGraph
import com.example.sayit.presentation.installments.components.RecordPaymentDialog
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun InstallmentsScreen(
    viewModel: InstallmentsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "الأقساط والمديونيات",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "إدارة الأقساط الشهرية ومسار التصفير",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Button(
                    onClick = { isAddDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("خطة جديدة", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Payoff Forecast Graph
            item {
                InstallmentPayoffGraph(
                    forecast = uiState.forecast,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Top Status KPI Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Current Month Summary Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "أقساط هذا الشهر",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${numberFormat.format(uiState.forecast.currentMonthDues)} ج.م",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val isSettled = uiState.forecast.isCurrentMonthFullySettled
                            Text(
                                text = if (isSettled) "تم سداد الشهر بالكامل" else "تم سداد ${numberFormat.format(uiState.forecast.currentMonthPaid)} ج.م",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSettled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Active Plans Count Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "الخطط النشطة",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.forecast.activeInstallmentsCount} خطط",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF06B6D4)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "إجمالي المديونية: ${numberFormat.format(uiState.forecast.totalRemainingDebt)} ج.م",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
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
                            label = { Text(filter.labelAr) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(10.dp)
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
                            text = "لا توجد خطط تقسيط مسجلة في هذا التبويب",
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
            title = { Text("حذف خطة التقسيط") },
            text = { Text("هل أنت متأكد من حذف هذه الخطة وجميع سجلات السداد المرتبطة بها؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInstallment(id)
                        deletingInstallmentId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingInstallmentId = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun InstallmentItemCard(
    installment: Installment,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF06B6D4).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
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
                            )
                        )
                        Text(
                            text = installment.provider.displayNameAr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
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
                        text = "المسدد: ${numberFormat.format(installment.totalPaid)} ج.م",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "الإجمالي: ${numberFormat.format(installment.totalAmount)} ج.م",
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
                        .height(8.dp),
                    color = Color(0xFF10B981),
                    trackColor = Color.White.copy(alpha = 0.1f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Month Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (paymentStatus) {
                            PaymentStatus.PAID -> Color(0xFF10B981).copy(alpha = 0.15f)
                            PaymentStatus.PARTIALLY_PAID -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            PaymentStatus.UNPAID -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (paymentStatus) {
                                PaymentStatus.PAID -> Icons.Default.CheckCircle
                                PaymentStatus.PARTIALLY_PAID -> Icons.Default.Schedule
                                PaymentStatus.UNPAID -> Icons.Default.Schedule
                            },
                            contentDescription = null,
                            tint = when (paymentStatus) {
                                PaymentStatus.PAID -> Color(0xFF10B981)
                                PaymentStatus.PARTIALLY_PAID -> Color(0xFFF59E0B)
                                PaymentStatus.UNPAID -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (paymentStatus) {
                                PaymentStatus.PAID -> "تم سداد قسط هذا الشهر"
                                PaymentStatus.PARTIALLY_PAID -> "مدفوع جزئياً: ${numberFormat.format(paidAmount)} من ${numberFormat.format(dueAmount)} ج.م"
                                PaymentStatus.UNPAID -> "مستحق: ${numberFormat.format(dueAmount)} ج.م (يوم ${installment.dueDayOfMonth})"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = when (paymentStatus) {
                                    PaymentStatus.PAID -> Color(0xFF10B981)
                                    PaymentStatus.PARTIALLY_PAID -> Color(0xFFF59E0B)
                                    PaymentStatus.UNPAID -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )
                    }

                    if (paymentStatus != PaymentStatus.PAID) {
                        OutlinedButton(
                            onClick = onRecordPayment,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF10B981)
                            )
                        ) {
                            Text("تسجيل سداد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF10B981), CircleShape)
                                .size(24.dp),
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
