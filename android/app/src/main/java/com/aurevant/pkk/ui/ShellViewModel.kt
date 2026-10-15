package com.aurevant.pkk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.domain.usecase.LogoutUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ShellViewModel @Inject constructor(private val logoutUser: LogoutUser) : ViewModel() {
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut

    fun logout() {
        viewModelScope.launch {
            logoutUser()
            _loggedOut.value = true
        }
    }
}
