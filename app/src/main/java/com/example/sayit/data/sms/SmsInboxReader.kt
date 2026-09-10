package com.example.sayit.data.sms

import android.content.Context
import android.provider.Telephony
import com.example.sayit.data.parser.BankMessageParser
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.Transaction
import com.example.sayit.domain.model.TransactionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class SmsScanResult(
    val totalScanned: Int,
    val bankTransactionsFound: List<Transaction>,
    val skippedDuplicates: Int
)

class SmsInboxReader(private val context: Context) {

    suspend fun readPastSms(
        limit: Int = 50,
        existingTransactions: List<Transaction> = emptyList()
    ): SmsScanResult = withContext(Dispatchers.IO) {
        val foundTransactions = mutableListOf<Transaction>()
        var totalScanned = 0
        var skippedDuplicates = 0

        val existingSignatures = existingTransactions.map {
            "${it.amount.toInt()}_${it.merchant.lowercase()}"
        }.toSet()

        val contentResolver = context.contentResolver
        val projection = arrayOf(
            Telephony.Sms.Inbox._ID,
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        val sortOrder = if (limit > 0) {
            "${Telephony.Sms.Inbox.DATE} DESC LIMIT $limit"
        } else {
            "${Telephony.Sms.Inbox.DATE} DESC"
        }

        try {
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val addressIdx = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.Inbox.DATE)

                while (it.moveToNext()) {
                    totalScanned++
                    val sender = if (addressIdx != -1) it.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    val parsed = BankMessageParser.parse(sender, body)
                    if (parsed != null && parsed.amount > 0) {
                        val signature = "${parsed.amount.toInt()}_${(parsed.merchant ?: parsed.bankName).lowercase()}"
                        if (existingSignatures.contains(signature)) {
                            skippedDuplicates++
                        } else {
                            val catId = autoCategorize(parsed.merchant ?: parsed.bankName)
                            val category = Category.findDefault(catId)
                            val tx = Transaction(
                                id = UUID.randomUUID().toString(),
                                amount = parsed.amount,
                                currency = parsed.currency,
                                type = parsed.type,
                                categoryId = category.id,
                                category = category,
                                merchant = parsed.merchant ?: parsed.bankName,
                                paymentSource = parsed.paymentSource,
                                timestamp = if (parsed.timestamp > 0) parsed.timestamp else timestamp,
                                rawText = body,
                                source = TransactionSource.SMS,
                                notes = "مستوردة من رسائل الهاتف (${parsed.bankName})"
                            )
                            foundTransactions.add(tx)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied or query error handled safely
        }

        SmsScanResult(
            totalScanned = totalScanned,
            bankTransactionsFound = foundTransactions,
            skippedDuplicates = skippedDuplicates
        )
    }

    private fun autoCategorize(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("uber") || lower.contains("careem") || lower.contains("chillout") ||
            lower.contains("total") || lower.contains("shell") || lower.contains("بنزين") || lower.contains("مواصلات") -> "cat_transport"

            lower.contains("carrefour") || lower.contains("hyper") || lower.contains("seoudi") ||
            lower.contains("spinneys") || lower.contains("سوبرماركت") || lower.contains("بيم") || lower.contains("بقالة") -> "cat_groceries"

            lower.contains("fawry") || lower.contains("bill") || lower.contains("vodafone") ||
            lower.contains("orange") || lower.contains("etisalat") || lower.contains("فاتورة") || lower.contains("شحن") -> "cat_bills"

            lower.contains("pharmacy") || lower.contains("صيدلية") || lower.contains("علاج") || lower.contains("دكتور") -> "cat_health"

            lower.contains("mcdonald") || lower.contains("kfc") || lower.contains("starbucks") ||
            lower.contains("costa") || lower.contains("بلبن") || lower.contains("مطعم") || lower.contains("كافيه") -> "cat_food"

            lower.contains("amazon") || lower.contains("noon") || lower.contains("b.tech") ||
            lower.contains("shopping") || lower.contains("زارا") || lower.contains("zara") -> "cat_shopping"

            else -> "cat_other"
        }
    }
}
