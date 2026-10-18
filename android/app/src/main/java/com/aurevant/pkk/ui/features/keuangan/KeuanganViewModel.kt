package com.aurevant.pkk.ui.features.keuangan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Bill
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.Expense
import com.aurevant.pkk.domain.model.ExpenseCategory
import com.aurevant.pkk.domain.model.ProfitLoss
import com.aurevant.pkk.domain.model.Receivable
import com.aurevant.pkk.domain.usecase.ActExpense
import com.aurevant.pkk.domain.usecase.CreateExpense
import com.aurevant.pkk.domain.usecase.GetBills
import com.aurevant.pkk.domain.usecase.GetBranches
import com.aurevant.pkk.domain.usecase.GetCategories
import com.aurevant.pkk.domain.usecase.GetExpenses
import com.aurevant.pkk.domain.usecase.GetMyPermissions
import com.aurevant.pkk.domain.usecase.GetProfitLoss
import com.aurevant.pkk.domain.usecase.GetReceivables
import com.aurevant.pkk.domain.usecase.PayBill
import com.aurevant.pkk.domain.usecase.PayReceivable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface KeuanganAction {
    data object Load : KeuanganAction
    data class FilterStatus(val status: String?) : KeuanganAction
    data class Expense(val branchId: Int, val categoryId: Int, val amount: Double, val date: String, val description: String) : KeuanganAction
    data class ExpenseAct(val id: Int, val setuju: Boolean) : KeuanganAction
    data class Bill(val id: Int, val amount: Double) : KeuanganAction
    data class Receivable(val id: Int, val amount: Double) : KeuanganAction
    data object ClearMessage : KeuanganAction
}

data class KeuanganState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val companyId: Int? = null,
    val branches: List<Branch> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val filterStatus: String? = "draft",
    val bills: List<Bill> = emptyList(),
    val receivables: List<Receivable> = emptyList(),
    val profit: ProfitLoss? = null,
    val perms: Set<String> = emptySet(),
)

@HiltViewModel
class KeuanganViewModel @Inject constructor(
    private val session: SessionStore,
    private val getBranches: GetBranches,
    private val getCategories: GetCategories,
    private val getMyPermissions: GetMyPermissions,
    private val getExpenses: GetExpenses,
    private val createExpense: CreateExpense,
    private val actExpense: ActExpense,
    private val getBills: GetBills,
    private val payBill: PayBill,
    private val getReceivables: GetReceivables,
    private val payReceivable: PayReceivable,
    private val getProfitLoss: GetProfitLoss,
) : ViewModel() {
    private val _state = MutableStateFlow(KeuanganState(busy = true))
    val state: StateFlow<KeuanganState> = _state

    private inline fun update(f: (KeuanganState) -> KeuanganState) {
        _state.value = f(_state.value)
    }

    private fun err(e: Exception): String = (e as? ApiError)?.message ?: e.message ?: "Terjadi kesalahan."

    fun handleAction(action: KeuanganAction) {
        when (action) {
            KeuanganAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = KeuanganState(error = "Pilih perusahaan dulu.")
                }
                update { it.copy(busy = true, error = null, companyId = company) }
                update {
                    try {
                        it.copy(busy = false, perms = getMyPermissions(company).toSet())
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is KeuanganAction.FilterStatus -> {
                update { it.copy(filterStatus = action.status) }
                refresh()
            }
            is KeuanganAction.Expense -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        createExpense(company, action.branchId, action.categoryId, action.amount, "tunai", action.date, action.description)
                        it.copy(busy = false, message = "Biaya tercatat (draft).")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is KeuanganAction.ExpenseAct -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        actExpense(company, action.id, action.setuju)
                        it.copy(busy = false, message = if (action.setuju) "Biaya disetujui dan menjurnal." else "Biaya ditolak.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is KeuanganAction.Bill -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        payBill(company, action.id, action.amount, "transfer", null)
                        it.copy(busy = false, message = "Pembayaran hutang tercatat.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is KeuanganAction.Receivable -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        payReceivable(company, action.id, action.amount, "tunai", null)
                        it.copy(busy = false, message = "Cicilan piutang tercatat.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            KeuanganAction.ClearMessage -> update { it.copy(message = null, error = null) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val s = _state.value
            val company = s.companyId ?: return@launch
            val today = LocalDate.now()
            val from = today.withDayOfMonth(1).toString()
            update { it.copy(busy = true, error = null) }
            update {
                try {
                    it.copy(
                        busy = false,
                        branches = getBranches(company),
                        categories = getCategories(company),
                        expenses = getExpenses(company, s.filterStatus),
                        bills = getBills(company, true),
                        receivables = getReceivables(company, true),
                        profit = getProfitLoss(company, from, today.toString(), null),
                    )
                } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
            }
        }
    }
}
