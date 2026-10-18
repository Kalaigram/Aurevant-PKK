package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.ServiceRepo
import javax.inject.Inject

class ProcessOrder @Inject constructor(private val repo: ServiceRepo) {
    suspend operator fun invoke(company: Int, branch: Int, order: Int, assigneeId: Int?) =
        repo.process(company, branch, order, assigneeId)
}
