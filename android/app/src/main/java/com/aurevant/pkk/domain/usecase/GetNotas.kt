package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.repo.SaleRepo
import javax.inject.Inject

class GetNotas @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(company: Int, branch: Int): List<Sale> = repo.notas(company, branch)
}
