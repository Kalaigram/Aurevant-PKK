package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.FinanceRepo
import javax.inject.Inject

class CreateExpense @Inject constructor(private val repo: FinanceRepo) {
    suspend operator fun invoke(
        company: Int,
        branch: Int,
        categoryId: Int,
        amount: Double,
        method: String,
        date: String,
        description: String,
    ) = repo.createExpense(company, branch, categoryId, amount, method, date, description)
}
