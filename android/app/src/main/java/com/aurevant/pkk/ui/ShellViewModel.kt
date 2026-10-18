package com.aurevant.pkk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.usecase.GetMyPermissions
import com.aurevant.pkk.domain.usecase.LogoutUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val session: SessionStore,
    private val getMyPermissions: GetMyPermissions,
    private val logoutUser: LogoutUser,
) : ViewModel() {
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut

    private val _perms = MutableStateFlow<Set<String>>(emptySet())
    val perms: StateFlow<Set<String>> = _perms

    fun load() {
        viewModelScope.launch {
            val company = session.companyId.first() ?: return@launch
            try {
                _perms.value = getMyPermissions(company).toSet()
            } catch (_: Exception) {
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUser()
            _loggedOut.value = true
        }
    }
}
