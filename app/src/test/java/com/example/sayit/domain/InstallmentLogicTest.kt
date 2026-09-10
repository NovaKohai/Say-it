package com.example.sayit.domain

import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.InstallmentProvider
import com.example.sayit.domain.model.InstallmentStatus
import com.example.sayit.domain.model.MonthlyInstallmentRecord
import com.example.sayit.domain.model.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentLogicTest {

    @Test
    fun testInstallmentProgressAndRemainingDebt() {
        val records = listOf(
            MonthlyInstallmentRecord(
                id = "1",
                installmentId = "inst_1",
                monthYear = "2026-08",
                dueAmount = 1000.0,
                paidAmount = 1000.0,
                dueDate = 1000000L,
                status = PaymentStatus.PAID
            ),
            MonthlyInstallmentRecord(
                id = "2",
                installmentId = "inst_1",
                monthYear = "2026-09",
                dueAmount = 1000.0,
                paidAmount = 500.0,
                dueDate = 2000000L,
                status = PaymentStatus.PARTIALLY_PAID
            )
        )

        val installment = Installment(
            id = "inst_1",
            name = "iPhone 15",
            provider = InstallmentProvider.VALU,
            totalAmount = 12000.0,
            monthlyAmount = 1000.0,
            startDate = 1000000L,
            endDate = 12000000L,
            totalMonths = 12,
            monthlyRecords = records
        )

        assertEquals(1500.0, installment.totalPaid, 0.01)
        assertEquals(10500.0, installment.remainingTotalDebt, 0.01)
        assertEquals(1500f / 12000f, installment.progressRatio, 0.001f)
    }

    @Test
    fun testMonthlyRecordStatusTransitions() {
        val unpaidRecord = MonthlyInstallmentRecord(
            id = "rec_1",
            installmentId = "inst_1",
            monthYear = "2026-09",
            dueAmount = 1200.0,
            paidAmount = 0.0,
            dueDate = 1000L,
            status = PaymentStatus.UNPAID
        )
        assertFalse(unpaidRecord.isFullyPaid)
        assertEquals(1200.0, unpaidRecord.remainingAmount, 0.01)

        val partialRecord = unpaidRecord.copy(
            paidAmount = 400.0,
            status = PaymentStatus.PARTIALLY_PAID
        )
        assertFalse(partialRecord.isFullyPaid)
        assertEquals(800.0, partialRecord.remainingAmount, 0.01)

        val paidRecord = unpaidRecord.copy(
            paidAmount = 1200.0,
            status = PaymentStatus.PAID
        )
        assertTrue(paidRecord.isFullyPaid)
        assertEquals(0.0, paidRecord.remainingAmount, 0.01)
    }
}
