package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.CartLine
import com.aurevant.pkk.domain.model.PayInput
import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.repo.SaleRepo
import java.util.UUID
import javax.inject.Inject

class CreateSale @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(company: Int, branch: Int, lines: List<CartLine>, pays: List<PayInput>): Sale =
        repo.createSale(company, branch, lines, pays, UUID.randomUUID().toString())
}
