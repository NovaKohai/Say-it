package com.example.sayit.domain.repository

import com.example.sayit.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun insertCategory(category: Category)
    suspend fun updateBudget(categoryId: String, monthlyBudget: Double?)
}
