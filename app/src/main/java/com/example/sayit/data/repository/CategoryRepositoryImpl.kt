package com.example.sayit.data.repository

import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.local.mapper.toDomain
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val database: SayItDatabase
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> {
        return database.observeCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return Category.findDefault(id)
    }

    override suspend fun insertCategory(category: Category) {
        // Handled via defaults or custom additions
    }

    override suspend fun updateBudget(categoryId: String, monthlyBudget: Double?) {
        database.updateBudget(categoryId, monthlyBudget)
    }
}
