package com.example.sayit.domain.usecase

import com.example.sayit.domain.model.AiMessage
import com.example.sayit.domain.repository.AiCopilotRepository

class AskAiCopilotUseCase(
    private val repository: AiCopilotRepository
) {
    suspend operator fun invoke(
        userPrompt: String,
        isArabic: Boolean,
        conversationHistory: List<AiMessage> = emptyList()
    ): AiMessage {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank()) {
            return repository.getInitialGreeting(isArabic)
        }
        return repository.queryCopilot(trimmed, isArabic, conversationHistory)
    }

    suspend fun getInitialGreeting(isArabic: Boolean): AiMessage {
        return repository.getInitialGreeting(isArabic)
    }

    fun getGeminiApiKey(): String = repository.getGeminiApiKey()

    fun saveGeminiApiKey(key: String) = repository.saveGeminiApiKey(key)

    suspend fun testGeminiApiKey(key: String): Boolean = repository.testGeminiApiKey(key)
}
