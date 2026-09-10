package com.example.sayit.domain.model

sealed class AppError(
    override val message: String,
    open val messageEn: String = "",
    override val cause: Throwable? = null
) : Exception(message, cause) {

    // Network & Service Status
    data class NetworkUnavailable(
        override val message: String = "لا يتوفر اتصال بالإنترنت حالياً، تم إعداد الرد من بياناتك على الجهاز.",
        override val messageEn: String = "No internet connection available. Prepared response from local data.",
        override val cause: Throwable? = null
    ) : AppError(message, messageEn, cause)

    data class AiRateLimit(
        override val message: String = "الخدمة مشغولة مؤقتاً، تم إعداد الرد من حساباتك المسجلة على الجهاز.",
        override val messageEn: String = "Service is temporarily busy. Responded directly from local data.",
        override val cause: Throwable? = null
    ) : AppError(message, messageEn, cause)

    data class AiAuthError(
        override val message: String = "تعذر الاتصال بالسحابة حالياً، تم الاعتماد على حساباتك المحلية.",
        override val messageEn: String = "Cloud service temporarily unavailable. Responded using local calculations.",
        override val cause: Throwable? = null
    ) : AppError(message, messageEn, cause)

    data class AiTimeout(
        override val message: String = "استغرقت الاستجابة وقتاً طويلاً، تم الاعتماد على الإحصائيات المحلية.",
        override val messageEn: String = "Response took too long. Answered using local statistics.",
        override val cause: Throwable? = null
    ) : AppError(message, messageEn, cause)

    // Database & Data Errors
    data class DatabaseError(
        override val message: String = "حدث خطأ أثناء قراءة أو حفظ البيانات في قاعدة البيانات.",
        override val messageEn: String = "An error occurred while accessing the local database.",
        override val cause: Throwable? = null
    ) : AppError(message, messageEn, cause)

    // Audio & Speech Errors
    data class SpeechRecognitionError(
        val errorCode: Int,
        override val message: String = "تعذر التقاط الصوت، يرجى المحاولة مرة أخرى أو الكتابة.",
        override val messageEn: String = "Could not capture voice clearly. Please try again or type.",
        override val cause: Throwable? = null
    ) : AppError(message, messageEn, cause)

    data class PermissionDenied(
        val permission: String,
        override val message: String = "الإذن المطلوب غير متاح، يرجى تفعيله من الإعدادات.",
        override val messageEn: String = "Permission required is denied. Please enable it in system settings.",
        override val cause: Throwable? = null
    ) : AppError(message, messageEn, cause)

    // General Unexpected Error
    data class Unknown(
        override val message: String = "حدث خطأ غير متوقع، يرجى المحاولة لاحقاً.",
        override val messageEn: String = "An unexpected error occurred. Please try again later.",
        override val cause: Throwable? = null
    ) : AppError(message, messageEn, cause)
}
