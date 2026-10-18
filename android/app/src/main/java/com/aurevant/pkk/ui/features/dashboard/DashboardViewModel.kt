package com.aurevant.pkk.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Dashboard
import com.aurevant.pkk.domain.usecase.GetDashboard
import com.aurevant.pkk.domain.usecase.GetMyPermissions
import com.aurevant.pkk.domain.usecase.LogoutUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface DashboardAction {
    data object Load : DashboardAction
    data object Logout : DashboardAction
}

data class DashboardState(
    val busy: Boolean = false,
    val error: String? = null,
    val companyId: Int? = null,
    val data: Dashboard? = null,
    val permissions: List<String> = emptyList(),
    val loggedOut: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val session: SessionStore,
    private val getDashboard: GetDashboard,
    private val getMyPermissions: GetMyPermissions,
    private val logoutUser: LogoutUser,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState(busy = true))
    val state: StateFlow<DashboardState> = _state

    fun handleAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = DashboardState(error = "Pilih perusahaan dulu.")
                }
                _state.value = try {
                    DashboardState(
                        companyId = company,
                        data = getDashboard(company),
                        permissions = getMyPermissions(company),
                    )
                } catch (e: ApiError) {
                    DashboardState(companyId = company, error = e.message)
                } catch (e: Exception) {
                    DashboardState(companyId = company, error = e.message ?: "Terjadi kesalahan.")
                }
            }
            DashboardAction.Logout -> viewModelScope.launch {
                logoutUser()
                _state.value = _state.value.copy(loggedOut = true)
            }
        }
    }
}
