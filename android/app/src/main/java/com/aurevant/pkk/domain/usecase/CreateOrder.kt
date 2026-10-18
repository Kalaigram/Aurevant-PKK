package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.OrderInput
import com.aurevant.pkk.domain.repo.ServiceRepo
import javax.inject.Inject

class CreateOrder @Inject constructor(private val repo: ServiceRepo) {
    suspend operator fun invoke(company: Int, branch: Int, customerId: Int?, items: List<OrderInput>, note: String?) =
        repo.createOrder(company, branch, customerId, items, note)
}
