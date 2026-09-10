package com.example.sayit.data

import com.example.sayit.data.parser.VoiceTextParser
import com.example.sayit.domain.model.PaymentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTextParserTest {

    @Test
    fun testEgyptianDialectExpense() {
        val text = "صرفت 120 جنيه في بلبن"
        val parsed = VoiceTextParser.parse(text)

        assertEquals(120.0, parsed.amount, 0.001)
        assertEquals("EGP", parsed.currency)
        assertTrue(parsed.merchant.contains("بلبن"))
        assertEquals("cat_food", parsed.categoryId)
    }

    @Test
    fun testNumberWordsParsing() {
        val text = "دفعت ميتين بنزين كاش"
        val parsed = VoiceTextParser.parse(text)

        assertEquals(200.0, parsed.amount, 0.001)
        assertEquals("cat_transport", parsed.categoryId)
        assertEquals(PaymentSource.CASH, parsed.paymentSource)
    }

    @Test
    fun testCompoundNumberWords() {
        val text = "خمسة وعشرين قهوة"
        val parsed = VoiceTextParser.parse(text)

        assertEquals(25.0, parsed.amount, 0.001)
        assertEquals("cat_food", parsed.categoryId)
    }

    @Test
    fun testEnglishSpeechInput() {
        val text = "Track my 15 dollar lunch"
        val parsed = VoiceTextParser.parse(text)

        assertEquals(15.0, parsed.amount, 0.001)
        assertEquals("USD", parsed.currency)
        assertEquals("cat_food", parsed.categoryId)
    }

    @Test
    fun testRentAndBillsCategory() {
        val text = "ألف ونص إيجار الشقة"
        val parsed = VoiceTextParser.parse(text)

        assertEquals(1500.0, parsed.amount, 0.001)
        assertEquals("cat_bills", parsed.categoryId)
    }

    @Test
    fun testEgyptianSlangBako() {
        val text = "دفعت باكو في زارا بالفيزا"
        val parsed = VoiceTextParser.parse(text)

        assertEquals(1000.0, parsed.amount, 0.001)
        assertEquals("cat_shopping", parsed.categoryId)
        assertEquals(PaymentSource.BANK_CARD, parsed.paymentSource)
    }

    @Test
    fun testEtisalatCashVoice() {
        val text = "شحنت رصيد بمية اتصالات كاش"
        val parsed = VoiceTextParser.parse(text)

        assertEquals(100.0, parsed.amount, 0.001)
        assertEquals("cat_bills", parsed.categoryId)
        assertEquals(PaymentSource.ETISALAT_CASH, parsed.paymentSource)
    }
}
