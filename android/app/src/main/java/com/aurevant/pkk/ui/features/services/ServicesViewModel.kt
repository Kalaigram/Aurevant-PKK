package com.aurevant.pkk.ui.features.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.OrderInput
import com.aurevant.pkk.domain.model.PayInput
import com.aurevant.pkk.domain.model.ServiceOrder
import com.aurevant.pkk.domain.usecase.CancelOrder
import com.aurevant.pkk.domain.usecase.CompleteOrder
import com.aurevant.pkk.domain.usecase.CreateOrder
import com.aurevant.pkk.domain.usecase.DepositOrder
import com.aurevant.pkk.domain.usecase.GetBranches
import com.aurevant.pkk.domain.usecase.GetMyPermissions
import com.aurevant.pkk.domain.usecase.GetOrders
import com.aurevant.pkk.domain.usecase.PayOrder
import com.aurevant.pkk.domain.usecase.ProcessOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ServicesAction {
    data object Load : ServicesAction
    data class PickBranch(val branch: Branch) : ServicesAction
    data class Create(val itemId: Int, val qty: Double, val note: String) : ServicesAction
    data class Deposit(val order: ServiceOrder, val amount: Double) : ServicesAction
    data class Process(val order: ServiceOrder) : ServicesAction
    data class Complete(val order: ServiceOrder) : ServicesAction
    data class Pay(val order: ServiceOrder, val amount: Double, val method: String) : ServicesAction
    data class Cancel(val order: ServiceOrder) : ServicesAction
    data object ClearMessage : ServicesAction
}

data class ServicesState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val companyId: Int? = null,
    val branches: List<Branch> = emptyList(),
    val branch: Branch? = null,
    val orders: List<ServiceOrder> = emptyList(),
    val perms: Set<String> = emptySet(),
)

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val session: SessionStore,
    private val getBranches: GetBranches,
    private val getMyPermissions: GetMyPermissions,
    private val getOrders: GetOrders,
    private val createOrder: CreateOrder,
    private val depositOrder: DepositOrder,
    private val processOrder: ProcessOrder,
    private val completeOrder: CompleteOrder,
    private val payOrder: PayOrder,
    private val cancelOrder: CancelOrder,
) : ViewModel() {
    private val _state = MutableStateFlow(ServicesState(busy = true))
    val state: StateFlow<ServicesState> = _state

    private fun update(f: (ServicesState) -> ServicesState) {
        _state.value = f(_state.value)
    }

    fun handleAction(action: ServicesAction) {
        when (action) {
            ServicesAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = ServicesState(error = "Pilih perusahaan dulu.")
                }
                update { it.copy(busy = true, error = null, companyId = company) }
                update {
                    try {
                        it.copy(busy = false, branches = getBranches(company), perms = getMyPermissions(company).toSet())
                    } catch (e: Exception) {
                        it.copy(busy = false, error = (e as? ApiError)?.message ?: e.message)
                    }
                }
            }
            is ServicesAction.PickBranch -> {
                update { it.copy(branch = action.branch, message = null) }
                refresh()
            }
            is ServicesAction.Create -> act("Order jasa dibuat.") { s, c, b ->
                createOrder(c, b.id, null, listOf(OrderInput(action.itemId, action.qty)), action.note.ifBlank { null })
            }
            is ServicesAction.Deposit -> act("DP tercatat.") { s, c, b ->
                depositOrder(c, b.id, action.order.id, action.amount)
            }
            is ServicesAction.Process -> act("Order diproses.") { s, c, b ->
                processOrder(c, b.id, action.order.id, null)
            }
            is ServicesAction.Complete -> act("Order selesai. Bahan terpotong, komisi terhitung.") { s, c, b ->
                completeOrder(c, b.id, action.order.id)
            }
            is ServicesAction.Pay -> act("Order dibayar, nota terbit.") { s, c, b ->
                payOrder(c, b.id, action.order.id, listOf(PayInput(action.method, action.amount)))
            }
            is ServicesAction.Cancel -> act("Order dibatalkan.") { s, c, b ->
                cancelOrder(c, b.id, action.order.id)
            }
            ServicesAction.ClearMessage -> update { it.copy(message = null, error = null) }
        }
    }

    private fun act(msg: String, call: suspend (ServicesState, Int, Branch) -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            val company = s.companyId ?: return@launch
            val branch = s.branch ?: return@launch run { update { it.copy(error = "Pilih cabang dulu.") } }
            update { it.copy(busy = true, error = null, message = null) }
            update {
                try {
                    call(s, company, branch)
                    it.copy(busy = false, message = msg)
                } catch (e: Exception) {
                    it.copy(busy = false, error = (e as? ApiError)?.message ?: e.message)
                }
            }
            refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val s = _state.value
            val company = s.companyId ?: return@launch
            val branch = s.branch ?: return@launch
            update { it.copy(busy = true, error = null) }
            update {
                try {
                    it.copy(busy = false, orders = getOrders(company, branch.id))
                } catch (e: Exception) {
                    it.copy(busy = false, error = (e as? ApiError)?.message ?: e.message)
                }
            }
        }
    }
}
