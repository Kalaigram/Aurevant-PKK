package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.repo.SaleRepo
import javax.inject.Inject

class GetNota @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(company: Int, branch: Int, sale: Int): Sale = repo.notaDetail(company, branch, sale)
}
