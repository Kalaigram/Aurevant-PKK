package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

class CreateMovement @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int, branch: Int, itemId: Int, type: String, qty: Double, unitCost: Double?) =
        repo.createMovement(company, branch, itemId, type, qty, unitCost)
}
