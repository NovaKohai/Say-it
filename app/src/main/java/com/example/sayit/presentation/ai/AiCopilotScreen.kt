package com.example.sayit.presentation.ai

import android.content.Intent
import android.net.Uri
import com.example.sayit.data.local.SayItPreferences
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.domain.model.AiAction
import com.example.sayit.domain.model.AiMessage
import com.example.sayit.domain.model.MessageSender
import com.example.sayit.domain.model.NavigationTarget
import com.example.sayit.presentation.ai.components.ActionShortcutChip
import com.example.sayit.presentation.ai.components.BudgetSnapshotWidget
import com.example.sayit.presentation.ai.components.DeleteConfirmationCard
import com.example.sayit.presentation.ai.components.TopMerchantsWidget
import com.example.sayit.presentation.ai.components.TransactionConfirmationCard
import com.example.sayit.presentation.common.pressScale
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500
import com.example.sayit.theme.Emerald600

@Composable
fun AiCopilotScreen(
    viewModel: AiCopilotViewModel,
    onNavigateBack: () -> Unit,
    onNavigateTarget: (NavigationTarget) -> Unit,
    onStartVoiceInput: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isArabic = strings.currency != "EGP"
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showClearChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initGreeting(isArabic)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val suggestions = if (isArabic) {
        listOf(
            "لخصلي مصاريف الشهر",
            "ميزانيتي تمام وفي السليم؟",
            "كام صرفت على الأكل؟",
            "عليا أقساط إيه الشهر ده؟",
            "لو صرفت 5,000 ج.م إيه الحسبة؟",
            "إزاي أظبط ميزانيتي؟",
            "إزاي أسحب رسائل البنك؟"
        )
    } else {
        listOf(
            "Summarize this month's spending",
            "Is my budget safe?",
            "How much did I spend on food?",
            "What installments are due?",
            "What if I spend 5,000 EGP?",
            "How to optimize my budget?",
            "How to import bank SMS?"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Professional Human Top Header with AI Status & Key Setup
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
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

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.15f))
                        .border(1.dp, Emerald500.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = if (isArabic) "المساعد المالي الذكي" else "AI Financial Companion",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (uiState.selectedUserName.isNotBlank()) {
                            if (isArabic) "منور يا ${uiState.selectedUserName} 👋" else "Welcome, ${uiState.selectedUserName} 👋"
                        } else {
                            if (isArabic) "مستشارك وصاحبك المالي الذكي" else "AI Financial Companion (Nemotron)"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // AI Status Indicator (Clean read-only indicator)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (uiState.isApiKeyConfigured) Emerald500.copy(alpha = 0.15f)
                        else Color(0xFFF59E0B).copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (uiState.isApiKeyConfigured) Emerald500.copy(alpha = 0.4f)
                        else Color(0xFFF59E0B).copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isApiKeyConfigured) Emerald500 else Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isApiKeyConfigured) {
                            if (isArabic) "AI نشط" else "AI Active"
                        } else {
                            if (isArabic) "صيانة مؤقتة" else "Maintenance"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.isApiKeyConfigured) Emerald500 else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Clear Chat Action
            IconButton(
                onClick = { showClearChatDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .pressScale(0.92f)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = strings.clearChatDesc,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Suggestions Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .pressScale(0.95f, onClick = { viewModel.sendMessage(suggestion, isArabic) })
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(uiState.messages, key = { it.id }) { msg ->
                ChatMessageBubble(
                    message = msg,
                    onNavigateTarget = onNavigateTarget,
                    onSelectPersona = { personaName ->
                        viewModel.onSelectPersona(personaName, isArabic)
                    },
                    onOpenApiKeySetup = {},
                    onConfirmTransaction = { action ->
                        viewModel.confirmAddTransaction(action, isArabic)
                    },
                    onCancelAction = { actionId ->
                        viewModel.cancelAction(actionId, isArabic)
                    },
                    onConfirmDeleteTransaction = { action ->
                        viewModel.confirmDeleteTransaction(action, isArabic)
                    },
                    onRetryQuery = { prompt ->
                        viewModel.sendMessage(prompt, isArabic)
                    }
                )
            }

            if (uiState.isLoading) {
                item {
                    ThinkingIndicatorBubble(isArabic = isArabic)
                }
            }
        }

        // Bottom Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.onInputTextChanged(it) },
                placeholder = {
                    Text(
                        text = if (isArabic) "اسألني أي حاجة عن فلوسك ومصاريفك..." else "Ask anything about your money and budget...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Emerald500
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onStartVoiceInput,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pressScale(0.92f)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = strings.voiceInputDesc,
                    tint = Emerald500
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { viewModel.sendMessage(uiState.inputText, isArabic) },
                enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (uiState.inputText.isNotBlank() && !uiState.isLoading) Emerald600 else MaterialTheme.colorScheme.surfaceVariant)
                    .pressScale(0.92f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = strings.sendMessageDesc,
                    tint = if (uiState.inputText.isNotBlank() && !uiState.isLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showClearChatDialog) {
            AlertDialog(
                onDismissRequest = { showClearChatDialog = false },
                title = { Text(strings.clearChatConfirmTitle) },
                text = { Text(strings.clearChatConfirmMessage) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearChat(isArabic)
                            showClearChatDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.clearChatDesc)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearChatDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            )
        }
    }
}

@Composable
fun ApiKeyCalloutBanner(
    isArabic: Boolean,
    onActivateClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Brush.linearGradient(listOf(Emerald500, CyanAccent)), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Emerald500, CyanAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isArabic) "فعّل المحادثة الذكية الحقيقية (Gemini AI)" else "Enable Real AI Chat (Gemini)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isArabic) "احصل على استشارات وتحليلات مالية ذكية مجاناً" else "Get personalized financial insights for free",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isArabic) {
                    "الروبوت يحتاج لمفتاح Gemini API المجاني ليعمل بذكاء اصطناعي حقيقي بدلاً من الردود المبرمجة مسبقاً. يستغرق إنشاؤه دقيقة واحدة من Google AI Studio بدون بطاقة بنكية."
                } else {
                    "To enable deep financial reasoning and conversational intelligence, connect your free Gemini API key in 1 minute from Google AI Studio."
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onActivateClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "إدخال مفتاح Gemini API وتفعيل الشات بوت" else "Configure Gemini API Key & Activate",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ApiKeyInputDialog(
    currentKey: String,
    testState: ApiKeyTestState,
    errorMessage: String,
    onKeyChange: (String) -> Unit,
    onTestKey: () -> Unit,
    onSaveKey: () -> Unit,
    onDismiss: () -> Unit,
    isArabic: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isArabic) "مفتاح الذكاء الاصطناعي (OpenRouter / AI)" else "AI API Key (OpenRouter)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isArabic) {
                        "المفتاح مفعل تلقائياً وجاهز للاستخدام مع نموذج (Nvidia Nemotron). يمكنك اختبار الاتصال، إدخال مفتاحك الخاص، أو استعادة المفتاح الافتراضي في أي وقت."
                    } else {
                        "Pre-configured and active with Nvidia Nemotron. You can test the connection, input a custom key, or restore default key anytime."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = currentKey,
                    onValueChange = onKeyChange,
                    placeholder = { Text("sk-or-v1-...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null && clip.text.isNotBlank()) {
                                    onKeyChange(clip.text.trim())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = Emerald500
                            )
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions: Reset to Default & OpenRouter link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onKeyChange(SayItPreferences.DEFAULT_API_KEY)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "⟲ استعادة الافتراضي" else "⟲ Reset Default",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Emerald500
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/keys"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "OpenRouter Keys" else "OpenRouter Keys",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CyanAccent
                        )
                    }
                }

                // Feedback status
                if (testState == ApiKeyTestState.TESTING) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Emerald500)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "جاري فحص المفتاح والاتصال..." else "Testing connection...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (testState == ApiKeyTestState.SUCCESS) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "✓ المفتاح سليم والاتصال ناجح بنسبة 100%" else "✓ Valid API key and connection successful",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Emerald500
                        )
                    }
                } else if (testState == ApiKeyTestState.ERROR || errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (errorMessage.isNotBlank()) errorMessage else if (isArabic) "تعذر الاتصال، يرجى التأكد من المفتاح" else "Connection failed, please check key",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                OutlinedButton(
                    onClick = onTestKey,
                    shape = RoundedCornerShape(10.dp),
                    enabled = currentKey.isNotBlank() && testState != ApiKeyTestState.TESTING
                ) {
                    Text(if (isArabic) "فحص المفتاح" else "Test Key")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSaveKey,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text(if (isArabic) "حفظ وتفعيل" else "Save & Activate", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
fun ChatMessageBubble(
    message: AiMessage,
    onNavigateTarget: (NavigationTarget) -> Unit,
    onSelectPersona: (String) -> Unit = {},
    onOpenApiKeySetup: () -> Unit = {},
    onConfirmTransaction: (AiAction.ConfirmTransaction) -> Unit = {},
    onCancelAction: (String) -> Unit = {},
    onConfirmDeleteTransaction: (AiAction.ConfirmDeleteTransaction) -> Unit = {},
    onRetryQuery: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val strings = LocalStrings.current
    val isArabic = strings.currency != "EGP"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Emerald600 else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(if (message.actions.isNotEmpty()) 0.95f else 0.85f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "المساعد المالي" else "Financial Assistant",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Emerald500
                        )
                    }
                }

                val displayText = if (message.text.trim().equals("null", ignoreCase = true)) {
                    if (isArabic) "معلش يا بطل، حصل تعليق لحظي في الرد. اسألني تاني كده؟" else "Brief hiccup in response. Could you please ask again?"
                } else message.text

                val formattedText = remember(displayText) {
                    parseMarkdownToAnnotatedString(displayText)
                }

                Text(
                    text = formattedText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )

                // Render Persona Choice Buttons if present (Tarek vs Ahmed)
                val personaActions = message.actions.filterIsInstance<AiAction.SelectPersona>()
                if (personaActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        personaActions.forEach { persona ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Emerald600)
                                    .pressScale(0.92f, onClick = { onSelectPersona(persona.name) })
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = persona.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Render Actions & Rich Widgets
                val otherActions = message.actions.filter { it !is AiAction.SelectPersona }
                if (otherActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    otherActions.forEach { action ->
                        when (action) {
                            is AiAction.OpenApiKeySetup -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Emerald600)
                                        .pressScale(0.92f, onClick = onOpenApiKeySetup)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isArabic) action.labelAr else action.labelEn,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            is AiAction.ShowBudgetSnapshot -> {
                                BudgetSnapshotWidget(action = action)
                            }
                            is AiAction.ShowTopMerchants -> {
                                TopMerchantsWidget(action = action)
                            }
                            is AiAction.Navigate -> {
                                ActionShortcutChip(action = action, onClick = { onNavigateTarget(action.target) })
                            }
                            is AiAction.ConfirmTransaction -> {
                                TransactionConfirmationCard(
                                    action = action,
                                    onConfirm = { onConfirmTransaction(action) },
                                    onCancel = { onCancelAction(action.actionId) }
                                )
                            }
                            is AiAction.ConfirmDeleteTransaction -> {
                                DeleteConfirmationCard(
                                    action = action,
                                    onConfirm = { onConfirmDeleteTransaction(action) },
                                    onCancel = { onCancelAction(action.actionId) }
                                )
                            }
                            is AiAction.RetryQuery -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onRetryQuery(action.prompt) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) action.labelAr else action.labelEn,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicatorBubble(isArabic: Boolean) {
    val transition = rememberInfiniteTransition(label = "thinkingWave")
    val dot1Scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = dot1Scale
                            scaleY = dot1Scale
                        }
                        .clip(CircleShape)
                        .background(Emerald500)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = dot2Scale
                            scaleY = dot2Scale
                        }
                        .clip(CircleShape)
                        .background(CyanAccent)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = dot3Scale
                            scaleY = dot3Scale
                        }
                        .clip(CircleShape)
                        .background(Emerald600)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isArabic) "بفكر وبحسبهالك دلوقتي..." else "Thinking & crunching the numbers...",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Parses markdown bold (**text**) into Compose AnnotatedString with FontWeight.Bold.
 * Eliminates raw asterisk characters from LLM responses while preserving rich text formatting.
 */
fun parseMarkdownToAnnotatedString(content: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = content.split("**")
        for (i in parts.indices) {
            if (i % 2 == 1) {
                // Inside **bold**
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(parts[i])
                pop()
            } else {
                append(parts[i])
            }
        }
    }
}

