package com.example.sayit.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600

@Composable
fun PrivacyDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val strings = LocalStrings.current
    val isEn = !strings.isArabic

    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Emerald500, CyanAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEn) "Privacy & Data Security" else "سياسة الخصوصية وأمان البيانات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEn) "In accordance with Google Play data protection policies, Say It transparently details how your sensitive data is handled:" else "التزاماً بسياسات Google Play لحماية بياناتك، يوضح تطبيق Say It بشفافية تامة كيفية التعامل مع بياناتك الحساسة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DisclosureItem(
                    icon = Icons.Default.Storage,
                    title = if (isEn) "Local-first storage" else "تخزين محلي أولاً",
                    description = if (isEn) "Transactions and budgets are stored in a local SQLite database and are excluded from Android backups." else "تُخزن المعاملات والميزانية داخل قاعدة SQLite محلية، وتُستبعد من نسخ Android الاحتياطية."
                )

                DisclosureItem(
                    icon = Icons.Default.Security,
                    title = if (isEn) "Optional cloud AI" else "ذكاء اصطناعي سحابي اختياري",
                    description = if (isEn) "If you add an AI API key and send a message, relevant financial summaries and recent transactions are sent to the selected AI provider to answer you. Do not enable AI if you prefer to keep all financial data on this device." else "إذا أضفت مفتاح AI وأرسلت رسالة، يُرسل ملخص مالي ومعاملات حديثة مرتبطة بسؤالك إلى مزود الذكاء الاصطناعي المختار للرد عليك. لا تفعّل AI إذا كنت تفضّل بقاء كل بياناتك المالية على الجهاز."
                )

                DisclosureItem(
                    icon = Icons.Default.Mic,
                    title = if (isEn) "Temporary Audio Processing (Speech Recognition)" else "معالجة الصوت المؤقتة (Speech Recognition)",
                    description = if (isEn) "The microphone is only used when you explicitly tap the voice button to transcribe speech in real-time. No recordings are ever saved." else "يتم استخدام المايكروفون فقط عند ضغطك الصريح على زر التسجيل الصوتي لتحويل كلامك إلى نص لحظياً دون حفظ أي تسجيلات صوتية."
                )

                DisclosureItem(
                    icon = Icons.Default.NotificationsActive,
                    title = if (isEn) "Transaction Notifications (Notification Listener)" else "إشعارات المعاملات (Notification Listener)",
                    description = if (isEn) "Only bank and InstaPay app alerts are read to automatically extract purchase amounts. Personal notifications and OTP codes are strictly ignored." else "تُقرأ إشعارات تطبيقات البنوك وإنستاباي فقط لاستخراج مبالغ الشراء تلقائياً. لا يتم الوصول لأي إشعارات شخصية أو أكواد OTP."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text(
                    text = if (isEn) "Agree & Continue" else "موافق ومتابعة",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun DisclosureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Emerald500,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
