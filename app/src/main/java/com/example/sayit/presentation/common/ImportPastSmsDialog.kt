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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.data.sms.SmsScanResult
import com.example.sayit.domain.model.Transaction
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600

@Composable
fun ImportPastSmsDialog(
    isScanning: Boolean,
    scanResult: SmsScanResult?,
    onDismiss: () -> Unit,
    onStartScan: (Int) -> Unit,
    onConfirmImport: (List<Transaction>) -> Unit
) {
    val strings = LocalStrings.current
    val isEn = !strings.isArabic

    var selectedLimit by remember { mutableIntStateOf(50) }
    val limitOptions = listOf(
        Pair(if (isEn) "10 SMS" else "10 رسائل", 10),
        Pair(if (isEn) "25 SMS" else "25 رسالة", 25),
        Pair(if (isEn) "50 SMS" else "50 رسالة", 50),
        Pair(if (isEn) "100 SMS" else "100 رسالة", 100),
        Pair(if (isEn) "All" else "الكل", 0)
    )

    AlertDialog(
        onDismissRequest = { if (!isScanning) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = strings.smsSyncTitle,
                        tint = Emerald500,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = strings.smsSyncTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEn) "Scan inbox & extract bank expenses" else "فحص صندوق الوارد واستخراج مصاريف البنوك",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isEn) "Choose how many recent SMS to scan for bank and digital wallet alerts:" else "اختر عدد الرسائل الأخيرة التي تريد فحصها من هاتفك للبحث عن رسائل البنوك والمحافظ:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Limit Selector Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(limitOptions) { option ->
                        val isSelected = selectedLimit == option.second
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !isScanning) { selectedLimit = option.second }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = option.first,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Scanning Progress
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FintechLoadingSpinner(size = 40.dp, strokeWidth = 3.5.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (isEn) "Reading & filtering bank SMS..." else "جاري قراءة وتصفية رسائل البنوك...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else if (scanResult != null) {
                    // Summary Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isEn) "Scanned: ${scanResult.totalScanned} messages" else "تم فحص: ${scanResult.totalScanned} رسالة",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isEn) "Found: ${scanResult.bankTransactionsFound.size} transactions" else "تم العثور على: ${scanResult.bankTransactionsFound.size} معاملة",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Emerald500
                                )
                            }
                            if (scanResult.skippedDuplicates > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isEn) "Skipped ${scanResult.skippedDuplicates} already recorded duplicates" else "تم تخطي ${scanResult.skippedDuplicates} معاملة مكررة مسجلة مسبقاً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent
                                )
                            }
                        }
                    }

                    // Found Transactions Preview
                    if (scanResult.bankTransactionsFound.isNotEmpty()) {
                        Text(
                            text = if (isEn) "Ready to import:" else "المعاملات الجاهزة للإضافة:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(scanResult.bankTransactionsFound) { tx ->
                                val catName = if (isEn) (tx.category?.nameEn ?: "General") else (tx.category?.nameAr ?: "عام")
                                val sourceName = if (isEn) tx.paymentSource.titleEn else tx.paymentSource.titleAr
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = tx.merchant,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$catName • $sourceName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${tx.amount.toInt()} ${strings.currency}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                        color = Emerald500
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEn) "No new bank transactions found in these messages." else "لم يتم العثور على رسائل بنكية جديدة في هذه الرسائل.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (scanResult != null && scanResult.bankTransactionsFound.isNotEmpty()) {
                Button(
                    onClick = { onConfirmImport(scanResult.bankTransactionsFound) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEn) "Import (${scanResult.bankTransactionsFound.size}) to Database" else "إضافة (${scanResult.bankTransactionsFound.size}) لقاعدة البيانات")
                }
            } else {
                Button(
                    onClick = { onStartScan(selectedLimit) },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val label = if (scanResult == null) {
                        if (isEn) "Start Scanning" else "بدء فحص الرسائل"
                    } else {
                        if (isEn) "Rescan" else "إعادة الفحص"
                    }
                    Text(label)
                }
            }
        },
        dismissButton = {
            if (!isScanning) {
                TextButton(onClick = onDismiss) {
                    Text(strings.close)
                }
            }
        }
    )
}
