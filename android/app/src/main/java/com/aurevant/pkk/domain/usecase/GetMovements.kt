package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Movement
import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

class GetMovements @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int, branch: Int): List<Movement> = repo.movements(company, branch)
}
