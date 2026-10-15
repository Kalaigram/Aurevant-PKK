package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.ReturnLine
import com.aurevant.pkk.domain.repo.SaleRepo
import javax.inject.Inject

class CreateRetur @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(
        company: Int,
        branch: Int,
        sale: Int,
        lines: List<ReturnLine>,
        reason: String,
        method: String,
    ) = repo.createReturn(company, branch, sale, lines, reason, method)
}
