package com.aurevant.pkk.ui.features.gudang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.Movement
import com.aurevant.pkk.domain.model.Opname
import com.aurevant.pkk.domain.model.OpnameInput
import com.aurevant.pkk.domain.model.StockLine
import com.aurevant.pkk.domain.model.Transfer
import com.aurevant.pkk.domain.model.TransferInput
import com.aurevant.pkk.domain.usecase.ActOpname
import com.aurevant.pkk.domain.usecase.ActTransfer
import com.aurevant.pkk.domain.usecase.CreateMovement
import com.aurevant.pkk.domain.usecase.CreateOpname
import com.aurevant.pkk.domain.usecase.CreateTransfer
import com.aurevant.pkk.domain.usecase.GetBranches
import com.aurevant.pkk.domain.usecase.GetMovements
import com.aurevant.pkk.domain.usecase.GetMyPermissions
import com.aurevant.pkk.domain.usecase.GetOpnames
import com.aurevant.pkk.domain.usecase.GetStock
import com.aurevant.pkk.domain.usecase.GetTransfers
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface GudangAction {
    data object Load : GudangAction
    data class PickBranch(val branch: Branch) : GudangAction
    data class Transfer(val to: Branch, val itemId: Int, val qty: Double, val note: String) : GudangAction
    data class TransferAct(val transfer: Transfer, val aksi: String) : GudangAction
    data class Opname(val lines: List<OpnameInput>, val note: String) : GudangAction
    data class OpnameAct(val opname: Opname, val aksi: String) : GudangAction
    data class Movement(val itemId: Int, val type: String, val qty: Double) : GudangAction
    data object ClearMessage : GudangAction
}

data class GudangState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val companyId: Int? = null,
    val branches: List<Branch> = emptyList(),
    val branch: Branch? = null,
    val stock: List<StockLine> = emptyList(),
    val movements: List<Movement> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val opnames: List<Opname> = emptyList(),
    val perms: Set<String> = emptySet(),
)

@HiltViewModel
class GudangViewModel @Inject constructor(
    private val session: SessionStore,
    private val getBranches: GetBranches,
    private val getMyPermissions: GetMyPermissions,
    private val getTransfers: GetTransfers,
    private val createTransfer: CreateTransfer,
    private val actTransfer: ActTransfer,
    private val getOpnames: GetOpnames,
    private val createOpname: CreateOpname,
    private val actOpname: ActOpname,
    private val getMovements: GetMovements,
    private val getStock: GetStock,
    private val createMovement: CreateMovement,
) : ViewModel() {
    private val _state = MutableStateFlow(GudangState(busy = true))
    val state: StateFlow<GudangState> = _state

    private inline fun update(f: (GudangState) -> GudangState) {
        _state.value = f(_state.value)
    }

    private fun err(e: Exception): String = (e as? ApiError)?.message ?: e.message ?: "Terjadi kesalahan."

    fun handleAction(action: GudangAction) {
        when (action) {
            GudangAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = GudangState(error = "Pilih perusahaan dulu.")
                }
                update { it.copy(busy = true, error = null, companyId = company) }
                update {
                    try {
                        it.copy(busy = false, branches = getBranches(company), perms = getMyPermissions(company).toSet())
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
            }
            is GudangAction.PickBranch -> {
                update { it.copy(branch = action.branch, message = null) }
                refresh()
            }
            is GudangAction.Transfer -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                val from = s.branch ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        createTransfer(company, from.id, action.to.id, listOf(TransferInput(action.itemId, action.qty)), action.note.ifBlank { null })
                        it.copy(busy = false, message = "Transfer dibuat (draft).")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is GudangAction.TransferAct -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        actTransfer(company, action.transfer.id, action.aksi)
                        it.copy(busy = false, message = "Transfer ${action.transfer.id}: ${action.aksi}.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is GudangAction.Opname -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                val branch = s.branch ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        createOpname(company, branch.id, action.lines, action.note.ifBlank { null })
                        it.copy(busy = false, message = "Opname dibuat (draft).")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is GudangAction.OpnameAct -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                val branch = s.branch ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        actOpname(company, branch.id, action.opname.id, action.aksi)
                        it.copy(busy = false, message = "Opname ${action.opname.id}: ${action.aksi}.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            GudangAction.ClearMessage -> update { it.copy(message = null, error = null) }
            is GudangAction.Movement -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                val branch = s.branch ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        createMovement(company, branch.id, action.itemId, action.type, action.qty, null)
                        it.copy(busy = false, message = "Penyesuaian manual tercatat di audit.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
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
                    it.copy(
                        busy = false,
                        stock = getStock(company, branch.id),
                        transfers = getTransfers(company),
                        opnames = getOpnames(company, branch.id),
                        movements = getMovements(company, branch.id),
                    )
                } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
            }
        }
    }
}
