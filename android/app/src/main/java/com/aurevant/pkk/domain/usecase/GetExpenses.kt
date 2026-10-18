package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Expense
import com.aurevant.pkk.domain.repo.FinanceRepo
import javax.inject.Inject

class GetExpenses @Inject constructor(private val repo: FinanceRepo) {
    suspend operator fun invoke(company: Int, status: String?): List<Expense> = repo.expenses(company, status)
}
