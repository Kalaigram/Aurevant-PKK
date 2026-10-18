package com.aurevant.pkk.ui.features.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.CartLine
import com.aurevant.pkk.domain.model.CatalogItem
import com.aurevant.pkk.domain.model.PayInput
import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.model.Shift
import com.aurevant.pkk.domain.usecase.CloseShift
import com.aurevant.pkk.domain.usecase.CreateSale
import com.aurevant.pkk.domain.usecase.GetBranches
import com.aurevant.pkk.domain.usecase.GetItems
import com.aurevant.pkk.domain.usecase.GetPrice
import com.aurevant.pkk.domain.usecase.OpenShift
import com.aurevant.pkk.domain.usecase.SaveReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface PosAction {
    data object Load : PosAction
    data class PickBranch(val branch: Branch) : PosAction
    data class Search(val query: String) : PosAction
    data class Add(val item: CatalogItem) : PosAction
    data class Qty(val itemId: Int, val qty: Double) : PosAction
    data class Remove(val itemId: Int) : PosAction
    data class SetPayMethod(val index: Int, val method: String) : PosAction
    data class SetPayAmount(val index: Int, val amount: Double) : PosAction
    data object AddPayRow : PosAction
    data object Open : PosAction
    data class SetOpeningCash(val cash: Double) : PosAction
    data object Pay : PosAction
    data class SetCounted(val cash: Double) : PosAction
    data object Close : PosAction
    data object Receipt : PosAction
    data object ClearMessage : PosAction
}

data class PosState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val companyId: Int? = null,
    val branches: List<Branch> = emptyList(),
    val branch: Branch? = null,
    val shift: Shift? = null,
    val openingCash: Double = 0.0,
    val countedCash: Double = 0.0,
    val query: String = "",
    val catalog: List<CatalogItem> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val pays: List<PayInput> = listOf(PayInput("tunai", 0.0)),
    val sale: Sale? = null,
    val receiptFile: File? = null,
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val session: SessionStore,
    private val getBranches: GetBranches,
    private val getItems: GetItems,
    private val getPrice: GetPrice,
    private val openShift: OpenShift,
    private val closeShift: CloseShift,
    private val createSale: CreateSale,
    private val saveReceipt: SaveReceipt,
) : ViewModel() {
    private val _state = MutableStateFlow(PosState(busy = true))
    val state: StateFlow<PosState> = _state

    private inline fun update(f: (PosState) -> PosState) {
        _state.value = f(_state.value)
    }

    private fun fail(e: Throwable): PosState {
        val s = _state.value
        return s.copy(busy = false, error = if (e is ApiError) e.message else e.message ?: "Terjadi kesalahan.")
    }

    fun handleAction(action: PosAction) {
        when (action) {
            PosAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = PosState(error = "Pilih perusahaan dulu.")
                }
                update { it.copy(busy = true, error = null, companyId = company) }
                try {
                    val branches = getBranches(company)
                    update { it.copy(busy = false, branches = branches) }
                } catch (e: Exception) {
                    update { fail(e) }
                }
            }
            is PosAction.PickBranch -> {
                update { it.copy(branch = action.branch, cart = emptyList(), sale = null, shift = null, receiptFile = null) }
                loadCatalog()
            }
            is PosAction.Search -> {
                update { it.copy(query = action.query) }
                loadCatalog()
            }
            is PosAction.Add -> addItem(action.item)
            is PosAction.Qty -> update {
                it.copy(cart = it.cart.map { l -> if (l.item.id == action.itemId) l.copy(qty = action.qty.coerceAtLeast(0.0)) else l })
            }
            is PosAction.Remove -> update { it.copy(cart = it.cart.filterNot { l -> l.item.id == action.itemId }) }
            is PosAction.SetPayMethod -> update {
                it.copy(pays = it.pays.mapIndexed { i, p -> if (i == action.index) p.copy(method = action.method) else p })
            }
            is PosAction.SetPayAmount -> update {
                it.copy(pays = it.pays.mapIndexed { i, p -> if (i == action.index) p.copy(amount = action.amount) else p })
            }
            PosAction.AddPayRow -> update { it.copy(pays = it.pays + PayInput("tunai", 0.0)) }
            is PosAction.SetOpeningCash -> update { it.copy(openingCash = action.cash) }
            PosAction.Open -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                val branch = s.branch ?: return@launch
                update { it.copy(busy = true, error = null) }
                try {
                    val shift = openShift(company, branch.id, s.openingCash)
                    update { it.copy(busy = false, shift = shift, message = "Shift dibuka.") }
                } catch (e: Exception) {
                    update { fail(e) }
                }
            }
            PosAction.Pay -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                val branch = s.branch ?: return@launch
                if (s.cart.isEmpty()) return@launch run { update { it.copy(error = "Keranjang masih kosong.") } }
                update { it.copy(busy = true, error = null) }
                try {
                    val sale = createSale(company, branch.id, s.cart, s.pays)
                    update {
                        it.copy(
                            busy = false,
                            sale = sale,
                            pays = listOf(PayInput("tunai", 0.0)),
                            message = "Nota terbit.",
                        )
                    }
                } catch (e: Exception) {
                    update { fail(e) }
                }
            }
            is PosAction.SetCounted -> update { it.copy(countedCash = action.cash) }
            PosAction.Close -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                val branch = s.branch ?: return@launch
                val shift = s.shift ?: return@launch
                update { it.copy(busy = true, error = null) }
                try {
                    val closed = closeShift(company, branch.id, shift.id, s.countedCash)
                    update { it.copy(busy = false, shift = null, message = "Shift ditutup. Selisih: ${closed.difference ?: "0"}") }
                } catch (e: Exception) {
                    update { fail(e) }
                }
            }
            PosAction.Receipt -> viewModelScope.launch {
                val s = _state.value
                val branch = s.branch ?: return@launch
                val sale = s.sale ?: return@launch
                update { it.copy(busy = true, error = null) }
                try {
                    val companyName = session.companyName.first() ?: ""
                    val names = s.cart.associate { it.item.id to it.item.name }
                    val file = saveReceipt(sale, companyName, branch.name, names)
                    update { it.copy(busy = false, receiptFile = file, message = "Struk PDF disimpan: ${file.name}") }
                } catch (e: Exception) {
                    update { fail(e) }
                }
            }
            PosAction.ClearMessage -> update { it.copy(message = null, error = null) }
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            val s = _state.value
            val company = s.companyId ?: return@launch
            try {
                val all = getItems(company, null)
                val q = s.query.trim().lowercase()
                update {
                    it.copy(catalog = if (q.isEmpty()) all else all.filter { i -> i.name.lowercase().contains(q) || i.code.lowercase().contains(q) })
                }
            } catch (e: Exception) {
                update { fail(e) }
            }
        }
    }

    private fun addItem(item: CatalogItem) {
        viewModelScope.launch {
            val s = _state.value
            val company = s.companyId ?: return@launch
            val branch = s.branch ?: return@launch
            val exists = s.cart.find { it.item.id == item.id }
            if (exists != null) {
                update { it.copy(cart = it.cart.map { l -> if (l.item.id == item.id) l.copy(qty = l.qty + 1) else l }) }
                return@launch
            }
            update { it.copy(busy = true) }
            try {
                val price = getPrice(company, item.id, branch.id)
                update { it.copy(busy = false, cart = it.cart + CartLine(item, price, 1.0)) }
            } catch (e: Exception) {
                update { fail(e) }
            }
        }
    }
}
