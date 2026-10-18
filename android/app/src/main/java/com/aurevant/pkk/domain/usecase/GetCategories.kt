package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.ExpenseCategory
import com.aurevant.pkk.domain.repo.FinanceRepo
import javax.inject.Inject

class GetCategories @Inject constructor(private val repo: FinanceRepo) {
    suspend operator fun invoke(company: Int): List<ExpenseCategory> = repo.categories(company)
}
