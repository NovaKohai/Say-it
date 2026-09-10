package com.example.sayit.data.parser

import com.example.sayit.domain.model.ParsedBankTransaction
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.TransactionType

object BankMessageParser {

    data class BankPattern(
        val bankIdentifier: String,
        val paymentSource: PaymentSource,
        val regexes: List<Regex>
    )

    // Filter out OTPs and security alerts
    private val securityOtpFilter = Regex(
        """(?i)(otp|كود التحقق|رمز التأكيد|one time password|secret code|do not share|لا تشارك|password)"""
    )

    fun isFinancialMessage(message: String): Boolean {
        if (securityOtpFilter.containsMatchIn(message) && !message.contains("تمت عملية") && !message.contains("Purchase")) {
            return false
        }
        val financialKeywords = listOf(
            "شراء", "خصم", "سحب", "تحويل", "استلام", "مبلغ", "رصيد", "بطاقة",
            "purchase", "debit", "spent", "transfer", "received", "egp", "جم", "ج.م"
        )
        return financialKeywords.any { message.contains(it, ignoreCase = true) }
    }

    fun parse(sender: String, message: String): ParsedBankTransaction? {
        if (!isFinancialMessage(message)) return null

        val lowerMsg = message.lowercase()
        val detectedBank = detectBankName(sender, message)

        // 1. InstaPay Pattern
        if (sender.contains("instapay", ignoreCase = true) || message.contains("instapay", ignoreCase = true) || message.contains("إنستاباي")) {
            return parseInstaPay(message, detectedBank)
        }

        // 2. Vodafone / Mobile Wallets
        if (sender.contains("vodafone", ignoreCase = true) || sender.contains("vf-cash", ignoreCase = true) || message.contains("فودافون كاش")) {
            return parseVodafoneCash(message)
        }

        // 3. Etisalat Cash
        if (sender.contains("etisalat", ignoreCase = true) || message.contains("اتصالات كاش")) {
            return parseEtisalatCash(message)
        }

        // 4. WE Pay
        if (sender.contains("we", ignoreCase = true) && (message.contains("we pay", ignoreCase = true) || message.contains("وي باي"))) {
            return parseWePay(message)
        }

        // 5. Telda Pattern
        if (sender.contains("telda", ignoreCase = true) || message.contains("telda", ignoreCase = true)) {
            return parseTelda(message)
        }

        // 6. Standard Bank Card Purchase (NBE, BM, CIB, BDC, ADIB, QNB, etc.)
        return parseStandardBankCard(message, detectedBank)
    }

    private fun detectBankName(sender: String, message: String): String {
        return when {
            sender.contains("NBE", ignoreCase = true) || message.contains("الأهلي") -> "البنك الأهلي المصري"
            sender.contains("BM", ignoreCase = true) || message.contains("بنك مصر") -> "بنك مصر"
            sender.contains("CIB", ignoreCase = true) -> "بنك CIB"
            sender.contains("BDC", ignoreCase = true) || message.contains("القاهرة") || sender.contains("BanqueDuCaire", ignoreCase = true) -> "بنك القاهرة"
            sender.contains("ADIB", ignoreCase = true) || message.contains("أبوظبي الإسلامي") || message.contains("ابوظبي الاسلامي") -> "مصرف أبوظبي الإسلامي"
            sender.contains("QNB", ignoreCase = true) -> "بنك QNB الأهلي"
            sender.contains("AlexBank", ignoreCase = true) || message.contains("الإسكندرية") -> "بنك الإسكندرية"
            sender.contains("Faisal", ignoreCase = true) || message.contains("فيصل") -> "بنك فيصل الإسلامي"
            sender.contains("SmartWallet", ignoreCase = true) || message.contains("المحفظة الذكية") -> "المحفظة الذكية CIB"
            sender.contains("InstaPay", ignoreCase = true) -> "إنستاباي"
            sender.contains("Telda", ignoreCase = true) -> "تيلدا"
            sender.contains("Vodafone", ignoreCase = true) -> "فودافون كاش"
            sender.contains("Etisalat", ignoreCase = true) || message.contains("اتصالات كاش") -> "اتصالات كاش"
            sender.contains("WE", ignoreCase = true) || message.contains("وي باي") -> "وي باي (WE Pay)"
            sender.isNotBlank() -> sender
            else -> "معاملة بنكية"
        }
    }

