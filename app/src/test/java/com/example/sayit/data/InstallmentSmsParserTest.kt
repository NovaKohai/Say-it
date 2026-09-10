package com.example.sayit.data

import com.example.sayit.data.parser.InstallmentSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentSmsParserTest {

    @Test
    fun parseValuDueNotice_detectsCorrectAmountAndProvider() {
        val sender = "ValU"
        val message = "عزيزي العميل، قيمة قسطك لشهر سبتمبر هي 1,250 جم ومستحقة في 2026/09/10"

        assertTrue(InstallmentSmsParser.isInstallmentOrDebtMessage(sender, message))

        val result = InstallmentSmsParser.parse(sender, message)
        assertNotNull(result)
        assertEquals("VALU", result!!.provider)
        assertEquals(1250.0, result.amount, 0.01)
        assertTrue(result.isDueNotice)
        assertEquals(false, result.isPaymentConfirmation)
        assertEquals("2026-09", result.monthYearKey)
    }

    @Test
    fun parseValuPaymentConfirmation_detectsPayment() {
        val sender = "ValU"
        val message = "تم سداد قسطك بمبلغ 1,250.00 ج.م بنجاح. شكراً لتعاملك مع فاليو"

        assertTrue(InstallmentSmsParser.isInstallmentOrDebtMessage(sender, message))

        val result = InstallmentSmsParser.parse(sender, message)
        assertNotNull(result)
        assertEquals("VALU", result!!.provider)
        assertEquals(1250.0, result.amount, 0.01)
        assertTrue(result.isPaymentConfirmation)
        assertEquals(false, result.isDueNotice)
    }

    @Test
    fun parseSouhoolaDueNotice_detectsCorrectValues() {
        val sender = "Souhoola"
        val message = "قسط سهولة المستحق لشهر 9 هو 850 جم"

        assertTrue(InstallmentSmsParser.isInstallmentOrDebtMessage(sender, message))

        val result = InstallmentSmsParser.parse(sender, message)
        assertNotNull(result)
        assertEquals("SOUHOOLA", result!!.provider)
        assertEquals(850.0, result.amount, 0.01)
        assertTrue(result.isDueNotice)
    }

    @Test
    fun parseBankCreditCardDebtNotice_detectsCorrectValues() {
        val sender = "CIB"
        val message = "إجمالي المديونية المستحقة على بطاقتكم الائتمانية 4,500 ج.م وتاريخ الاستحقاق 25/09/2026"

        assertTrue(InstallmentSmsParser.isInstallmentOrDebtMessage(sender, message))

        val result = InstallmentSmsParser.parse(sender, message)
        assertNotNull(result)
        assertEquals("CIB", result!!.provider)
        assertEquals(4500.0, result.amount, 0.01)
        assertTrue(result.isDueNotice)
        assertEquals("2026-09", result.monthYearKey)
    }

    @Test
    fun parseBankCreditCardPayment_detectsPayment() {
        val sender = "CIB"
        val message = "تم سداد لبطاقتكم الائتمانية بمبلغ 2000 جم بنجاح"

        assertTrue(InstallmentSmsParser.isInstallmentOrDebtMessage(sender, message))

        val result = InstallmentSmsParser.parse(sender, message)
        assertNotNull(result)
        assertEquals("CIB", result!!.provider)
        assertEquals(2000.0, result.amount, 0.01)
        assertTrue(result.isPaymentConfirmation)
    }
}
