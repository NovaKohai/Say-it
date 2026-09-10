package com.example.sayit.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class PaymentSource(val titleAr: String, val titleEn: String) {
    CASH("كاش", "Cash"),
    BANK_CARD("بطاقة بنكية", "Bank Card"),
    INSTAPAY("إنستاباي", "InstaPay"),
    VODAFONE_CASH("فودافون كاش", "Vodafone Cash"),
    ORANGE_CASH("أورنج كاش", "Orange Cash"),
    ETISALAT_CASH("اتصالات كاش", "Etisalat Cash"),
    WE_PAY("وي باي", "WE Pay"),
    TELDA("تيلدا", "Telda"),
    OTHER("أخرى", "Other")
}

enum class TransactionSource(val labelAr: String, val labelEn: String) {
    VOICE("صوتي", "Voice"),
    SMS("رسالة بنكية", "Bank SMS"),
    NOTIFICATION("إشعار محفظة/بنك", "App Notification"),
    MANUAL("يدوي", "Manual")
}