    private fun parseInstaPay(message: String, bankName: String): ParsedBankTransaction? {
        // e.g. "تم تحويل مبلغ 250.00 جم إلى أحمد علي بنجاح"
        val transferOutRegex = Regex("""تم تحويل مبلغ\s*([\d,.]+)\s*(جم|ج\.م|EGP|جنيه)?\s*إلى\s*([^.]+?)(?:\s+بنجاح|\.|\s*$)""")
        val transferMatch = transferOutRegex.find(message)
        if (transferMatch != null) {
            val amount = cleanAmount(transferMatch.groupValues[1])
            val recipient = transferMatch.groupValues[3].trim().removeSuffix("بنجاح").trim()
            return ParsedBankTransaction(
                bankName = "إنستاباي",
                amount = amount,
                currency = "EGP",
                merchant = recipient,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.INSTAPAY,
                rawMessage = message
            )
        }

        // e.g. "تم استلام مبلغ 1500.00 جم من سارة محمد بنجاح"
        val transferInRegex = Regex("""تم استلام مبلغ\s*([\d,.]+)\s*(جم|ج\.م|EGP|جنيه)?\s*من\s*([^.]+?)(?:\s+بنجاح|\.|\s*$)""")
        val receiveMatch = transferInRegex.find(message)
        if (receiveMatch != null) {
            val amount = cleanAmount(receiveMatch.groupValues[1])
            val senderName = receiveMatch.groupValues[3].trim().removeSuffix("بنجاح").trim()
            return ParsedBankTransaction(
                bankName = "إنستاباي",
                amount = amount,
                currency = "EGP",
                merchant = senderName,
                type = TransactionType.INCOME,
                paymentSource = PaymentSource.INSTAPAY,
                rawMessage = message
            )
        }

        // English: "Payment of EGP 340.00 to Carrefour was successful"
        val engPayRegex = Regex("""Payment of\s*(EGP)?\s*([\d,.]+)\s*to\s*([^.]+?)\s*(was successful|via)""", RegexOption.IGNORE_CASE)
        val engMatch = engPayRegex.find(message)
        if (engMatch != null) {
            val amount = cleanAmount(engMatch.groupValues[2])
            val merchant = engMatch.groupValues[3].trim()
            return ParsedBankTransaction(
                bankName = "إنستاباي",
                amount = amount,
                currency = "EGP",
                merchant = merchant,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.INSTAPAY,
                rawMessage = message
            )
        }

        return fallbackParse(message, "إنستاباي", PaymentSource.INSTAPAY)
    }

    private fun parseVodafoneCash(message: String): ParsedBankTransaction? {
        // e.g. "تم تحويل 150 جنيه لرقم 01012345678 بنجاح"
        val transferRegex = Regex("""تم تحويل\s*([\d,.]+)\s*(جنيه|ج|جم)?\s*ل(رقم)?\s*(\d+)""")
        val match = transferRegex.find(message)
        if (match != null) {
            val amount = cleanAmount(match.groupValues[1])
            val recipient = match.groupValues[4]
            return ParsedBankTransaction(
                bankName = "فودافون كاش",
                amount = amount,
                currency = "EGP",
                merchant = "تحويل كاش: $recipient",
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.VODAFONE_CASH,
                rawMessage = message
            )
        }

        // e.g. "تم دفع 80.50 جنيه لـ FAWRY"
        val payRegex = Regex("""تم دفع\s*([\d,.]+)\s*(جنيه|ج|جم)?\s*لـ?\s*([^.]+)""")
        val payMatch = payRegex.find(message)
        if (payMatch != null) {
            val amount = cleanAmount(payMatch.groupValues[1])
            val merchant = payMatch.groupValues[3].trim()
            return ParsedBankTransaction(
                bankName = "فودافون كاش",
                amount = amount,
                currency = "EGP",
                merchant = merchant,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.VODAFONE_CASH,
                rawMessage = message
            )
        }

        return fallbackParse(message, "فودافون كاش", PaymentSource.VODAFONE_CASH)
    }

