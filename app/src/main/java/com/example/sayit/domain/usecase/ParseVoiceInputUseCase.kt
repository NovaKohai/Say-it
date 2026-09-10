package com.example.sayit.domain.usecase

import com.example.sayit.data.parser.VoiceTextParser
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.ParsedVoiceTransaction
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import java.util.UUID

class ParseVoiceInputUseCase {

    operator fun invoke(spokenText: String): ParsedVoiceTransaction {
        return VoiceTextParser.parse(spokenText)
    }

    fun toTransaction(parsed: ParsedVoiceTransaction): Transaction {
        val category = Category.findDefault(parsed.categoryId)
        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = parsed.amount,
            currency = parsed.currency,
            type = parsed.type,
            categoryId = category.id,
            category = category,
            merchant = parsed.merchant,
            paymentSource = parsed.paymentSource,
            timestamp = System.currentTimeMillis(),
            rawText = parsed.rawSpeech,
            source = TransactionSource.VOICE,
            notes = "إدخال صوتي: \"${parsed.rawSpeech}\""
        )
    }
}
