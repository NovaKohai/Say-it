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
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.theme.Emerald600
import com.example.sayit.theme.GreenIncome
import com.example.sayit.theme.RedExpense
import java.util.UUID

@Composable
fun AddManualTransactionDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSaveTransaction: (Transaction) -> Unit
) {
    val strings = LocalStrings.current
    val isEn = strings.currency == "EGP"

    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedSource by remember { mutableStateOf(PaymentSource.CASH) }
    var selectedCategoryId by remember {
        mutableStateOf(categories.firstOrNull()?.id ?: "cat_food")
    }

    val paymentSources = listOf(
        Pair(PaymentSource.CASH, if (isEn) PaymentSource.CASH.titleEn else PaymentSource.CASH.titleAr),
        Pair(PaymentSource.BANK_CARD, if (isEn) PaymentSource.BANK_CARD.titleEn else PaymentSource.BANK_CARD.titleAr),
        Pair(PaymentSource.INSTAPAY, if (isEn) PaymentSource.INSTAPAY.titleEn else PaymentSource.INSTAPAY.titleAr),
        Pair(PaymentSource.VODAFONE_CASH, if (isEn) PaymentSource.VODAFONE_CASH.titleEn else PaymentSource.VODAFONE_CASH.titleAr),
        Pair(PaymentSource.ETISALAT_CASH, if (isEn) PaymentSource.ETISALAT_CASH.titleEn else PaymentSource.ETISALAT_CASH.titleAr),
        Pair(PaymentSource.WE_PAY, if (isEn) PaymentSource.WE_PAY.titleEn else PaymentSource.WE_PAY.titleAr)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = strings.addTransactionTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Selector (Expense / Income)
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
                            color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val isIncome = selectedType == TransactionType.INCOME
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isIncome) GreenIncome else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedType = TransactionType.INCOME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.incomeType,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(strings.amountLabel) },
                    placeholder = { Text(strings.amountPlaceholder) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Merchant / Place Field
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text(strings.merchantLabel) },
                    placeholder = { Text(strings.merchantPlaceholder) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Source Selector
                Text(
                    text = strings.paymentMethodLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Category Selector
                Text(
                    text = strings.categoryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val catColor = Color(cat.colorHex)
                        val catName = if (isEn) cat.nameEn else cat.nameAr
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
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
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val defaultMerchant = if (isEn) "Manual Transaction" else "معاملة يدوية"
                    val defaultNotes = if (isEn) "Manual Entry" else "إدخال يدوي"
                    val merchant = merchantText.trim().ifBlank { defaultMerchant }
                    val category = categories.find { it.id == selectedCategoryId }

                    if (amount > 0) {
                        val tx = Transaction(
                            id = UUID.randomUUID().toString(),
                            amount = amount,
                            currency = "EGP",
                            type = selectedType,
                            categoryId = selectedCategoryId,
                            category = category,
                            merchant = merchant,
                            paymentSource = selectedSource,
                            timestamp = System.currentTimeMillis(),
                            source = TransactionSource.MANUAL,
                            notes = defaultNotes
                        )
                        onSaveTransaction(tx)
                        onDismiss()
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.pressScale(0.95f)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.save)
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
