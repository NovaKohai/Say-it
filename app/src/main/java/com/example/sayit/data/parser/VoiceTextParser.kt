package com.example.sayit.data.parser

import com.example.sayit.domain.model.ParsedVoiceTransaction
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.TransactionType

object VoiceTextParser {

    private val numberWordsMap = mapOf(
        "واحد" to 1.0,
        "اتنين" to 2.0,
        "تلاتة" to 3.0,
        "ثلاثة" to 3.0,
        "اربعة" to 4.0,
        "أربعة" to 4.0,
        "خمسة" to 5.0,
        "ستة" to 6.0,
        "سبعة" to 7.0,
        "تمانية" to 8.0,
        "ثمانية" to 8.0,
        "تسعة" to 9.0,
        "عشرة" to 10.0,
        "عشرين" to 20.0,
        "تلاتين" to 30.0,
        "ثلاثين" to 30.0,
        "اربعين" to 40.0,
        "أربعين" to 40.0,
        "خمسين" to 50.0,
        "ستين" to 60.0,
        "سبعين" to 70.0,
        "تمانين" to 80.0,
        "ثمانين" to 80.0,
        "تسعين" to 90.0,
        "مية" to 100.0,
        "مائة" to 100.0,
        "ميتين" to 200.0,
        "مائتين" to 200.0,
        "تلتومية" to 300.0,
        "ثلاثمائة" to 300.0,
        "اربعمية" to 400.0,
        "أربعمائة" to 400.0,
        "خمسمية" to 500.0,
        "خمسمائة" to 500.0,
        "ستمية" to 600.0,
        "سبعمية" to 700.0,
        "تمنمية" to 800.0,
        "تسعمية" to 900.0,
        "ألف" to 1000.0,
        "الف" to 1000.0,
        "ألفين" to 2000.0,
        "الفين" to 2000.0,
        "ألف ونص" to 1500.0,
        "الف ونص" to 1500.0,
        "باكو" to 1000.0,
        "باكويين" to 2000.0,
        "باكوين" to 2000.0,
        "نص باكو" to 500.0,
        "ربع باكو" to 250.0,
        "تلات بواكي" to 3000.0,
        "أربع بواكي" to 4000.0,
        "اربع بواكي" to 4000.0,
        "خمس بواكي" to 5000.0,
        "عشر بواكي" to 10000.0,
        "أرنب" to 1000000.0,
        "ارنب" to 1000000.0,
        "نص أرنب" to 500000.0,
        "نص ارنب" to 500000.0
    )

    private val categoryKeywordMap = mapOf(
        "cat_food" to listOf(
            "غدا", "فطار", "عشا", "قهوة", "شاي", "كافيه", "مطعم", "كشري", "برجر",
            "بيتزا", "شاورما", "أكل", "اكل", "ماكدونالدز", "كنتاكي", "بلبن", "لابوار",
            "ايتوال", "ابن الشام", "كشري التحرير", "توم اند بصل", "بازوكا", "حواوشي",
            "lunch", "dinner", "breakfast", "coffee", "tea", "burger", "pizza", "food"
        ),
        "cat_groceries" to listOf(
            "سوبرماركت", "كارفور", "بيم", "خير زمان", "سعودي", "طلبات مارت", "بقالة", "لبن", "جبنة", "بيض", "خضار", "فاكهة",
            "سيركل كي", "كازيون", "لولو", "اولاد رجب",
            "carrefour", "bim", "spinneys", "hyperone", "groceries", "supermarket"
        ),
        "cat_transport" to listOf(
            "بنزين", "مواصلات", "مترو", "تاكسي", "أوبر", "اوبر", "كريم", "ديدي", "ان درايف", "قطار", "كارته",
            "uber", "careem", "gas", "fuel", "taxi", "transport", "metro"
        ),
        "cat_bills" to listOf(
            "فاتورة", "كهرباء", "مياه", "غاز", "نت", "واي فاي", "ايجار", "إيجار", "تجديد باقة", "شحن رصيد", "اقساط", "قسط",
            "اتصالات", "أورنج", "اورنج", "وي", "we", "فودافون", "فوري", "امان",
            "bill", "rent", "internet", "electricity", "water"
        ),
        "cat_health" to listOf(
            "صيدلية", "دكتور", "علاج", "دوا", "دواء", "تحاليل", "مستشفى", "اسنان", "أسنان",
            "العزبي", "سيف", "19011", "رشدي",
            "pharmacy", "doctor", "medicine", "hospital"
        ),
        "cat_shopping" to listOf(
            "هدوم", "ملابس", "شوز", "جزمة", "قميص", "بنطلون", "ساعة", "أمازون", "امازون", "نون", "زارا",
            "clothes", "shoes", "shopping", "amazon", "noon", "zara"
        ),
        "cat_entertainment" to listOf(
            "سينما", "بلايستيشن", "جيم", "حلاقة", "صالون", "فسحة", "سفر", "مصيف",
            "cinema", "movies", "gym", "game", "trip"
        )
    )

