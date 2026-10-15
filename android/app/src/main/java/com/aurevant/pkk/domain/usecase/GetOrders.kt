package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.ServiceOrder
import com.aurevant.pkk.domain.repo.ServiceRepo
import javax.inject.Inject

class GetOrders @Inject constructor(private val repo: ServiceRepo) {
    suspend operator fun invoke(company: Int, branch: Int): List<ServiceOrder> = repo.orders(company, branch)
}
