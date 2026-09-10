package com.example.sayit.data

import com.example.sayit.data.parser.BankMessageParser
import com.example.sayit.domain.model.PaymentSource
import com.example.sayit.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankMessageParserTest {

    @Test
    fun testInstaPayTransferOut() {
        val msg = "تم تحويل مبلغ 250.00 جم إلى أحمد علي بنجاح. الرقم المرجعي: 987654"
        val parsed = BankMessageParser.parse("InstaPay", msg)

        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.001)
        assertEquals("EGP", parsed.currency)
        assertEquals("أحمد علي", parsed.merchant)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(PaymentSource.INSTAPAY, parsed.paymentSource)
    }

    @Test
    fun testInstaPayTransferIn() {
        val msg = "تم استلام مبلغ 1500.00 جم من سارة محمد بنجاح."
        val parsed = BankMessageParser.parse("InstaPay", msg)

        assertNotNull(parsed)
        assertEquals(1500.0, parsed!!.amount, 0.001)
        assertEquals("سارة محمد", parsed.merchant)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(PaymentSource.INSTAPAY, parsed.paymentSource)
    }

    @Test
    fun testNbeCardPurchase() {
        val msg = "تمت عملية شراء على بطاقتك بمبلغ 450.50 جم لدى HYPER ONE بتاريخ 08/09/2026. الرصيد المتاح: 12450.00 جم"
        val parsed = BankMessageParser.parse("NBE", msg)

        assertNotNull(parsed)
        assertEquals(450.50, parsed!!.amount, 0.001)
        assertEquals("HYPER ONE", parsed.merchant)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(PaymentSource.BANK_CARD, parsed.paymentSource)
    }

    @Test
    fun testBanqueMisrDebit() {
        val msg = "حركة خصم بمبلغ 85.00 جم ببطاقتك المنتهية بـ 4321 طرف CHILLOUT. الرصيد المتبقي: 3200 جم"
        val parsed = BankMessageParser.parse("BM", msg)

        assertNotNull(parsed)
        assertEquals(85.0, parsed!!.amount, 0.001)
        assertEquals("CHILLOUT", parsed.merchant)
        assertEquals("4321", parsed.cardLast4)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }

    @Test
    fun testCibPurchaseEnglish() {
        val msg = "Purchase of EGP 89.00 on card ending in 9876 at STARBUCKS. Available limit: EGP 25,000"
        val parsed = BankMessageParser.parse("CIB", msg)

        assertNotNull(parsed)
        assertEquals(89.0, parsed!!.amount, 0.001)
        assertEquals("STARBUCKS", parsed.merchant)
        assertEquals("9876", parsed.cardLast4)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }

    @Test
    fun testVodafoneCashTransfer() {
        val msg = "تم تحويل 150 جنيه لرقم 01012345678 بنجاح. مصاريف الخدمة 1 جنيه. رصيدك الحالي 850 جنيه."
        val parsed = BankMessageParser.parse("VodafoneCash", msg)

        assertNotNull(parsed)
        assertEquals(150.0, parsed!!.amount, 0.001)
        assertEquals(PaymentSource.VODAFONE_CASH, parsed.paymentSource)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }

    @Test
    fun testOtpMessageIsRejected() {
        val otpMsg = "Your OTP is 492810. Do not share this one time password with anyone."
        val parsed = BankMessageParser.parse("CIB", otpMsg)

        assertNull(parsed)
        assertFalse(BankMessageParser.isFinancialMessage(otpMsg))
    }

    @Test
    fun testEtisalatCashTransfer() {
        val msg = "تم تحويل 250 جنيه لرقم 01112345678 بنجاح. رصيدك الحالي 400 جنيه."
        val parsed = BankMessageParser.parse("EtisalatCash", msg)

        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.001)
        assertEquals(PaymentSource.ETISALAT_CASH, parsed.paymentSource)
        assertEquals("اتصالات كاش", parsed.bankName)
    }

    @Test
    fun testBanqueDuCairePurchase() {
        val msg = "شراء بمبلغ 320.00 جم ببطاقتك طرف CARREFOUR. بنك القاهرة"
        val parsed = BankMessageParser.parse("BDC", msg)

        assertNotNull(parsed)
        assertEquals(320.0, parsed!!.amount, 0.001)
        assertEquals("بنك القاهرة", parsed.bankName)
        assertEquals(PaymentSource.BANK_CARD, parsed.paymentSource)
    }
}