    fun parse(text: String): ParsedVoiceTransaction {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()

        // 1. Extract Amount
        val amount = extractAmount(lower) ?: 0.0

        // 2. Extract Currency
        val currency = when {
            lower.contains("dollar") || lower.contains("دولار") || lower.contains("$") -> "USD"
            lower.contains("ريال") || lower.contains("sar") -> "SAR"
            lower.contains("درهم") || lower.contains("aed") -> "AED"
            lower.contains("يورو") || lower.contains("euro") || lower.contains("€") -> "EUR"
            else -> "EGP"
        }

        // 3. Extract Payment Source
        val paymentSource = when {
            lower.contains("فيزا") || lower.contains("البطاقة") || lower.contains("كارت") || lower.contains("card") || lower.contains("visa") -> PaymentSource.BANK_CARD
            lower.contains("إنستاباي") || lower.contains("انستاباي") || lower.contains("instapay") -> PaymentSource.INSTAPAY
            lower.contains("فودافون كاش") || lower.contains("فودافون") || lower.contains("vodafone") -> PaymentSource.VODAFONE_CASH
            lower.contains("اتصالات كاش") || lower.contains("اتصالات") || lower.contains("etisalat") -> PaymentSource.ETISALAT_CASH
            lower.contains("وي باي") || lower.contains("وي كاش") || lower.contains("we pay") -> PaymentSource.WE_PAY
            lower.contains("تيلدا") || lower.contains("telda") -> PaymentSource.TELDA
            lower.contains("أورنج كاش") || lower.contains("اورنج كاش") -> PaymentSource.ORANGE_CASH
            lower.contains("كاش") || lower.contains("نقدي") || lower.contains("cash") -> PaymentSource.CASH
            else -> PaymentSource.CASH
        }

        // 4. Extract Category
        val categoryId = matchCategory(lower)

        // 5. Extract Merchant or Description
        val merchant = extractMerchant(trimmed, amount)

        return ParsedVoiceTransaction(
            amount = amount,
            currency = currency,
            merchant = merchant,
            categoryId = categoryId,
            paymentSource = paymentSource,
            type = TransactionType.EXPENSE,
            rawSpeech = text
        )
    }

    private fun extractAmount(text: String): Double? {
        // Look for digit numbers first: e.g. 150, 45.5, 300
        val digitRegex = Regex("""(\d+(\.\d+)?)""")
        val digitMatch = digitRegex.find(text)
        if (digitMatch != null) {
            return digitMatch.value.toDoubleOrNull()
        }

        // Look for combined compound word numbers: e.g. "خمسة وعشرين" (25), "مية وخمسين" (150)
        val compoundPattern = Regex("""(خمسة|ستة|سبعة|تمانية|تسعة|أربعة|تلاتة|واحد|اتنين)\s+و(عشرين|تلاتين|اربعين|خمسين|ستين|سبعين|تمانين|تسعين)""")
        val compoundMatch = compoundPattern.find(text)
        if (compoundMatch != null) {
            val unit = numberWordsMap[compoundMatch.groupValues[1]] ?: 0.0
            val tens = numberWordsMap[compoundMatch.groupValues[2]] ?: 0.0
            return unit + tens
        }

        // Sort keys by descending length so "ألف ونص" is checked before "ألف"
        for (word in numberWordsMap.keys.sortedByDescending { it.length }) {
            if (text.contains(word)) {
                return numberWordsMap[word]
            }
        }
        return null
    }

    private fun matchCategory(text: String): String {
        for ((catId, keywords) in categoryKeywordMap) {
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    return catId
                }
            }
        }
        return "cat_other"
    }

    private fun extractMerchant(text: String, amount: Double): String {
        // Strip out common verbs and fillers
        var cleaned = text
            .replace(Regex("""(?i)\b(track my|track|log|spent|bought|paid|egp|usd|dollar|dollars)\b"""), "")
            .replace(Regex("""(سجلت|سجل|صرفت|دفعت|اشتريت|جبت|شحنت|حاسبت|حولت|سحبت|في|من|عند|لدى|جنيه|جنية|ج|دولار|كاش|بالفيزا|بالكارت)\s*"""), "")
            .replace(Regex("""\d+(\.\d+)?"""), "")
            .trim()

        for (word in numberWordsMap.keys) {
            cleaned = cleaned.replace(word, "").trim()
        }

        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()

        return if (cleaned.isNotBlank()) {
            cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            "مصروف عام"
        }
    }
}
