package com.example.sayit.data.repository

import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.local.entity.InstallmentRecordEntity
import com.example.sayit.data.local.mapper.toDomain
import com.example.sayit.data.local.mapper.toEntity
import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.InstallmentPayoffForecast
import com.example.sayit.domain.model.InstallmentProvider
import com.example.sayit.domain.model.InstallmentStatus
import com.example.sayit.domain.model.MonthForecastPoint
import com.example.sayit.domain.model.MonthlyInstallmentRecord
import com.example.sayit.domain.model.PaymentStatus
import com.example.sayit.domain.repository.InstallmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class InstallmentRepositoryImpl(
    private val database: SayItDatabase
) : InstallmentRepository {

    override fun observeInstallments(): Flow<List<Installment>> {
        return combine(
            database.observeInstallments(),
            database.observeAllRecords()
        ) { instEntities, recordEntities ->
            val recordsByInstId = recordEntities
                .map { it.toDomain() }
                .groupBy { it.installmentId }

            instEntities.map { entity ->
                val records = recordsByInstId[entity.id]?.sortedBy { it.dueDate } ?: emptyList()
                entity.toDomain(records)
            }
        }
    }

    override fun observeForecast(): Flow<InstallmentPayoffForecast> {
        return observeInstallments().combine(database.observeAllRecords()) { installments, _ ->
            calculateForecast(installments)
        }
    }

    override suspend fun getInstallmentById(id: String): Installment? = withContext(Dispatchers.IO) {
        val entity = database.getInstallmentById(id) ?: return@withContext null
        val records = database.getRecordsForInstallment(id).map { it.toDomain() }.sortedBy { it.dueDate }
        entity.toDomain(records)
    }

    override suspend fun saveInstallment(installment: Installment) = withContext(Dispatchers.IO) {
        database.insertInstallment(installment.toEntity())
        generateScheduleIfEmpty(installment)
    }

    override suspend fun updateInstallment(installment: Installment) = withContext(Dispatchers.IO) {
        database.updateInstallment(installment.toEntity())
    }

    override suspend fun deleteInstallment(id: String) = withContext(Dispatchers.IO) {
        database.deleteInstallment(id)
    }

    override suspend fun recordPayment(
        installmentId: String,
        monthYear: String,
        paidAmount: Double,
        paidAt: Long,
        linkedTransactionId: String?
    ): MonthlyInstallmentRecord? = withContext(Dispatchers.IO) {
        val inst = database.getInstallmentById(installmentId) ?: return@withContext null
        val existing = database.getRecordByMonth(installmentId, monthYear)

        val totalPaidForMonth = paidAmount.coerceAtLeast(0.0)
        val dueAmount = existing?.dueAmount ?: inst.monthlyAmount
        val newStatus = when {
            totalPaidForMonth >= dueAmount && dueAmount > 0 -> PaymentStatus.PAID
            totalPaidForMonth > 0 -> PaymentStatus.PARTIALLY_PAID
            else -> PaymentStatus.UNPAID
        }

        val updatedRecord = if (existing != null) {
            existing.copy(
                paidAmount = totalPaidForMonth,
                status = newStatus.name,
                paidAt = if (totalPaidForMonth > 0) paidAt else null,
                linkedTransactionId = linkedTransactionId ?: existing.linkedTransactionId
            )
        } else {
            val dueDate = computeDueDate(monthYear, inst.dueDayOfMonth)
            InstallmentRecordEntity(
                id = UUID.randomUUID().toString(),
                installmentId = installmentId,
                monthYear = monthYear,
                dueAmount = dueAmount,
                paidAmount = totalPaidForMonth,
                dueDate = dueDate,
                status = newStatus.name,
                paidAt = if (totalPaidForMonth > 0) paidAt else null,
                linkedTransactionId = linkedTransactionId
            )
        }

        if (existing != null) {
            database.updateRecord(updatedRecord)
        } else {
            database.insertRecord(updatedRecord)
        }

        // Check if installment is completely paid off
        checkAndMarkCompletion(installmentId)

        updatedRecord.toDomain()
    }

    override suspend fun recordDueNotice(
        providerOrName: String,
        dueAmount: Double,
        monthYear: String?,
        dueTimestamp: Long?
    ): Boolean = withContext(Dispatchers.IO) {
        val targetMonthKey = monthYear ?: run {
            val cal = Calendar.getInstance()
            String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }

        val target = findMatchingInstallment(providerOrName) ?: return@withContext false

        val existing = database.getRecordByMonth(target.id, targetMonthKey)
        val dueDate = dueTimestamp ?: computeDueDate(targetMonthKey, target.dueDayOfMonth)

        if (existing != null) {
            val updated = existing.copy(
                dueAmount = dueAmount,
                dueDate = dueDate
            )
            database.insertRecord(updated)
        } else {
            val newRecord = InstallmentRecordEntity(
                id = UUID.randomUUID().toString(),
                installmentId = target.id,
                monthYear = targetMonthKey,
                dueAmount = dueAmount,
                paidAmount = 0.0,
                dueDate = dueDate,
                status = PaymentStatus.UNPAID.name,
                paidAt = null,
                linkedTransactionId = null
            )
            database.insertRecord(newRecord)
        }
        true
    }

    override suspend fun recordSmsPayment(
        providerOrName: String,
        paidAmount: Double,
        timestamp: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val target = findMatchingInstallment(providerOrName) ?: return@withContext false
        val records = database.getRecordsForInstallment(target.id)

        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val currentMonthKey = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

        // Find record for current month first
        val currentMonthRecord = records.firstOrNull { it.monthYear == currentMonthKey }

        // Find first unpaid or partially paid record if no current month record
        val targetRecord = currentMonthRecord
            ?: records.firstOrNull { it.status != PaymentStatus.PAID.name }

        val monthKey = if (targetRecord != null) targetRecord.monthYear else currentMonthKey
        val existingPaid = targetRecord?.paidAmount ?: 0.0

        // Only add if this isn't already fully paid for this month
        val newTotalPaid = if (targetRecord != null && targetRecord.status == PaymentStatus.PAID.name && existingPaid >= targetRecord.dueAmount) {
            existingPaid // Already fully paid, don't add more
        } else {
            existingPaid + paidAmount
        }

        val newStatus = when {
            newTotalPaid >= (targetRecord?.dueAmount ?: 0.0) && (targetRecord?.dueAmount ?: 0.0) > 0 -> PaymentStatus.PAID
            newTotalPaid > 0 -> PaymentStatus.PARTIALLY_PAID
            else -> PaymentStatus.UNPAID
        }

        val updatedRecord = if (targetRecord != null) {
            targetRecord.copy(
                paidAmount = newTotalPaid,
                status = newStatus.name,
                paidAt = if (newTotalPaid > 0) timestamp else null,
                linkedTransactionId = targetRecord.linkedTransactionId
            )
        } else {
            val dueDate = computeDueDate(monthKey, target.dueDayOfMonth)
            InstallmentRecordEntity(
                id = UUID.randomUUID().toString(),
                installmentId = target.id,
                monthYear = monthKey,
                dueAmount = target.monthlyAmount,
                paidAmount = newTotalPaid,
                dueDate = dueDate,
                status = newStatus.name,
                paidAt = if (newTotalPaid > 0) timestamp else null,
                linkedTransactionId = null
            )
        }

        if (targetRecord != null) {
            database.updateRecord(updatedRecord)
        } else {
            database.insertRecord(updatedRecord)
        }

        true
    }

    private suspend fun checkAndMarkCompletion(installmentId: String) {
        val inst = database.getInstallmentById(installmentId) ?: return
        val records = database.getRecordsForInstallment(installmentId)
        val totalPaid = records.sumOf { it.paidAmount }

        if (totalPaid >= inst.totalAmount && inst.totalAmount > 0) {
            database.updateInstallment(inst.copy(status = InstallmentStatus.COMPLETED.name))
        }
    }

    private fun findMatchingInstallment(providerOrName: String): com.example.sayit.data.local.entity.InstallmentEntity? {
        val all = database.getAllInstallments().filter { it.status == InstallmentStatus.ACTIVE.name }
        val providerEnum = InstallmentProvider.fromString(providerOrName)

        return all.firstOrNull { it.provider.equals(providerEnum.name, ignoreCase = true) }
            ?: all.firstOrNull { it.name.contains(providerOrName, ignoreCase = true) || providerOrName.contains(it.name, ignoreCase = true) }
    }

    private suspend fun generateScheduleIfEmpty(installment: Installment) {
        val existing = database.getRecordsForInstallment(installment.id)
        if (existing.isNotEmpty()) return

        val count = installment.totalMonths.coerceIn(1, 120)
        val records = mutableListOf<InstallmentRecordEntity>()

        val cal = Calendar.getInstance().apply {
            timeInMillis = installment.startDate
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        for (i in 0 until count) {
            cal.set(Calendar.DAY_OF_MONTH, installment.dueDayOfMonth.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            val monthKey = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            records.add(
                InstallmentRecordEntity(
                    id = UUID.randomUUID().toString(),
                    installmentId = installment.id,
                    monthYear = monthKey,
                    dueAmount = installment.monthlyAmount,
                    paidAmount = 0.0,
                    dueDate = cal.timeInMillis,
                    status = PaymentStatus.UNPAID.name,
                    paidAt = null,
                    linkedTransactionId = null
                )
            )
            cal.add(Calendar.MONTH, 1)
        }

        database.insertRecords(records)
    }

    private fun computeDueDate(monthYear: String, dueDay: Int): Long {
        val parts = monthYear.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
        val cal = Calendar.getInstance().apply {
            set(year, month, dueDay.coerceIn(1, maxDayOfMonth(year, month + 1)), 12, 0, 0)
        }
        return cal.timeInMillis
    }
    
    private fun maxDayOfMonth(year: Int, month: Int): Int {
        return when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    private fun calculateForecast(installments: List<Installment>): InstallmentPayoffForecast {
        val active = installments.filter { it.status == InstallmentStatus.ACTIVE }

        val curCal = Calendar.getInstance()
        val curMonthKey = String.format(Locale.US, "%04d-%02d", curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH) + 1)

        val totalOriginalDebt = active.sumOf { it.totalAmount }
        val totalPaidSoFar = active.sumOf { it.totalPaid }
        val totalRemainingDebt = (totalOriginalDebt - totalPaidSoFar).coerceAtLeast(0.0)

        var curMonthDues = 0.0
        var curMonthPaid = 0.0

        for (inst in active) {
            val rec = inst.monthlyRecords.firstOrNull { it.monthYear == curMonthKey }
            if (rec != null) {
                curMonthDues += rec.dueAmount
                curMonthPaid += rec.paidAmount
            } else {
                curMonthDues += inst.monthlyAmount
            }
        }

        val curMonthRemaining = (curMonthDues - curMonthPaid).coerceAtLeast(0.0)
        val isSettled = curMonthRemaining <= 0 && curMonthDues > 0

        val maxEndDate = active.map { it.endDate }.maxOrNull()

        // Build monthly projection for the payoff graph (up to 18 future months or until debt is 0)
        val projection = mutableListOf<MonthForecastPoint>()
        var runningDebt = totalRemainingDebt

        val projCal = Calendar.getInstance()
        val monthNamesAr = arrayOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

        // First point: current state
        projection.add(
            MonthForecastPoint(
                monthLabel = "${monthNamesAr[projCal.get(Calendar.MONTH)]} (الآن)",
                monthYearKey = curMonthKey,
                remainingDebt = runningDebt,
                monthlyPaymentDue = curMonthDues
            )
        )

        for (step in 1..18) {
            projCal.add(Calendar.MONTH, 1)
            val stepMonthKey = String.format(Locale.US, "%04d-%02d", projCal.get(Calendar.YEAR), projCal.get(Calendar.MONTH) + 1)
            val stepMonthLabel = monthNamesAr[projCal.get(Calendar.MONTH)]

            // Sum active payments for this future month
            val stepDues = active.filter { it.endDate >= projCal.timeInMillis }.sumOf { it.monthlyAmount }
            runningDebt = (runningDebt - stepDues).coerceAtLeast(0.0)

            projection.add(
                MonthForecastPoint(
                    monthLabel = stepMonthLabel,
                    monthYearKey = stepMonthKey,
                    remainingDebt = runningDebt,
                    monthlyPaymentDue = stepDues
                )
            )

            if (runningDebt <= 0) break
        }

        return InstallmentPayoffForecast(
            totalOriginalDebt = totalOriginalDebt,
            totalRemainingDebt = totalRemainingDebt,
            totalPaidSoFar = totalPaidSoFar,
            currentMonthDues = curMonthDues,
            currentMonthPaid = curMonthPaid,
            currentMonthRemaining = curMonthRemaining,
            isCurrentMonthFullySettled = isSettled,
            activeInstallmentsCount = active.size,
            estimatedPayoffDate = maxEndDate,
            monthlyProjection = projection
        )
    }
}
