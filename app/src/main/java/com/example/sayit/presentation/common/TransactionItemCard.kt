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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
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
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.theme.GreenIncome
import com.example.sayit.theme.RedExpense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isEn = !strings.isArabic
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountPrefix = if (isExpense) "-" else "+"
    val amountColor = if (isExpense) RedExpense else GreenIncome

    val locale = if (isEn) Locale.ENGLISH else Locale.forLanguageTag("ar")
    val formattedDate = SimpleDateFormat("dd MMM, hh:mm a", locale).format(Date(transaction.timestamp))

    // Authentic Bank & Source Badge Colors
    val (sourceLabel, sourceBgColor, sourceTextColor) = when {
        transaction.paymentSource == PaymentSource.INSTAPAY ->
            Triple(if (isEn) "InstaPay" else "إنستاباي", Color(0xFF6A1B9A), Color.White)
        transaction.paymentSource == PaymentSource.VODAFONE_CASH ->
            Triple(if (isEn) "Vodafone Cash" else "فودافون كاش", Color(0xFFE60000), Color.White)
        transaction.paymentSource == PaymentSource.ETISALAT_CASH ->
            Triple(if (isEn) "Etisalat Cash" else "اتصالات كاش", Color(0xFF709E00), Color.White)
        transaction.paymentSource == PaymentSource.WE_PAY ->
            Triple(if (isEn) "WE Pay" else "وي باي (WE)", Color(0xFF532B88), Color.White)
        transaction.notes?.contains("NBE", ignoreCase = true) == true || transaction.rawText?.contains("الأهلي", ignoreCase = true) == true ->
            Triple(if (isEn) "NBE Bank" else "الأهلي NBE", Color(0xFF006837), Color.White)
        transaction.notes?.contains("BM", ignoreCase = true) == true || transaction.rawText?.contains("مصر", ignoreCase = true) == true ->
            Triple(if (isEn) "Banque Misr" else "بنك مصر", Color(0xFF9E1B32), Color.White)
        transaction.notes?.contains("CIB", ignoreCase = true) == true || transaction.rawText?.contains("CIB", ignoreCase = true) == true ->
            Triple(if (isEn) "CIB Bank" else "بنك CIB", Color(0xFF003366), Color.White)
        transaction.notes?.contains("القاهرة", ignoreCase = true) == true || transaction.rawText?.contains("القاهرة", ignoreCase = true) == true ->
            Triple(if (isEn) "Banque du Caire" else "بنك القاهرة", Color(0xFFC88A23), Color.White)
        transaction.notes?.contains("أبوظبي", ignoreCase = true) == true || transaction.rawText?.contains("أبوظبي", ignoreCase = true) == true ->
            Triple(if (isEn) "ADIB Bank" else "مصرف ADIB", Color(0xFF004B87), Color.White)
        transaction.source == TransactionSource.VOICE ->
            Triple(if (isEn) "Voice" else "تسجيل صوتي", Color(0xFF047857), Color(0xFF6EE7B7))
        else ->
            Triple(
                if (isEn) transaction.paymentSource.titleEn else transaction.paymentSource.titleAr,
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
    }

    // Refactored Card with Emil Kowalski pressScale physics & Refactoring UI visual hierarchy
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .pressScale(targetScale = 0.98f, onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with subtle Glow
            val catColor = Color(transaction.category?.colorHex ?: 0xFF10B981)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.12f))
                    .border(1.dp, catColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category?.iconName),
                    contentDescription = if (isEn) transaction.category?.nameEn else transaction.category?.nameAr,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Merchant & Context Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Bank / Source Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(sourceBgColor)
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = sourceTextColor
                        )
                    }
                }
            }

            // Amount Column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${transaction.amount.toInt()} ${strings.currency}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.5.sp,
                        letterSpacing = 0.sp
                    ),
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isEn) (transaction.category?.nameEn ?: "General") else (transaction.category?.nameAr ?: "عام"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

fun getCategoryIcon(iconName: String?): ImageVector {
    return when (iconName) {
        "Restaurant" -> Icons.Default.Restaurant
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "ReceiptLong" -> Icons.AutoMirrored.Filled.ReceiptLong
        "ShoppingBag" -> Icons.Default.ShoppingBag
        "LocalHospital" -> Icons.Default.LocalHospital
        "SportsEsports" -> Icons.Default.SportsEsports
        "Payments" -> Icons.Default.Payments
        else -> Icons.Default.Category
    }
}
