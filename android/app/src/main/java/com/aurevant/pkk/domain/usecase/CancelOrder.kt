package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.ServiceRepo
import javax.inject.Inject

class CancelOrder @Inject constructor(private val repo: ServiceRepo) {
    suspend operator fun invoke(company: Int, branch: Int, order: Int) = repo.cancel(company, branch, order)
}
