package com.example.sayit.presentation.installments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.InstallmentProvider
import com.example.sayit.domain.model.InstallmentStatus
import java.util.Calendar
import java.util.UUID

@Composable
fun AddInstallmentDialog(
    onDismiss: () -> Unit,
    onSave: (Installment) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(InstallmentProvider.VALU) }
    var isProviderDropdownOpen by remember { mutableStateOf(false) }

    var monthlyAmountStr by remember { mutableStateOf("") }
    var totalMonthsStr by remember { mutableStateOf("12") }
    var totalAmountStr by remember { mutableStateOf("") }
    var dueDayStr by remember { mutableStateOf("10") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إضافة خطة تقسيط جديدة",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Item Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("اسم القسط / الغرض (مثال: هاتف جديد، سيارة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Provider Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedProvider.displayNameAr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("جهة التقسيط / البنك") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isProviderDropdownOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { isProviderDropdownOpen = true }
                    )
                    DropdownMenu(
                        expanded = isProviderDropdownOpen,
                        onDismissRequest = { isProviderDropdownOpen = false }
                    ) {
                        InstallmentProvider.entries.forEach { prov ->
                            DropdownMenuItem(
                                text = { Text(prov.displayNameAr) },
                                onClick = {
                                    selectedProvider = prov
                                    isProviderDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly Amount & Total Months Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = monthlyAmountStr,
                        onValueChange = { input ->
                            monthlyAmountStr = input
                            errorMessage = null
                            val monthly = input.toDoubleOrNull() ?: 0.0
                            val months = totalMonthsStr.toIntOrNull() ?: 0
                            if (monthly > 0 && months > 0) {
                                totalAmountStr = (monthly * months).toInt().toString()
                            }
                        },
                        label = { Text("القسط الشهري (ج.م)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = totalMonthsStr,
                        onValueChange = { input ->
                            totalMonthsStr = input
                            errorMessage = null
                            val months = input.toIntOrNull() ?: 0
                            val monthly = monthlyAmountStr.toDoubleOrNull() ?: 0.0
                            if (monthly > 0 && months > 0) {
                                totalAmountStr = (monthly * months).toInt().toString()
                            }
                        },
                        label = { Text("عدد الشهور") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Total Debt & Due Day Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = totalAmountStr,
                        onValueChange = { input ->
                            totalAmountStr = input
                            errorMessage = null
                            val total = input.toDoubleOrNull() ?: 0.0
                            val months = totalMonthsStr.toIntOrNull() ?: 0
                            if (total > 0 && months > 0) {
                                monthlyAmountStr = (total / months).toInt().toString()
                            }
                        },
                        label = { Text("إجمالي المديونية") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = dueDayStr,
                        onValueChange = { dueDayStr = it },
                        label = { Text("يوم الاستحقاق") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val monthly = monthlyAmountStr.toDoubleOrNull() ?: 0.0
                            val months = totalMonthsStr.toIntOrNull() ?: 0
                            val total = totalAmountStr.toDoubleOrNull() ?: (monthly * months)
                            val dueDay = dueDayStr.toIntOrNull() ?: 1

                            if (name.isBlank()) {
                                errorMessage = "يرجى كتابة اسم القسط"
                                return@Button
                            }
                            if (monthly <= 0) {
                                errorMessage = "يرجى تحديد القسط الشهري بشكل صحيح"
                                return@Button
                            }
                            if (months <= 0) {
                                errorMessage = "يرجى تحديد عدد الشهور"
                                return@Button
                            }

                            val startCal = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, 28))
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            val endCal = Calendar.getInstance().apply {
                                timeInMillis = startCal.timeInMillis
                                add(Calendar.MONTH, months)
                            }

                            val newInstallment = Installment(
                                id = UUID.randomUUID().toString(),
                                name = name.trim(),
                                provider = selectedProvider,
                                totalAmount = total,
                                monthlyAmount = monthly,
                                startDate = startCal.timeInMillis,
                                endDate = endCal.timeInMillis,
                                totalMonths = months,
                                dueDayOfMonth = dueDay.coerceIn(1, 28),
                                status = InstallmentStatus.ACTIVE,
                                notes = notes.trim().ifBlank { null }
                            )
                            onSave(newInstallment)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("حفظ الخطة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
