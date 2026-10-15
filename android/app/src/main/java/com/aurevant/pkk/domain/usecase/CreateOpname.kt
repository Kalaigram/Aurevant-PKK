package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.OpnameInput
import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

class CreateOpname @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int, branch: Int, lines: List<OpnameInput>, note: String?) =
        repo.createOpname(company, branch, lines, note)
}
