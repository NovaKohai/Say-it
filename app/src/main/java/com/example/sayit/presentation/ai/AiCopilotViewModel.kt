package com.example.sayit.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.local.SayItPreferences
import com.example.sayit.data.local.mapper.toEntity
import com.example.sayit.domain.model.ActionStatus
import com.example.sayit.domain.model.AiAction
import com.example.sayit.domain.model.AiEngineType
import com.example.sayit.domain.model.AiMessage
import com.example.sayit.domain.model.MessageSender
import com.example.sayit.domain.model.NavigationTarget
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.usecase.AskAiCopilotUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ApiKeyTestState {
    IDLE,
    TESTING,
    SUCCESS,
    ERROR
}

data class AiCopilotUiState(
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val activeEngine: AiEngineType = AiEngineType.LOCAL_FAST,
    val selectedUserName: String = "",
    val isApiKeyConfigured: Boolean = false,
    val isApiKeyDialogOpen: Boolean = false,
    val currentApiKeyInput: String = "",
    val apiKeyTestState: ApiKeyTestState = ApiKeyTestState.IDLE,
    val apiKeyErrorMessage: String = ""
)

class AiCopilotViewModel(
    private val askCopilotUseCase: AskAiCopilotUseCase,
    private val preferences: SayItPreferences,
    private val database: SayItDatabase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AiCopilotUiState(
            activeEngine = if (preferences.geminiApiKey.isNotBlank()) AiEngineType.GEMINI_CLOUD else AiEngineType.LOCAL_FAST,
            selectedUserName = preferences.userPreferredName,
            isApiKeyConfigured = preferences.geminiApiKey.isNotBlank(),
            currentApiKeyInput = preferences.geminiApiKey
        )
    )
    val uiState: StateFlow<AiCopilotUiState> = _uiState.asStateFlow()

    fun initGreeting(isArabic: Boolean) {
        if (_uiState.value.messages.isEmpty()) {
            viewModelScope.launch {
                val greeting = askCopilotUseCase.getInitialGreeting(isArabic)
                _uiState.update { it.copy(messages = listOf(greeting)) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun openApiKeyDialog() {
        // Disabled: API Key is managed securely and internally
    }

    fun dismissApiKeyDialog() {
        _uiState.update {
            it.copy(
                isApiKeyDialogOpen = false,
                apiKeyTestState = ApiKeyTestState.IDLE
            )
        }
    }

    fun onApiKeyInputChanged(key: String) {
        _uiState.update {
            it.copy(
                currentApiKeyInput = key,
                apiKeyTestState = ApiKeyTestState.IDLE,
                apiKeyErrorMessage = ""
            )
        }
    }

    fun testApiKey() {
        val key = _uiState.value.currentApiKeyInput.trim()
        if (key.isBlank()) {
            _uiState.update {
                it.copy(
                    apiKeyTestState = ApiKeyTestState.ERROR,
                    apiKeyErrorMessage = "يرجى كتابة أو لصق المفتاح أولاً"
                )
            }
            return
        }

        _uiState.update { it.copy(apiKeyTestState = ApiKeyTestState.TESTING) }
        viewModelScope.launch {
            val isValid = askCopilotUseCase.testGeminiApiKey(key)
            _uiState.update {
                if (isValid) {
                    it.copy(apiKeyTestState = ApiKeyTestState.SUCCESS, apiKeyErrorMessage = "")
                } else {
                    it.copy(
                        apiKeyTestState = ApiKeyTestState.ERROR,
                        apiKeyErrorMessage = "المفتاح غير صالح أو تعذر الاتصال بـ Google AI"
                    )
                }
            }
        }
    }

    fun saveApiKey(isArabic: Boolean) {
        val key = _uiState.value.currentApiKeyInput.trim()
        askCopilotUseCase.saveGeminiApiKey(key)
        val hasKey = key.isNotBlank()

        _uiState.update {
            it.copy(
                isApiKeyConfigured = hasKey,
                activeEngine = if (hasKey) AiEngineType.GEMINI_CLOUD else AiEngineType.LOCAL_FAST,
                isApiKeyDialogOpen = false,
                apiKeyTestState = ApiKeyTestState.IDLE
            )
        }

        if (hasKey) {
            val confirmation = AiMessage(
                sender = MessageSender.ASSISTANT,
                text = if (isArabic) {
                    "✨ تم تفعيل المساعد المالي الذكي بنجاح!\nأنا الآن جاهز للإجابة على كل استفساراتك المالية بحرية وبذكاء كامل. بماذا تحب أن نبدأ؟"
                } else {
                    "✨ Real AI Copilot activated successfully!\nI am ready to chat freely and answer any financial question. What would you like to ask?"
                },
                sourceEngine = AiEngineType.GEMINI_CLOUD
            )
            _uiState.update { it.copy(messages = it.messages + confirmation) }
        }
    }

    fun onSelectPersona(name: String, isArabic: Boolean) {
        preferences.userPreferredName = name
        _uiState.update { it.copy(selectedUserName = name) }

        val userMessage = AiMessage(
            sender = MessageSender.USER,
            text = name,
            sourceEngine = _uiState.value.activeEngine
        )

        val replyText = if (isArabic) {
            "عاش يا $name، منور يا بطل! 👋\nأنا معاك خطوة بخطوة عشان نظبط ميزانيتك ومصاريفك.\nتحب نراجع ميزانية الشهر سوا، ولا نشوف أكتر أماكن طيرت فيها فلوسك؟"
        } else {
            "Welcome, $name. I am here to help you stay on top of your budget and spending.\nWould you like to review this month's budget or check your top spend merchants?"
        }

        val assistantReply = AiMessage(
            sender = MessageSender.ASSISTANT,
            text = replyText,
            actions = listOf(
                AiAction.Navigate(
                    target = NavigationTarget.ANALYTICS,
                    labelAr = "مراجعة التحليلات والمصاريف",
                    labelEn = "Review Analytics & Expenses"
                )
            ),
            sourceEngine = _uiState.value.activeEngine
        )

        _uiState.update {
            it.copy(messages = it.messages + userMessage + assistantReply)
        }
    }

    fun sendMessage(userPrompt: String, isArabic: Boolean) {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val userMessage = AiMessage(
            sender = MessageSender.USER,
            text = trimmed,
            sourceEngine = _uiState.value.activeEngine
        )

        val history = _uiState.value.messages

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val response = askCopilotUseCase(trimmed, isArabic, history)
                _uiState.update {
                    it.copy(
                        messages = it.messages + response,
                        isLoading = false,
                        activeEngine = response.sourceEngine,
                        selectedUserName = preferences.userPreferredName
                    )
                }
            } catch (e: Exception) {
                val userName = preferences.userPreferredName.trim()
                val greeting = if (userName.isNotBlank()) "يا $userName" else "يا بطل"
                val retryAction = AiAction.RetryQuery(
                    prompt = trimmed
                )
                val errMessage = AiMessage(
                    sender = MessageSender.ASSISTANT,
                    text = if (isArabic) {
                        "معلش $greeting، حصل دروب بسيط في الاتصال بالخادم. متقلقش، بياناتك وميزانيتك في أمان تام.\nاضغط على 'إعادة المحاولة' لإعادة إرسال السؤال فوراً."
                    } else {
                        "Temporary connection hiccup. Your records and budget are safe.\nTap 'Retry' to resend your query."
                    },
                    actions = listOf(retryAction),
                    sourceEngine = AiEngineType.LOCAL_FAST
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errMessage,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun confirmAddTransaction(action: AiAction.ConfirmTransaction, isArabic: Boolean) {
        viewModelScope.launch {
            if (database != null) {
                val newTx = Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    amount = action.amount,
                    currency = "EGP",
                    type = action.type,
                    categoryId = action.categoryId,
                    merchant = action.merchant,
                    paymentSource = try { PaymentSource.valueOf(action.paymentSource.uppercase()) } catch (_: IllegalArgumentException) { PaymentSource.CASH },
                    timestamp = System.currentTimeMillis(),
                    rawText = action.notes,
                    source = TransactionSource.MANUAL,
                    notes = action.notes ?: if (isArabic) "تسجيل بواسطة المساعد الذكي" else "Logged via AI Copilot"
                )
                database.insertTransaction(newTx.toEntity())
            }

            _uiState.update { state ->
                val updatedMessages = state.messages.map { msg ->
                    val updatedActions = msg.actions.map { a ->
                        if (a is AiAction.ConfirmTransaction && a.actionId == action.actionId) {
                            a.copy(status = ActionStatus.CONFIRMED)
                        } else a
                    }
                    msg.copy(actions = updatedActions)
                }

                val confirmationReply = AiMessage(
                    sender = MessageSender.ASSISTANT,
                    text = if (isArabic) {
                        "تمام يا ${state.selectedUserName.ifBlank { "بطل" }}! سجلتلك ${action.amount.toInt()} ج.م في ${action.merchant} ونزلت في حساباتك وميزانيتك فوراً ✅"
                    } else {
                        "All set! Logged ${action.amount.toInt()} EGP for ${action.merchant} to your transactions and budget ✅"
                    },
                    sourceEngine = state.activeEngine
                )
                state.copy(messages = updatedMessages + confirmationReply)
            }
        }
    }

    fun cancelAction(actionId: String, isArabic: Boolean) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                val updatedActions = msg.actions.map { a ->
                    when {
                        a is AiAction.ConfirmTransaction && a.actionId == actionId -> a.copy(status = ActionStatus.CANCELLED)
                        a is AiAction.ConfirmDeleteTransaction && a.actionId == actionId -> a.copy(status = ActionStatus.CANCELLED)
                        else -> a
                    }
                }
                msg.copy(actions = updatedActions)
            }

            val cancelReply = AiMessage(
                sender = MessageSender.ASSISTANT,
                text = if (isArabic) "تمام يا غالي، لغيت العملية ولا تشيل هم 👍" else "Cancelled, no changes made 👍",
                sourceEngine = state.activeEngine
            )
            state.copy(messages = updatedMessages + cancelReply)
        }
    }

    fun confirmDeleteTransaction(action: AiAction.ConfirmDeleteTransaction, isArabic: Boolean) {
        viewModelScope.launch {
            if (database != null) {
                database.deleteTransaction(action.transactionId)
            }

            _uiState.update { state ->
                val updatedMessages = state.messages.map { msg ->
                    val updatedActions = msg.actions.map { a ->
                        if (a is AiAction.ConfirmDeleteTransaction && a.actionId == action.actionId) {
                            a.copy(status = ActionStatus.CONFIRMED)
                        } else a
                    }
                    msg.copy(actions = updatedActions)
                }

                val confirmationReply = AiMessage(
                    sender = MessageSender.ASSISTANT,
                    text = if (isArabic) {
                        "مسحتلك معاملة ${action.merchant} بقيمة ${action.amount.toInt()} ج.م من الحسابات بنجاح 🗑️"
                    } else {
                        "Deleted transaction for ${action.merchant} (${action.amount.toInt()} EGP) successfully 🗑️"
                    },
                    sourceEngine = state.activeEngine
                )
                state.copy(messages = updatedMessages + confirmationReply)
            }
        }
    }

    fun clearChat(isArabic: Boolean) {
        viewModelScope.launch {
            val greeting = askCopilotUseCase.getInitialGreeting(isArabic)
            _uiState.update { it.copy(messages = listOf(greeting), inputText = "") }
        }
    }
}
