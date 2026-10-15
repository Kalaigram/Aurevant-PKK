package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.FinanceRepo
import javax.inject.Inject

/** Putuskan biaya: setujui atau tolak. */
class ActExpense @Inject constructor(private val repo: FinanceRepo) {
    suspend operator fun invoke(company: Int, id: Int, setuju: Boolean) = repo.expenseAct(company, id, setuju)
}
