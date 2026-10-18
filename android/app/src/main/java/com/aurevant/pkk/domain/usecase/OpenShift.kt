package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Shift
import com.aurevant.pkk.domain.repo.SaleRepo
import javax.inject.Inject

class OpenShift @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(company: Int, branch: Int, cash: Double): Shift = repo.openShift(company, branch, cash)
}
