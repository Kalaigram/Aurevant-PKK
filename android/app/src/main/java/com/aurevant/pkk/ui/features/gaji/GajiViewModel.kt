package com.aurevant.pkk.ui.features.gaji

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.PayrollPeriod
import com.aurevant.pkk.domain.usecase.ActPeriod
import com.aurevant.pkk.domain.usecase.GetMyPermissions
import com.aurevant.pkk.domain.usecase.GetPeriods
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface GajiAction {
    data object Load : GajiAction
    data class Act(val period: PayrollPeriod, val aksi: String) : GajiAction
    data object ClearMessage : GajiAction
}

data class GajiState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val companyId: Int? = null,
    val periods: List<PayrollPeriod> = emptyList(),
    val perms: Set<String> = emptySet(),
)

@HiltViewModel
class GajiViewModel @Inject constructor(
    private val session: SessionStore,
    private val getMyPermissions: GetMyPermissions,
    private val getPeriods: GetPeriods,
    private val actPeriod: ActPeriod,
) : ViewModel() {
    private val _state = MutableStateFlow(GajiState(busy = true))
    val state: StateFlow<GajiState> = _state

    private inline fun update(f: (GajiState) -> GajiState) {
        _state.value = f(_state.value)
    }

    private fun err(e: Exception): String = (e as? ApiError)?.message ?: e.message ?: "Terjadi kesalahan."

    fun handleAction(action: GajiAction) {
        when (action) {
            GajiAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = GajiState(error = "Pilih perusahaan dulu.")
                }
                update { it.copy(busy = true, error = null, companyId = company) }
                update {
                    try {
                        it.copy(busy = false, periods = getPeriods(company), perms = getMyPermissions(company).toSet())
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
            }
            is GajiAction.Act -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        actPeriod(company, action.period.id, action.aksi)
                        it.copy(busy = false, message = "Periode ${action.period.name}: ${action.aksi}.", periods = getPeriods(company))
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
            }
            GajiAction.ClearMessage -> update { it.copy(message = null, error = null) }
        }
    }
}
