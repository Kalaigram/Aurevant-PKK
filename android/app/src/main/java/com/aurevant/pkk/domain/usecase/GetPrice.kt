package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.SaleRepo
import javax.inject.Inject

class GetPrice @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(company: Int, item: Int, branch: Int): Double = repo.price(company, item, branch)
}
