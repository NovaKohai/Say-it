package com.example.sayit.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sayit.data.local.SayItPreferences
import com.example.sayit.domain.repository.CategoryRepository
import com.example.sayit.domain.repository.DatabaseInspectorRepository
import com.example.sayit.domain.repository.TransactionRepository

class DashboardViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val databaseInspectorRepository: DatabaseInspectorRepository,
    private val preferences: SayItPreferences? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(transactionRepository, categoryRepository, databaseInspectorRepository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
