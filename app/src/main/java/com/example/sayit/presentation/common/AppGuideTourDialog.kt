package com.example.sayit.presentation.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600

private data class TourStepData(
    val icon: ImageVector,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val badgeAr: String,
    val badgeEn: String,
    val accentColor: Color
)

/**
 * High-craft Interactive Guided Tour for Say It application.
 * Designed according to onboarding-cro and emil-design-eng principles:
 * - Clear Aha moments
 * - High visual polish
 * - Full BackHandler and Dismiss controls
 */
@Composable
fun AppGuideTourDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            TourStepData(
                icon = Icons.Default.AccountBalanceWallet,
                titleAr = "تحكم كامل في ميزانيتك ومعدل حرقك",
                titleEn = "Smart Budget & Live Runway",
                descAr = "حدد ميزانيتك الشهرية وشاهد لحظياً كم تبقى لك، كم تصرف يومياً، وموعد نفاد ميزانيتك المتوقع لتضمن أمانك المالي دائماً.",
                descEn = "Track real-time spending, daily burn rate, and projected depletion date to stay in complete financial safety.",
                badgeAr = "لوحة التحكم",
                badgeEn = "Dashboard",
                accentColor = Emerald500
            ),
            TourStepData(
                icon = Icons.Default.Mic,
                titleAr = "سجّل مصاريفك بالصوت وبالعامية",
                titleEn = "Instant Voice Expense Logging",
                descAr = "اضغط على المايك وقول صرفت إيه وإزاي بالعامية المصرية (مثلاً: 'صرفت 120 جنيه غدا كاش')، والتطبيق هيصنفها ويسجلها في ثانية!",
                descEn = "Tap the mic and speak naturally. Say It parses amounts, merchants, and categories in seconds offline.",
                badgeAr = "الصوت الذكي",
                badgeEn = "Voice AI",
                accentColor = CyanAccent
            ),
            TourStepData(
                icon = Icons.Default.SupportAgent,
                titleAr = "مساعدك المالي الذكي (AI Copilot)",
                titleEn = "Conversational AI Copilot",
                descAr = "شات بوت حقيقي يفهم العامية المصرية، يحلل مصاريفك، يجاوبك على أسئلتك، ويقترح عليك تسجيل أو حذف المعاملات بكروت تأكيد آمنة.",
                descEn = "Real conversational AI assistant that answers budget questions, crunches your numbers, and proposes actions with safety cards.",
                badgeAr = "المساعد الذكي",
                badgeEn = "AI Companion",
                accentColor = Color(0xFF6366F1)
            ),
            TourStepData(
                icon = Icons.Default.Sms,
                titleAr = "مزامنة البنوك والمحافظ بدون إنترنت",
                titleEn = "Offline Bank & Wallet Sync",
                descAr = "قراءة ومزامنة فورية لرسائل البنوك المصرية (الأهلي، مصر، CIB) ومحافظ الكاش (فودافون كاش، إنستاباي) محلياً 100% وبخصوصية تامة.",
                descEn = "Seamless local parsing for Egyptian bank and wallet SMS. Your financial data never leaves your device.",
                badgeAr = "الربط البنكي",
                badgeEn = "Bank Hub",
                accentColor = Color(0xFFF59E0B)
            ),
            TourStepData(
                icon = Icons.Default.CreditCard,
                titleAr = "الأقساط، التحليلات، والوضع الليلي",
                titleEn = "Installments, Analytics & Dark Theme",
                descAr = "تابع مواعيد سداد أقساطك القادمة، استمتع برؤية مصاريفك في رسوم بيانية، وبدّل بين الوضع الليلي والفاتح بضغطة زر من الإعدادات.",
                descEn = "Stay on top of due installments, visual analytics, and effortlessly toggle between dark and light themes.",
                badgeAr = "الأقساط والمظهر",
                badgeEn = "Installments",
                accentColor = Color(0xFFEC4899)
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }

    // Intercept Android Back button inside the tour
    BackHandler(enabled = true) {
        if (currentStep > 0) {
            currentStep--
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            val step = steps[currentStep]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Row: Badge & Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(step.accentColor.copy(alpha = 0.15f))
                            .border(1.dp, step.accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isArabic) step.badgeAr else step.badgeEn,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = step.accentColor
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تخطي الجولة" else "Skip",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content Animated Switch
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            animationSpec = tween(280),
                            initialOffsetX = { fullWidth -> direction * fullWidth / 2 }
                        ) + fadeIn(tween(280)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(280),
                                    targetOffsetX = { fullWidth -> -direction * fullWidth / 2 }
                                ) + fadeOut(tween(200))
                            )
                    },
                    label = "tourStepAnimation"
                ) { stepIdx ->
                    val activeStep = steps[stepIdx]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Hero Icon Circle with glowing gradient
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            activeStep.accentColor.copy(alpha = 0.25f),
                                            activeStep.accentColor.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .border(2.dp, activeStep.accentColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeStep.icon,
                                contentDescription = null,
                                tint = activeStep.accentColor,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (isArabic) activeStep.titleAr else activeStep.titleEn,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isArabic) activeStep.descAr else activeStep.descEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(steps.size) { idx ->
                        val isSelected = idx == currentStep
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 24.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) step.accentColor
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons (Previous / Next / Finish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = if (isArabic) "السابق" else "Back",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val isLastStep = currentStep == steps.size - 1
                    Button(
                        onClick = {
                            if (isLastStep) {
                                onDismiss()
                            } else {
                                currentStep++
                            }
                        },
                        modifier = Modifier
                            .weight(if (currentStep > 0) 1.5f else 1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastStep) Emerald600 else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLastStep) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "ابدأ الاستخدام" else "Get Started",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = if (isArabic) "التالي (${currentStep + 1}/${steps.size})" else "Next (${currentStep + 1}/${steps.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
