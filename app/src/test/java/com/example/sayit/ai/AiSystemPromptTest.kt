package com.example.sayit.ai

import com.example.sayit.data.ai.AiSystemPrompt
import com.example.sayit.data.ai.LocalFinancialSummary
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSystemPromptTest {

    @Test
    fun testBuildSystemPromptArabic() {
        val summary = LocalFinancialSummary(
            totalExpenseMonth = 3500.0,
            totalIncomeMonth = 12000.0,
            transactionCountMonth = 8,
            monthlyBudget = 10000.0,
            remainingBudget = 6500.0,
            dailyBurn = 250.0,
            topMerchants = emptyList()
        )

        val prompt = AiSystemPrompt.buildSystemPrompt(
            userName = "عمر",
            isEnglishInput = false,
            summary = summary,
            categoryList = listOf("طعام: 1200 ج.م"),
            topMerchantsStr = "كارفور: 800 ج.م",
            installmentList = listOf("قسط لابتوب: 1500 ج.م"),
            recentTxList = listOf("ستاربكس: 150 ج.م")
        )

        assertTrue(prompt.contains("عمر"))
        assertTrue(prompt.contains("10000"))
        assertTrue(prompt.contains("3500"))
        assertTrue(prompt.contains("6500"))
        assertTrue(prompt.contains("كارفور"))
        assertTrue(prompt.contains("كابتن مالي مصري"))
    }

    @Test
    fun testBuildSystemPromptEnglish() {
        val summary = LocalFinancialSummary(
            totalExpenseMonth = 2000.0,
            totalIncomeMonth = 6000.0,
            transactionCountMonth = 4,
            monthlyBudget = 5000.0,
            remainingBudget = 3000.0,
            dailyBurn = 100.0,
            topMerchants = emptyList()
        )

        val prompt = AiSystemPrompt.buildSystemPrompt(
            userName = "Omar",
            isEnglishInput = true,
            summary = summary,
            categoryList = emptyList(),
            topMerchantsStr = "",
            installmentList = emptyList(),
            recentTxList = emptyList()
        )

        assertTrue(prompt.contains("Omar"))
        assertTrue(prompt.contains("Say It Financial Copilot"))
        assertTrue(prompt.contains("5000 EGP"))
        assertTrue(prompt.contains("3000 EGP"))
    }
}