    private fun parseEtisalatCash(message: String): ParsedBankTransaction? {
        val transferRegex = Regex("""تم تحويل\s*([\d,.]+)\s*(جنيه|ج|جم)?\s*ل(رقم)?\s*(\d+)""")
        val match = transferRegex.find(message)
        if (match != null) {
            val amount = cleanAmount(match.groupValues[1])
            val recipient = match.groupValues[4]
            return ParsedBankTransaction(
                bankName = "اتصالات كاش",
                amount = amount,
                currency = "EGP",
                merchant = "تحويل كاش: $recipient",
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.ETISALAT_CASH,
                rawMessage = message
            )
        }

        val payRegex = Regex("""تم دفع\s*([\d,.]+)\s*(جنيه|ج|جم)?\s*لـ?\s*([^.]+)""")
        val payMatch = payRegex.find(message)
        if (payMatch != null) {
            val amount = cleanAmount(payMatch.groupValues[1])
            val merchant = payMatch.groupValues[3].trim()
            return ParsedBankTransaction(
                bankName = "اتصالات كاش",
                amount = amount,
                currency = "EGP",
                merchant = merchant,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.ETISALAT_CASH,
                rawMessage = message
            )
        }

        return fallbackParse(message, "اتصالات كاش", PaymentSource.ETISALAT_CASH)
    }

    private fun parseWePay(message: String): ParsedBankTransaction? {
        val payRegex = Regex("""تم (خصم|دفع|تحويل)\s*([\d,.]+)\s*(جنيه|ج|جم)?\s*(لـ|إلى|لدى)?\s*([^.]+)""")
        val match = payRegex.find(message)
        if (match != null) {
            val amount = cleanAmount(match.groupValues[2])
            val merchant = match.groupValues[5].trim()
            return ParsedBankTransaction(
                bankName = "وي باي (WE Pay)",
                amount = amount,
                currency = "EGP",
                merchant = merchant,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.WE_PAY,
                rawMessage = message
            )
        }

        return fallbackParse(message, "وي باي (WE Pay)", PaymentSource.WE_PAY)
    }

    private fun parseTelda(message: String): ParsedBankTransaction? {
        // e.g. "You spent EGP 150.00 at CIRCLE K with your Telda card."
        val engRegex = Regex("""spent\s*(EGP)?\s*([\d,.]+)\s*at\s*([^.]+?)\s*(with|using|\.)""", RegexOption.IGNORE_CASE)
        val match = engRegex.find(message)
        if (match != null) {
            val amount = cleanAmount(match.groupValues[2])
            val merchant = match.groupValues[3].trim()
            return ParsedBankTransaction(
                bankName = "تيلدا",
                amount = amount,
                currency = "EGP",
                merchant = merchant,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.TELDA,
                rawMessage = message
            )
        }

        // Arabic: "دفعت 75.00 جنيه عند بلبن"
        val arRegex = Regex("""دفعت\s*([\d,.]+)\s*(جنيه|جم)?\s*(عند|لدى|في)\s*([^.]+)""")
        val arMatch = arRegex.find(message)
        if (arMatch != null) {
            val amount = cleanAmount(arMatch.groupValues[1])
            val merchant = arMatch.groupValues[4].trim()
            return ParsedBankTransaction(
                bankName = "تيلدا",
                amount = amount,
                currency = "EGP",
                merchant = merchant,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.TELDA,
                rawMessage = message
            )
        }

        return fallbackParse(message, "تيلدا", PaymentSource.TELDA)
    }

