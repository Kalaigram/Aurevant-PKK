package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.PayInput
import com.aurevant.pkk.domain.repo.ServiceRepo
import javax.inject.Inject

class PayOrder @Inject constructor(private val repo: ServiceRepo) {
    suspend operator fun invoke(company: Int, branch: Int, order: Int, pays: List<PayInput>) =
        repo.pay(company, branch, order, pays)
}
