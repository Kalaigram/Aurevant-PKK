package com.aurevant.pkk.ui.features.companies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.push.PushTokenSync
import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.usecase.GetCompanies
import com.aurevant.pkk.domain.usecase.LogoutUser
import com.aurevant.pkk.domain.usecase.SaveCompany
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface CompaniesAction {
    data object Load : CompaniesAction
    data class Pick(val company: Company) : CompaniesAction
    data object Logout : CompaniesAction
}

data class CompaniesState(
    val busy: Boolean = false,
    val error: String? = null,
    val items: List<Company> = emptyList(),
    val picked: Company? = null,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class CompaniesViewModel @Inject constructor(
    private val getCompanies: GetCompanies,
    private val saveCompany: SaveCompany,
    private val logoutUser: LogoutUser,
    private val pushTokenSync: PushTokenSync,
) : ViewModel() {
    private val _state = MutableStateFlow(CompaniesState(busy = true))
    val state: StateFlow<CompaniesState> = _state

    fun handleAction(action: CompaniesAction) {
        when (action) {
            CompaniesAction.Load -> load()
            is CompaniesAction.Pick -> viewModelScope.launch {
                saveCompany(action.company)
                pushTokenSync.sync()
                _state.value = _state.value.copy(picked = action.company)
            }
            CompaniesAction.Logout -> viewModelScope.launch {
                logoutUser()
                _state.value = _state.value.copy(loggedOut = true)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                CompaniesState(items = getCompanies())
            } catch (e: ApiError) {
                CompaniesState(error = e.message)
            } catch (e: Exception) {
                CompaniesState(error = e.message ?: "Terjadi kesalahan.")
            }
        }
    }
}