    private fun parseStandardBankCard(message: String, bankName: String): ParsedBankTransaction? {
        // Arabic pattern: "عملية شراء على بطاقتك بمبلغ 450.50 جم لدى HYPER ONE"
        // or: "حركة خصم بمبلغ 85.00 جم ببطاقتك المنتهية بـ 4321 طرف CHILLOUT"
        val arPurchaseRegex = Regex(
            """(شراء|خصم|سحب)\s*.*?(بمبلغ|مبلغ)?\s*([\d,.]+)\s*(جم|ج\.م|جنيه|EGP)?\s*.*?(لدى|طرف|عند|في)\s*([^\n\r.]+?)(بتاريخ|الرصيد|\.|\s*$)"""
        )
        val arMatch = arPurchaseRegex.find(message)
        if (arMatch != null) {
            val amount = cleanAmount(arMatch.groupValues[3])
            val merchant = arMatch.groupValues[6].trim()
            val cardLast4 = extractCardLast4(message)
            val balance = extractBalance(message)
            return ParsedBankTransaction(
                bankName = bankName,
                amount = amount,
                currency = "EGP",
                merchant = merchant,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.BANK_CARD,
                cardLast4 = cardLast4,
                remainingBalance = balance,
                rawMessage = message
            )
        }

        // English pattern: "Purchase of EGP 89.00 on card ending in 1234 at COSTA COFFEE"
        val enPurchaseRegex = Regex(
            """(Purchase|Debit)\s*.*?(for|of)?\s*(EGP|USD|EUR)?\s*([\d,.]+)\s*.*?(at|with)\s*([^\n\r.]+?)(on|Available|\.|\s*$)""",
            RegexOption.IGNORE_CASE
        )
        val enMatch = enPurchaseRegex.find(message)
        if (enMatch != null) {
            val amount = cleanAmount(enMatch.groupValues[4])
            val merchant = enMatch.groupValues[6].trim()
            val cardLast4 = extractCardLast4(message)
            val balance = extractBalance(message)
            return ParsedBankTransaction(
                bankName = bankName,
                amount = amount,
                currency = enMatch.groupValues[3].ifBlank { "EGP" },
                merchant = merchant,
                type = TransactionType.EXPENSE,
                paymentSource = PaymentSource.BANK_CARD,
                cardLast4 = cardLast4,
                remainingBalance = balance,
                rawMessage = message
            )
        }

        return fallbackParse(message, bankName, PaymentSource.BANK_CARD)
    }

    private fun fallbackParse(message: String, bankName: String, source: PaymentSource): ParsedBankTransaction? {
        val amountRegex = Regex("""([\d,]+(\.\d{1,2})?)\s*(جم|ج\.م|جنيه|EGP|USD)""", RegexOption.IGNORE_CASE)
        val match = amountRegex.find(message)
        if (match != null) {
            val amount = cleanAmount(match.groupValues[1])
            if (amount > 0) {
                return ParsedBankTransaction(
                    bankName = bankName,
                    amount = amount,
                    currency = match.groupValues[3].uppercase().let { if (it.contains("ج")) "EGP" else it },
                    merchant = bankName,
                    type = TransactionType.EXPENSE,
                    paymentSource = source,
                    cardLast4 = extractCardLast4(message),
                    remainingBalance = extractBalance(message),
                    rawMessage = message
                )
            }
        }
        return null
    }

    private fun cleanAmount(raw: String): Double {
        return raw.replace(",", "").toDoubleOrNull() ?: 0.0
    }

    private fun extractCardLast4(message: String): String? {
        val cardRegex = Regex("""(بطاقتك|كارت|card).*?(\d{4})""", RegexOption.IGNORE_CASE)
        return cardRegex.find(message)?.groupValues?.get(2)
    }

    private fun extractBalance(message: String): Double? {
        val balRegex = Regex("""(الرصيد|رصيدك|Avail Bal|Available|Balance).*?([\d,]+(\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val match = balRegex.find(message) ?: return null
        return cleanAmount(match.groupValues[2])
    }
}
