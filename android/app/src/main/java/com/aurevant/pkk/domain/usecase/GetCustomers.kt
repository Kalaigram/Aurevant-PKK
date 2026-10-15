package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Customer
import com.aurevant.pkk.domain.repo.CustomerRepo
import javax.inject.Inject

class GetCustomers @Inject constructor(private val repo: CustomerRepo) {
    suspend operator fun invoke(company: Int): List<Customer> = repo.customers(company)
}
