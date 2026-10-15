package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Bill
import com.aurevant.pkk.domain.repo.FinanceRepo
import javax.inject.Inject

class GetBills @Inject constructor(private val repo: FinanceRepo) {
    suspend operator fun invoke(company: Int, onlyUnpaid: Boolean): List<Bill> = repo.bills(company, onlyUnpaid)
}
