package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.CatalogItem
import com.aurevant.pkk.domain.repo.SaleRepo
import javax.inject.Inject

class GetItems @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(company: Int, type: String? = null): List<CatalogItem> = repo.items(company, type)
}
