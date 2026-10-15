package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.toDomain
import com.aurevant.pkk.domain.model.Bill
import com.aurevant.pkk.domain.model.Expense
import com.aurevant.pkk.domain.model.ExpenseCategory
import com.aurevant.pkk.domain.model.PayrollPeriod
import com.aurevant.pkk.domain.model.ProfitLoss
import com.aurevant.pkk.domain.model.Receivable
import com.aurevant.pkk.domain.repo.FinanceRepo
import com.aurevant.pkk.domain.repo.PayrollRepo
import javax.inject.Inject

class FinanceRepoImpl @Inject constructor(private val api: ApiService) : FinanceRepo {
    override suspend fun categories(company: Int): List<ExpenseCategory> =
        unwrap(api.expenseCategories(company)).data.map { ExpenseCategory(it.id, it.name) }

    override suspend fun expenses(company: Int, status: String?): List<Expense> =
        unwrap(api.expenses(company, status)).data.map { it.toDomain() }

    override suspend fun createExpense(company: Int, branch: Int, categoryId: Int, amount: Double, method: String, date: String, description: String) {
        api.createExpense(
            company,
            mapOf(
                "branch_id" to branch,
                "expense_category_id" to categoryId,
                "amount" to amount,
                "method" to method,
                "date" to date,
                "description" to description,
            ),
        )
    }

    override suspend fun expenseAct(company: Int, id: Int, setuju: Boolean) {
        if (setuju) api.approveExpense(company, id) else api.rejectExpense(company, id)
    }

    override suspend fun bills(company: Int, onlyUnpaid: Boolean): List<Bill> =
        unwrap(api.bills(company, if (onlyUnpaid) true else null)).data.map { it.toDomain() }

    override suspend fun payBill(company: Int, id: Int, amount: Double, method: String, note: String?) {
        api.payBill(company, id, mapOf("amount" to amount, "method" to method, "note" to note))
    }

    override suspend fun receivables(company: Int, onlyUnpaid: Boolean): List<Receivable> =
        unwrap(api.receivables(company, if (onlyUnpaid) true else null)).data.map { it.toDomain() }

    override suspend fun payReceivable(company: Int, id: Int, amount: Double, method: String, note: String?) {
        api.payReceivable(company, id, mapOf("amount" to amount, "method" to method, "note" to note))
    }

    override suspend fun profitLoss(company: Int, from: String, to: String, branchId: Int?): ProfitLoss =
        unwrap(api.profitLoss(company, from, to, branchId)).toDomain()
}

class PayrollRepoImpl @Inject constructor(private val api: ApiService) : PayrollRepo {
    override suspend fun periods(company: Int): List<PayrollPeriod> =
        unwrap(api.periods(company)).data.map { it.toDomain() }

    override suspend fun periodAct(company: Int, id: Int, aksi: String) {
        when (aksi) {
            "calculate" -> api.calculatePeriod(company, id)
            "review" -> api.reviewPeriod(company, id)
            "approve" -> api.approvePeriod(company, id)
            "pay" -> api.payPeriod(company, id)
            else -> throw IllegalArgumentException("Aksi periode tidak dikenal: $aksi")
        }
    }
}
