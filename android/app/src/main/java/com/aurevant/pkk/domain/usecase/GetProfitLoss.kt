package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.ProfitLoss
import com.aurevant.pkk.domain.repo.FinanceRepo
import javax.inject.Inject

class GetProfitLoss @Inject constructor(private val repo: FinanceRepo) {
    suspend operator fun invoke(company: Int, from: String, to: String, branchId: Int?): ProfitLoss =
        repo.profitLoss(company, from, to, branchId)
}
