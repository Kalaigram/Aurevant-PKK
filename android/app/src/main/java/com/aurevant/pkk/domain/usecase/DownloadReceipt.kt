package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.SaleRepo
import java.io.File
import javax.inject.Inject

class DownloadReceipt @Inject constructor(private val repo: SaleRepo) {
    suspend operator fun invoke(company: Int, branch: Int, sale: Int, number: String): File =
        repo.receiptPdf(company, branch, sale, number)
}
