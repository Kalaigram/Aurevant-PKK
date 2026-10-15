package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.SalePaymentRequest
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.toDomain
import com.aurevant.pkk.domain.model.Customer
import com.aurevant.pkk.domain.model.OrderInput
import com.aurevant.pkk.domain.model.PayInput
import com.aurevant.pkk.domain.model.ServiceOrder
import com.aurevant.pkk.domain.repo.CustomerRepo
import com.aurevant.pkk.domain.repo.ServiceRepo
import javax.inject.Inject

class ServiceRepoImpl @Inject constructor(private val api: ApiService) : ServiceRepo {
    override suspend fun orders(company: Int, branch: Int): List<ServiceOrder> =
        unwrap(api.serviceOrders(company, branch)).data.map {
            ServiceOrder(it.id, branch, it.customerId, null, null, it.status, it.depositAmount, null)
        }

    override suspend fun createOrder(company: Int, branch: Int, customerId: Int?, items: List<OrderInput>, note: String?) {
        api.createOrder(
            company,
            branch,
            mapOf(
                "customer_id" to customerId,
                "items" to items.map { mapOf("item_id" to it.itemId, "qty" to it.qty) },
                "note" to note,
            ),
        )
    }

    override suspend fun deposit(company: Int, branch: Int, order: Int, amount: Double) {
        api.depositOrder(company, branch, order, mapOf("amount" to amount))
    }

    override suspend fun process(company: Int, branch: Int, order: Int, assigneeId: Int?) {
        api.processOrder(company, branch, order, mapOf("assignee_id" to assigneeId))
    }

    override suspend fun complete(company: Int, branch: Int, order: Int) {
        api.completeOrder(company, branch, order)
    }

    override suspend fun pay(company: Int, branch: Int, order: Int, pays: List<PayInput>) {
        api.payOrder(
            company,
            branch,
            order,
            mapOf("payments" to pays.map { SalePaymentRequest(it.method, it.amount, it.tendered, it.reference) }),
        )
    }

    override suspend fun cancel(company: Int, branch: Int, order: Int) {
        api.cancelOrder(company, branch, order)
    }
}

class CustomerRepoImpl @Inject constructor(private val api: ApiService) : CustomerRepo {
    override suspend fun customers(company: Int): List<Customer> =
        unwrap(api.customers(company)).data.map { it.toDomain() }
}
