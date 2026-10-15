package com.aurevant.pkk.ui.features.slip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Slip
import com.aurevant.pkk.domain.usecase.GetSlip
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SlipAction {
    data class Load(val period: Int) : SlipAction
}

data class SlipState(
    val busy: Boolean = false,
    val error: String? = null,
    val slip: Slip? = null,
)

@HiltViewModel
class SlipViewModel @Inject constructor(
    private val session: SessionStore,
    private val getSlip: GetSlip,
) : ViewModel() {
    private val _state = MutableStateFlow(SlipState())
    val state: StateFlow<SlipState> = _state

    fun handleAction(action: SlipAction) {
        when (action) {
            is SlipAction.Load -> viewModelScope.launch {
                val company = session.companyId.first()
                val user = session.userId.first()
                if (company == null || user == null) {
                    _state.value = SlipState(error = "Pilih perusahaan dulu.")
                    return@launch
                }
                _state.value = SlipState(busy = true)
                _state.value = try {
                    SlipState(slip = getSlip(company, action.period, user))
                } catch (e: ApiError) {
                    SlipState(error = e.message)
                } catch (e: Exception) {
                    SlipState(error = e.message ?: "Terjadi kesalahan.")
                }
            }
        }
    }
}
