package com.example.sayit.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600
import com.example.sayit.theme.GreenIncome
import com.example.sayit.theme.RedExpense

@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onUpdateTransaction: (Transaction) -> Unit
) {
    val strings = LocalStrings.current
    val isEn = strings.currency == "EGP"

    val initialAmount = if (transaction.amount % 1.0 == 0.0) {
        transaction.amount.toInt().toString()
    } else {
        transaction.amount.toString()
    }

    var amountText by remember { mutableStateOf(initialAmount) }
    var merchantText by remember { mutableStateOf(transaction.merchant) }
    var notesText by remember { mutableStateOf(transaction.notes ?: "") }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedSource by remember { mutableStateOf(transaction.paymentSource) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }

    val paymentSources = listOf(
        Pair(PaymentSource.CASH, if (isEn) PaymentSource.CASH.titleEn else PaymentSource.CASH.titleAr),
        Pair(PaymentSource.BANK_CARD, if (isEn) PaymentSource.BANK_CARD.titleEn else PaymentSource.BANK_CARD.titleAr),
        Pair(PaymentSource.INSTAPAY, if (isEn) PaymentSource.INSTAPAY.titleEn else PaymentSource.INSTAPAY.titleAr),
        Pair(PaymentSource.VODAFONE_CASH, if (isEn) PaymentSource.VODAFONE_CASH.titleEn else PaymentSource.VODAFONE_CASH.titleAr),
        Pair(PaymentSource.ETISALAT_CASH, if (isEn) PaymentSource.ETISALAT_CASH.titleEn else PaymentSource.ETISALAT_CASH.titleAr),
        Pair(PaymentSource.WE_PAY, if (isEn) PaymentSource.WE_PAY.titleEn else PaymentSource.WE_PAY.titleAr),
        Pair(PaymentSource.TELDA, if (isEn) PaymentSource.TELDA.titleEn else PaymentSource.TELDA.titleAr)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.editTransaction,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Selector (مصروف / دخل)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isExpense = selectedType == TransactionType.EXPENSE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isExpense) RedExpense else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedType = TransactionType.EXPENSE }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.expenseType,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isExpense) GreenIncome else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedType = TransactionType.INCOME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.incomeType,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("${strings.amountLabel} (${strings.currency})") },
                    placeholder = { Text(strings.amountPlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Merchant / Title
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text(strings.merchantLabel) },
                    placeholder = { Text(strings.merchantPlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text(strings.notesLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection
                Text(
                    text = "${strings.categoryLabel}:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val catColor = Color(cat.colorHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) catColor
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedCategoryId = cat.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getCategoryIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEn) cat.nameEn else cat.nameAr,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Payment Source Selection
                Text(
                    text = "${strings.paymentMethodLabel}:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paymentSources) { (source, label) ->
                        val isSelected = selectedSource == source
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedSource = source }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && merchantText.isNotBlank()) {
                        val category = categories.find { it.id == selectedCategoryId }
                            ?: Category.findDefault(selectedCategoryId)

                        val updated = transaction.copy(
                            amount = amount,
                            type = selectedType,
                            merchant = merchantText.trim(),
                            categoryId = selectedCategoryId,
                            category = category,
                            paymentSource = selectedSource,
                            notes = notesText.trim().ifBlank { null }
                        )
                        onUpdateTransaction(updated)
                        onDismiss()
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && merchantText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                modifier = Modifier.pressScale(0.95f)
            ) {
                Icon(Icons.Default.Check, contentDescription = strings.save, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.pressScale(0.95f)
            ) {
                Text(strings.cancel)
            }
        }
    )
}
