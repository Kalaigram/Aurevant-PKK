package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.PayrollPeriod
import com.aurevant.pkk.domain.repo.PayrollRepo
import javax.inject.Inject

class GetPeriods @Inject constructor(private val repo: PayrollRepo) {
    suspend operator fun invoke(company: Int): List<PayrollPeriod> = repo.periods(company)
}
