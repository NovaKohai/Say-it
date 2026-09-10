package com.example.sayit.data.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.parser.InstallmentSmsParser
import com.example.sayit.data.repository.InstallmentRepositoryImpl
import com.example.sayit.data.repository.TransactionRepositoryImpl
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.usecase.ProcessBankMessageUseCase
import com.example.sayit.domain.usecase.ProcessInstallmentSmsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: "Bank"
        val fullBody = messages.joinToString("") { it.messageBody ?: "" }

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val db = SayItDatabase.getInstance(context)

                // 1. Process Installments & Debt Notices if relevant
                if (InstallmentSmsParser.isInstallmentOrDebtMessage(sender, fullBody)) {
                    val instRepo = InstallmentRepositoryImpl(db)
                    val instUseCase = ProcessInstallmentSmsUseCase(instRepo)
                    val parsed = InstallmentSmsParser.parse(sender, fullBody)
                    val processed = instUseCase(sender, fullBody)
                    if (processed && parsed != null) {
                        val title = if (parsed.isPaymentConfirmation) "تم تسجيل سداد قسط" else "تم رصد استحقاق قسط جديد"
                        val body = if (parsed.isPaymentConfirmation) {
                            "تم تحديث قسط ${parsed.provider} وسداد ${parsed.amount.toInt()} ج.م"
                        } else {
                            "تم رصد قسط مستحق لـ ${parsed.provider} بمبلغ ${parsed.amount.toInt()} ج.م"
                        }
                        showCustomNotification(context, title, body, 1001)
                    }
                }

                // 2. Process Standard Financial Transactions
                val repo = TransactionRepositoryImpl(db)
                val processUseCase = ProcessBankMessageUseCase(repo)

                val savedTx = processUseCase(sender, fullBody, TransactionSource.SMS)
                if (savedTx != null) {
                    showCustomNotification(
                        context,
                        "تم تسجيل معاملة جديدة",
                        "تم تسجيل ${savedTx.amount.toInt()} ${savedTx.currency} في ${savedTx.merchant} تلقائياً",
                        (System.currentTimeMillis() % 100000).toInt()
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showCustomNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int
    ) {
        val channelId = "sayit_tx_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "المصاريف والأقساط المسجلة تلقائياً",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات عند رصد وتسجيل رسائل البنوك والأقساط وإنستاباي"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
