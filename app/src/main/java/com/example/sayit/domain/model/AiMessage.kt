package com.example.sayit.domain.model

import java.util.UUID

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class AiEngineType {
    LOCAL_FAST,
    GEMINI_CLOUD
}

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val actions: List<AiAction> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val sourceEngine: AiEngineType = AiEngineType.LOCAL_FAST,
    val error: AppError? = null
)
