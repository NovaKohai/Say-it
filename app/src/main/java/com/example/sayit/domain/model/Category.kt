package com.example.sayit.domain.model

data class Category(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val iconName: String,
    val colorHex: Long,
    val monthlyBudget: Double? = null
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category(
                id = "cat_food",
                nameAr = "طعام ومشروبات",
                nameEn = "Food & Dining",
                iconName = "Restaurant",
                colorHex = 0xFFFF6F00 // Orange
            ),
            Category(
                id = "cat_groceries",
                nameAr = "بقالة وسوبرماركت",
                nameEn = "Groceries",
                iconName = "ShoppingCart",
                colorHex = 0xFF2E7D32 // Green
            ),
            Category(
                id = "cat_transport",
                nameAr = "مواصلات وبنزين",
                nameEn = "Transport & Fuel",
                iconName = "DirectionsCar",
                colorHex = 0xFF0288D1 // Light Blue
            ),
            Category(
                id = "cat_bills",
                nameAr = "فواتير والتزامات",
                nameEn = "Bills & Utilities",
                iconName = "ReceiptLong",
                colorHex = 0xFFD32F2F // Red
            ),
            Category(
                id = "cat_shopping",
                nameAr = "تسوق وملابس",
                nameEn = "Shopping",
                iconName = "ShoppingBag",
                colorHex = 0xFF7B1FA2 // Purple
            ),
            Category(
                id = "cat_health",
                nameAr = "صحة وأدوية",
                nameEn = "Healthcare",
                iconName = "LocalHospital",
                colorHex = 0xFF00796B // Teal
            ),
            Category(
                id = "cat_entertainment",
                nameAr = "ترفيه وخروجات",
                nameEn = "Entertainment",
                iconName = "SportsEsports",
                colorHex = 0xFFC2185B // Pink
            ),
            Category(
                id = "cat_salary",
                nameAr = "راتب ودخل",
                nameEn = "Salary & Income",
                iconName = "Payments",
                colorHex = 0xFF388E3C // Emerald
            ),
            Category(
                id = "cat_other",
                nameAr = "مصاريف أخرى",
                nameEn = "General & Other",
                iconName = "Category",
                colorHex = 0xFF616161 // Grey
            )
        )

        fun findDefault(id: String): Category {
            return DEFAULT_CATEGORIES.find { it.id == id } ?: DEFAULT_CATEGORIES.last()
        }
    }
}
