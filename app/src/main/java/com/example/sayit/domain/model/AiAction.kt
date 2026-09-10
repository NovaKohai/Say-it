package com.example.sayit.domain.model

enum class NavigationTarget {
    DASHBOARD,
    ANALYTICS,
    INSTALLMENTS,
    BANKS_SYNC,
    SMS_IMPORT,
    BUDGET_EDIT,
    EXPORT_REPORT,
    SETTINGS
}

enum class ActionStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}

sealed class AiAction {
    data class Navigate(
        val target: NavigationTarget,
        val labelAr: String,
        val labelEn: String
    ) : AiAction()

    data class OpenApiKeySetup(
        val labelAr: String = "تفعيل مفتاح الذكاء الاصطناعي (Gemini)",
        val labelEn: String = "Configure Gemini API Key"
    ) : AiAction()

    data class ConfirmTransaction(
        val actionId: String = java.util.UUID.randomUUID().toString(),
        val amount: Double,
        val type: TransactionType = TransactionType.EXPENSE,
        val merchant: String,
        val categoryId: String = "cat_food",
        val categoryName: String = "طعام ومشروبات",
        val paymentSource: String = "CASH",
        val notes: String? = null,
        val status: ActionStatus = ActionStatus.PENDING
    ) : AiAction()

    data class ConfirmDeleteTransaction(
        val actionId: String = java.util.UUID.randomUUID().toString(),
        val transactionId: String,
        val merchant: String,
        val amount: Double,
        val status: ActionStatus = ActionStatus.PENDING
    ) : AiAction()

    data class ShowBudgetSnapshot(
        val monthlyBudget: Double,
        val totalSpent: Double,
        val remaining: Double,
        val burnRate: Double,
        val depletionDate: String
    ) : AiAction()

    data class ShowTopMerchants(
        val merchants: List<MerchantSpend>
    ) : AiAction()

    data class SelectPersona(
        val name: String,
        val label: String
    ) : AiAction()

    data class RetryQuery(
        val prompt: String,
        val labelAr: String = "إعادة المحاولة 🔄",
        val labelEn: String = "Retry 🔄"
    ) : AiAction()
}

data class MerchantSpend(
    val merchantName: String,
    val totalAmount: Double,
    val transactionCount: Int
)
