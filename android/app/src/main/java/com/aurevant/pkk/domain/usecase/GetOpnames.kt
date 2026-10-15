package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Opname
import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

class GetOpnames @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int, branch: Int): List<Opname> = repo.opnames(company, branch)
}
