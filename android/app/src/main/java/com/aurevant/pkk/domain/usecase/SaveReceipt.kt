package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.repo.SaleRepo
import java.io.File
import javax.inject.Inject

class SaveReceipt @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(sale: Sale, companyName: String, branchName: String, names: Map<Int, String>): File =
        repo.receiptPdf(sale, companyName, branchName, names)
}
