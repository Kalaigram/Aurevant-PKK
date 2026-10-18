package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.TransferInput
import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

class CreateTransfer @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int, from: Int, to: Int, items: List<TransferInput>, note: String?) =
        repo.createTransfer(company, from, to, items, note)
}
