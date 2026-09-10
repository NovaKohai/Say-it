package com.example.sayit.presentation.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.data.voice.AndroidVoiceRecognizer
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.ParsedVoiceTransaction
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.usecase.ParseVoiceInputUseCase
import com.example.sayit.presentation.common.AudioWaveVisualizer
import com.example.sayit.presentation.common.getCategoryIcon
import com.example.sayit.presentation.common.pressScale
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputDialog(
    onDismiss: () -> Unit,
    onSaveTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val isEn = strings.currency == "EGP"
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val voiceUseCase = remember { ParseVoiceInputUseCase() }
    val voiceRecognizer = remember { AndroidVoiceRecognizer(context) }

    val isListening by voiceRecognizer.isListening.collectAsStateWithLifecycle()
    val rmsLevel by voiceRecognizer.rmsLevel.collectAsStateWithLifecycle()
    val transcribedText by voiceRecognizer.transcribedText.collectAsStateWithLifecycle()
    val recognitionError by voiceRecognizer.errorMessage.collectAsStateWithLifecycle()

    var spokenText by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<ParsedVoiceTransaction?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            voiceRecognizer.startListening()
        }
    }

    // Clean up recognizer when dialog leaves composition
    DisposableEffect(Unit) {
        onDispose {
            voiceRecognizer.destroy()
        }
    }

    // Reactively update input text and AI parse when speech recognizer yields text
    LaunchedEffect(transcribedText) {
        if (transcribedText.isNotBlank()) {
            spokenText = transcribedText
            parsedResult = voiceUseCase(transcribedText)
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }

    // Pulsing circle animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEn) "Smart Voice Expense" else "تسجيل المصروف بالصوت",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEn) "Instant recognition of speech & monetary amounts" else "تعرف فوري على العامية المصرية ومبالغ الأرقام",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = strings.close)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Real Mic Button with Ripple
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .background(Emerald500.copy(alpha = 0.2f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Emerald500, CyanAccent))
                        )
                        .pressScale(
                            targetScale = 0.95f,
                            onClick = {
                                if (!permissionGranted) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    if (isListening) {
                                        voiceRecognizer.stopListening()
                                    } else {
                                        voiceRecognizer.startListening()
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) (if (isEn) "Stop recording" else "إيقاف التسجيل") else (if (isEn) "Start speaking" else "بدء التحدث بالصوت"),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Audio Wave Visualizer reacting to real Decibels
            AudioWaveVisualizer(
                isRecording = isListening,
                rmsLevel = rmsLevel,
                modifier = Modifier.height(36.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status indicator
            val statusText = when {
                isListening -> if (isEn) "Listening now... Speak clearly in English or Arabic" else "استمع إليك الآن... تحدث بوضوح بالعامية أو الإنجليزية"
                recognitionError != null -> if (isEn) "$recognitionError (Tap mic to retry)" else "$recognitionError (انقر للمايك لإعادة المحاولة)"
                !permissionGranted -> if (isEn) "Tap the mic to grant recording permission" else "اضغط على المايك لمنح صلاحية التسجيل الصوتي"
                else -> if (isEn) "Tap mic to speak and log an expense" else "اضغط على المايك للتحدث وتدوين المصروف بالصوت"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isListening) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Editable Text Field for Voice Transcript
            OutlinedTextField(
                value = spokenText,
                onValueChange = {
                    spokenText = it
                    parsedResult = if (it.isNotBlank()) voiceUseCase(it) else null
                },
                placeholder = {
                    Text(if (isEn) "e.g. Spent 120 EGP at Starbucks with card..." else "مثال: صرفت 120 جنيه في بلبن كاش...")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Extracted Entity Preview Card (AI Parsed)
            AnimatedVisibility(visible = parsedResult != null && (parsedResult?.amount ?: 0.0) > 0) {
                parsedResult?.let { res ->
                    val cat = Category.findDefault(res.categoryId)
                    val catName = if (isEn) cat.nameEn else cat.nameAr
                    val sourceName = if (isEn) res.paymentSource.titleEn else res.paymentSource.titleAr
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isEn) "Extracted Transaction Details" else "تفاصيل المعاملة المستخرجة",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.colorHex).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = Color(cat.colorHex),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = res.merchant,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "$catName • $sourceName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "${res.amount.toInt()} ${strings.currency}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Emerald600
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    parsedResult?.let { res ->
                        if (res.amount > 0) {
                            val tx = voiceUseCase.toTransaction(res)
                            onSaveTransaction(tx)
                            onDismiss()
                        }
                    }
                },
                enabled = parsedResult != null && (parsedResult?.amount ?: 0.0) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressScale(0.96f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEn) "Confirm & Save Transaction" else "تأكيد وحفظ المعاملة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
