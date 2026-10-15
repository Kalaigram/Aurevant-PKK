package com.aurevant.pkk.ui.features.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.usecase.GetBranches
import com.aurevant.pkk.domain.usecase.GetMyPermissions
import com.aurevant.pkk.domain.usecase.GetNotas
import com.aurevant.pkk.domain.usecase.RequestApproval
import com.aurevant.pkk.domain.usecase.VoidNota
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SalesAction {
    data object Load : SalesAction
    data class PickBranch(val branch: Branch) : SalesAction
    data class Void(val sale: Sale, val reason: String) : SalesAction
    data class Retur(val sale: Sale, val itemId: Int, val qty: Double, val condition: String, val method: String, val reason: String) : SalesAction
    data object ClearMessage : SalesAction
}

data class SalesState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val companyId: Int? = null,
    val branches: List<Branch> = emptyList(),
    val branch: Branch? = null,
    val notas: List<Sale> = emptyList(),
    val perms: Set<String> = emptySet(),
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val session: SessionStore,
    private val getBranches: GetBranches,
    private val getMyPermissions: GetMyPermissions,
    private val getNotas: GetNotas,
    private val voidNota: VoidNota,
    private val requestApproval: RequestApproval,
) : ViewModel() {
    private val _state = MutableStateFlow(SalesState(busy = true))
    val state: StateFlow<SalesState> = _state

    private fun update(f: (SalesState) -> SalesState) {
        _state.value = f(_state.value)
    }

    private fun fail(e: Exception): SalesState {
        val s = _state.value
        val msg = if (e is ApiError) e.message else e.message ?: "Terjadi kesalahan."
        return s.copy(busy = false, error = msg)
    }

    fun handleAction(action: SalesAction) {
        when (action) {
            SalesAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = SalesState(error = "Pilih perusahaan dulu.")
                }
                update { it.copy(busy = true, error = null, companyId = company) }
                update {
                    try {
                        it.copy(
                            busy = false,
                            branches = getBranches(company),
                            perms = getMyPermissions(company).toSet(),
                        )
                    } catch (e: Exception) { fail(e) }
                }
            }
            is SalesAction.PickBranch -> {
                update { it.copy(branch = action.branch, message = null) }
                refresh()
            }
            is SalesAction.Void -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                val branch = s.branch ?: return@launch
                if (action.reason.isBlank()) {
                    update { it.copy(error = "Alasan void wajib diisi.") }
                    return@launch
                }
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        if ("transaksi.void" in s.perms) {
                            voidNota(company, branch.id, action.sale.id, action.reason)
                            it.copy(busy = false, message = "Nota ${action.sale.number} dibatalkan (void).")
                        } else {
                            requestApproval(company, "sale.void", action.sale.id, mapOf("reason" to action.reason), null)
                            it.copy(busy = false, message = "Pengajuan void dikirim ke atasan.")
                        }
                    } catch (e: Exception) { fail(e) }
                }
                refresh()
            }
            is SalesAction.Retur -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                if (action.reason.isBlank()) {
                    update { it.copy(error = "Alasan retur wajib diisi.") }
                    return@launch
                }
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        // ID baris tidak dibuka API: ajukan ke atasan, dilengkapi manajer di web.
                        requestApproval(
                            company, "sale.refund", action.sale.id,
                            mapOf(
                                "lines" to listOf(mapOf("item_id" to action.itemId, "qty" to action.qty, "condition" to action.condition)),
                                "reason" to action.reason,
                                "refund_method" to action.method,
                            ),
                            null,
                        )
                        it.copy(busy = false, message = "Pengajuan retur dikirim. Eksekusi akhir oleh manajer.")
                    } catch (e: Exception) { fail(e) }
                }
            }
            SalesAction.ClearMessage -> update { it.copy(message = null, error = null) }
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
                    it.copy(busy = false, notas = getNotas(company, branch.id))
                } catch (e: Exception) { fail(e) }
            }
        }
    }
}
