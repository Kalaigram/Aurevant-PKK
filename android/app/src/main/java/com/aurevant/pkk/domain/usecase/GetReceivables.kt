package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Receivable
import com.aurevant.pkk.domain.repo.FinanceRepo
import javax.inject.Inject

class GetReceivables @Inject constructor(private val repo: FinanceRepo) {
    suspend operator fun invoke(company: Int, onlyUnpaid: Boolean): List<Receivable> =
        repo.receivables(company, onlyUnpaid)
}
