package com.example.sayit.domain.usecase

import com.example.sayit.data.parser.BankMessageParser
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.repository.TransactionRepository
import java.util.UUID

class ProcessBankMessageUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        sender: String,
        messageContent: String,
        source: TransactionSource = TransactionSource.SMS
    ): Transaction? {
        val parsed = BankMessageParser.parse(sender, messageContent) ?: return null

        val matchedCategoryId = autoCategorize(parsed.merchant ?: parsed.bankName)
        val category = Category.findDefault(matchedCategoryId)

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = parsed.amount,
            currency = parsed.currency,
            type = parsed.type,
            categoryId = category.id,
            category = category,
            merchant = parsed.merchant ?: parsed.bankName,
            paymentSource = parsed.paymentSource,
            timestamp = parsed.timestamp,
            rawText = messageContent,
            source = source,
            notes = "من ${parsed.bankName}" + (parsed.cardLast4?.let { " (كارت **$it)" } ?: "")
        )

        transactionRepository.insertTransaction(transaction)
        return transaction
    }

    private fun autoCategorize(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("uber") || lower.contains("careem") || lower.contains("chillout") ||
                lower.contains("بنزين") || lower.contains("مواصلات") -> "cat_transport"

            lower.contains("carrefour") || lower.contains("hyper") || lower.contains("seoudi") ||
                lower.contains("spinneys") || lower.contains("سوبرماركت") || lower.contains("بيم") || lower.contains("بقالة") -> "cat_groceries"

            lower.contains("fawry") || lower.contains("bill") || lower.contains("vodafone") ||
                lower.contains("orange") || lower.contains("etisalat") || lower.contains("فاتورة") || lower.contains("شحن") -> "cat_bills"

            lower.contains("pharmacy") || lower.contains("صيدلية") || lower.contains("علاج") || lower.contains("دكتور") -> "cat_health"

            lower.contains("mcdonald") || lower.contains("kfc") || lower.contains("starbucks") ||
                lower.contains("costa") || lower.contains("بلبن") || lower.contains("مطعم") || lower.contains("كافيه") -> "cat_food"

            lower.contains("amazon") || lower.contains("noon") || lower.contains("b.tech") ||
                lower.contains("زارا") || lower.contains("zara") -> "cat_shopping"

            else -> "cat_other"
        }
    }
}
