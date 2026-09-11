package com.example.sayit.presentation.installments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.repository.InstallmentRepositoryImpl
import com.example.sayit.domain.model.Installment
import com.example.sayit.domain.model.InstallmentPayoffForecast
import com.example.sayit.domain.model.InstallmentStatus
import com.example.sayit.domain.usecase.DeleteInstallmentUseCase
import com.example.sayit.domain.usecase.GetInstallmentForecastUseCase
import com.example.sayit.domain.usecase.GetInstallmentsUseCase
import com.example.sayit.domain.usecase.RecordInstallmentPaymentUseCase
import com.example.sayit.domain.usecase.SaveInstallmentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InstallmentFilter(val labelAr: String, val labelEn: String) {
    ALL("الكل", "All"),
    ACTIVE("النشطة", "Active"),
    COMPLETED("المسددة بالكامل", "Completed")
}

enum class InstallmentFeedback { ADDED, PAYMENT_RECORDED, DELETED }

data class InstallmentsUiState(
    val installments: List<Installment> = emptyList(),
    val filteredInstallments: List<Installment> = emptyList(),
    val forecast: InstallmentPayoffForecast = InstallmentPayoffForecast(
        totalOriginalDebt = 0.0,
        totalRemainingDebt = 0.0,
        totalPaidSoFar = 0.0,
        currentMonthDues = 0.0,
        currentMonthPaid = 0.0,
        currentMonthRemaining = 0.0,
        isCurrentMonthFullySettled = false,
        activeInstallmentsCount = 0,
        estimatedPayoffDate = null,
        monthlyProjection = emptyList()
    ),
    val activeFilter: InstallmentFilter = InstallmentFilter.ACTIVE,
    val isLoading: Boolean = false,
    val feedbackMessage: InstallmentFeedback? = null
)

class InstallmentsViewModel(
    private val getInstallmentsUseCase: GetInstallmentsUseCase,
    private val getInstallmentForecastUseCase: GetInstallmentForecastUseCase,
    private val saveInstallmentUseCase: SaveInstallmentUseCase,
    private val recordInstallmentPaymentUseCase: RecordInstallmentPaymentUseCase,
    private val deleteInstallmentUseCase: DeleteInstallmentUseCase
) : ViewModel() {

    var isAddDialogOpen by androidx.compose.runtime.mutableStateOf(false)
    var payingInstallment by androidx.compose.runtime.mutableStateOf<Installment?>(null)
    var deletingInstallmentId by androidx.compose.runtime.mutableStateOf<String?>(null)

    private val _activeFilter = MutableStateFlow(InstallmentFilter.ACTIVE)
    private val _feedbackMessage = MutableStateFlow<InstallmentFeedback?>(null)

    val uiState: StateFlow<InstallmentsUiState> = combine(
        getInstallmentsUseCase(),
        getInstallmentForecastUseCase(),
        _activeFilter,
        _feedbackMessage
    ) { allInstallments, forecast, filter, message ->
        val filtered = when (filter) {
            InstallmentFilter.ALL -> allInstallments
            InstallmentFilter.ACTIVE -> allInstallments.filter { it.status == InstallmentStatus.ACTIVE }
            InstallmentFilter.COMPLETED -> allInstallments.filter { it.status == InstallmentStatus.COMPLETED }
        }

        InstallmentsUiState(
            installments = allInstallments,
            filteredInstallments = filtered,
            forecast = forecast,
            activeFilter = filter,
            isLoading = false,
            feedbackMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InstallmentsUiState(isLoading = true)
    )

    fun setFilter(filter: InstallmentFilter) {
        _activeFilter.value = filter
    }

    fun addInstallment(installment: Installment) {
        viewModelScope.launch {
            saveInstallmentUseCase(installment)
            _feedbackMessage.value = InstallmentFeedback.ADDED
        }
    }

    fun recordPayment(installmentId: String, monthYear: String, paidAmount: Double) {
        viewModelScope.launch {
            recordInstallmentPaymentUseCase(installmentId, monthYear, paidAmount)
            _feedbackMessage.value = InstallmentFeedback.PAYMENT_RECORDED
        }
    }

    fun deleteInstallment(id: String) {
        viewModelScope.launch {
            deleteInstallmentUseCase(id)
            _feedbackMessage.value = InstallmentFeedback.DELETED
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    class Factory(private val database: SayItDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = InstallmentRepositoryImpl(database)
            return InstallmentsViewModel(
                getInstallmentsUseCase = GetInstallmentsUseCase(repository),
                getInstallmentForecastUseCase = GetInstallmentForecastUseCase(repository),
                saveInstallmentUseCase = SaveInstallmentUseCase(repository),
                recordInstallmentPaymentUseCase = RecordInstallmentPaymentUseCase(repository),
                deleteInstallmentUseCase = DeleteInstallmentUseCase(repository)
            ) as T
        }
    }
}
