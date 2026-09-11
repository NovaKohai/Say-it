package com.example.sayit.core

import com.example.sayit.core.localization.*
import org.junit.Assert.*
import org.junit.Test

class ThemeLocalizationRegressionTest {
    @Test fun languageDoesNotDependOnCurrency() {
        assertFalse(EnglishStrings.copy(currency = "ج.م").isArabic)
        assertTrue(ArabicStrings.copy(currency = "EGP").isArabic)
    }

    @Test fun localizedFormatsHaveMatchingArguments() {
        val pattern = Regex("%(?:[0-9]+\\$)?[sd]")
        for (field in AppStrings::class.java.declaredFields.filter { it.type == String::class.java }) {
            field.isAccessible = true
            val ar = field.get(ArabicStrings) as String
            val en = field.get(EnglishStrings) as String
            assertTrue(field.name, ar.isNotBlank() && en.isNotBlank())
            assertEquals(field.name, pattern.findAll(ar).map { it.value }.toList(), pattern.findAll(en).map { it.value }.toList())
        }
    }
}
