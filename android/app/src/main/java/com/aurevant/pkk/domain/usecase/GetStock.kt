package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.StockLine
import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

class GetStock @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int, branch: Int): List<StockLine> = repo.stock(company, branch)
}
