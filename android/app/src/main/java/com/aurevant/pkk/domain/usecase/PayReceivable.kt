package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.FinanceRepo
import javax.inject.Inject

class PayReceivable @Inject constructor(private val repo: FinanceRepo) {
    suspend operator fun invoke(company: Int, id: Int, amount: Double, method: String, note: String?) =
        repo.payReceivable(company, id, amount, method, note)
}
