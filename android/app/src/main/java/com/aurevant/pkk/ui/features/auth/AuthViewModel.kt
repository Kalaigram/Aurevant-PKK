package com.aurevant.pkk.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.domain.model.AuthSession
import com.aurevant.pkk.domain.usecase.LoginUser
import com.aurevant.pkk.domain.usecase.RegisterUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AuthAction {
    data class Login(val login: String, val password: String) : AuthAction
    data class Register(val name: String, val email: String, val phone: String, val password: String, val confirm: String) : AuthAction
    data object ConsumeNav : AuthAction
}

data class AuthState(
    val busy: Boolean = false,
    val error: String? = null,
    val session: AuthSession? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUser: LoginUser,
    private val registerUser: RegisterUser,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    fun handleAction(action: AuthAction) {
        when (action) {
            is AuthAction.Login -> submit { loginUser(action.login, action.password) }
            is AuthAction.Register -> submit {
                registerUser(action.name, action.email, action.phone.ifEmpty { null }, action.password, action.confirm)
            }
            AuthAction.ConsumeNav -> _state.value = _state.value.copy(session = null)
        }
    }

    private fun submit(block: suspend () -> AuthSession) {
        _state.value = AuthState(busy = true)
        viewModelScope.launch {
            _state.value = try {
                AuthState(session = block())
            } catch (e: ApiError) {
                AuthState(error = e.message)
            } catch (e: Exception) {
                AuthState(error = e.message ?: "Terjadi kesalahan.")
            }
        }
    }
}
