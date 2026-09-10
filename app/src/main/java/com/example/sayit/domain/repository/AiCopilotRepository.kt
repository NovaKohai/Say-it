package com.example.sayit.domain.repository

import com.example.sayit.domain.model.AiMessage

interface AiCopilotRepository {
    suspend fun queryCopilot(
        userPrompt: String,
        isArabic: Boolean,
        conversationHistory: List<AiMessage> = emptyList()
    ): AiMessage

    suspend fun getInitialGreeting(isArabic: Boolean): AiMessage

    fun getGeminiApiKey(): String
    fun saveGeminiApiKey(key: String)
    suspend fun testGeminiApiKey(key: String): Boolean
}
