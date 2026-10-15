package com.aurevant.pkk.ui.features.approvals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Approval
import com.aurevant.pkk.domain.usecase.DecideApproval
import com.aurevant.pkk.domain.usecase.GetInbox
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ApprovalsAction {
    data object Load : ApprovalsAction
    data class Decide(val id: Int, val approve: Boolean, val note: String) : ApprovalsAction
    data object ClearMessage : ApprovalsAction
}

data class ApprovalsState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val items: List<Approval> = emptyList(),
)

@HiltViewModel
class ApprovalsViewModel @Inject constructor(
    private val session: SessionStore,
    private val getInbox: GetInbox,
    private val decideApproval: DecideApproval,
) : ViewModel() {
    private val _state = MutableStateFlow(ApprovalsState(busy = true))
    val state: StateFlow<ApprovalsState> = _state

    fun handleAction(action: ApprovalsAction) {
        when (action) {
            ApprovalsAction.Load -> refresh()
            is ApprovalsAction.Decide -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch
                _state.value = _state.value.copy(busy = true, error = null)
                _state.value = try {
                    decideApproval(company, action.id, action.approve, action.note.ifEmpty { null })
                    ApprovalsState(message = if (action.approve) "Disetujui." else "Ditolak.", items = getInbox(company))
                } catch (e: ApiError) {
                    _state.value.copy(busy = false, error = e.message)
                } catch (e: Exception) {
                    _state.value.copy(busy = false, error = e.message ?: "Terjadi kesalahan.")
                }
            }
            ApprovalsAction.ClearMessage -> _state.value = _state.value.copy(message = null, error = null)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val company = session.companyId.first() ?: return@launch run {
                _state.value = ApprovalsState(error = "Pilih perusahaan dulu.")
            }
            _state.value = try {
                ApprovalsState(items = getInbox(company))
            } catch (e: ApiError) {
                ApprovalsState(error = e.message)
            } catch (e: Exception) {
                ApprovalsState(error = e.message ?: "Terjadi kesalahan.")
            }
        }
    }
}
