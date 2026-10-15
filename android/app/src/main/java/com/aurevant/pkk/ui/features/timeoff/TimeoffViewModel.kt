package com.aurevant.pkk.ui.features.timeoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.LeaveItem
import com.aurevant.pkk.domain.usecase.GetMyLeaves
import com.aurevant.pkk.domain.usecase.RequestLeave
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface TimeoffAction {
    data object Load : TimeoffAction
    data class File(val type: String, val from: String, val to: String, val reason: String) : TimeoffAction
    data object ClearMessage : TimeoffAction
}

data class TimeoffState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val items: List<LeaveItem> = emptyList(),
)

@HiltViewModel
class TimeoffViewModel @Inject constructor(
    private val session: SessionStore,
    private val getMyLeaves: GetMyLeaves,
    private val requestLeave: RequestLeave,
) : ViewModel() {
    private val _state = MutableStateFlow(TimeoffState(busy = true))
    val state: StateFlow<TimeoffState> = _state

    fun handleAction(action: TimeoffAction) {
        when (action) {
            TimeoffAction.Load -> refresh()
            is TimeoffAction.File -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch
                val user = session.userId.first() ?: return@launch
                _state.value = _state.value.copy(busy = true, error = null, message = null)
                _state.value = try {
                    requestLeave(company, user, action.type, action.from, action.to, action.reason)
                    TimeoffState(message = "Pengajuan terkirim.", items = getMyLeaves(company, user))
                } catch (e: ApiError) {
                    _state.value.copy(busy = false, error = e.message)
                } catch (e: Exception) {
                    _state.value.copy(busy = false, error = e.message ?: "Terjadi kesalahan.")
                }
            }
            TimeoffAction.ClearMessage -> _state.value = _state.value.copy(message = null, error = null)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val company = session.companyId.first()
            val user = session.userId.first()
            if (company == null || user == null) {
                _state.value = TimeoffState(error = "Pilih perusahaan dulu.")
                return@launch
            }
            _state.value = try {
                TimeoffState(items = getMyLeaves(company, user))
            } catch (e: ApiError) {
                TimeoffState(error = e.message)
            } catch (e: Exception) {
                TimeoffState(error = e.message ?: "Terjadi kesalahan.")
            }
        }
    }
}
