package com.example.sayit.data.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.repository.TransactionRepositoryImpl
import com.example.sayit.domain.model.TransactionSource
import com.example.sayit.domain.usecase.ProcessBankMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SayItNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val combinedMessage = if (bigText.isNotBlank()) "$title: $bigText" else "$title: $text"
        if (combinedMessage.isBlank()) return

        serviceScope.launch {
            val db = SayItDatabase.getInstance(applicationContext)
            val repo = TransactionRepositoryImpl(db)
            val processUseCase = ProcessBankMessageUseCase(repo)

            // Pass package name as sender identifier
            processUseCase(packageName, combinedMessage, TransactionSource.NOTIFICATION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
