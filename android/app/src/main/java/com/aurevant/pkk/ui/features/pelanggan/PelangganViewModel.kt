package com.aurevant.pkk.ui.features.pelanggan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Customer
import com.aurevant.pkk.domain.usecase.GetCustomers
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PelangganState(
    val busy: Boolean = false,
    val error: String? = null,
    val items: List<Customer> = emptyList(),
    val query: String = "",
)

@HiltViewModel
class PelangganViewModel @Inject constructor(
    private val session: SessionStore,
    private val getCustomers: GetCustomers,
) : ViewModel() {
    private val _state = MutableStateFlow(PelangganState(busy = true))
    val state: StateFlow<PelangganState> = _state

    init {
        viewModelScope.launch {
            val company = session.companyId.first() ?: return@launch run {
                _state.value = PelangganState(error = "Pilih perusahaan dulu.")
            }
            try {
                _state.value = PelangganState(items = getCustomers(company))
            } catch (e: Exception) {
                _state.value = PelangganState(error = (e as? ApiError)?.message ?: e.message)
            }
        }
    }

    fun search(q: String) {
        _state.value = _state.value.copy(query = q)
    }
}
