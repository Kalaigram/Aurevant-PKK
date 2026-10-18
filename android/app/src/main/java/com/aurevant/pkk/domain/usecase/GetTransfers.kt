package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Transfer
import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

class GetTransfers @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int): List<Transfer> = repo.transfers(company)
}
