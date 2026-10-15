package com.aurevant.pkk.ui.features.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginRoute(vm: AuthViewModel = hiltViewModel(), onDone: () -> Unit, onRegister: () -> Unit) {
    val state by vm.state.collectAsState()
    state.session?.let {
        LaunchedEffect(it) {
            vm.handleAction(AuthAction.ConsumeNav)
            onDone()
        }
    }
    LoginScreen(state, vm::handleAction, onRegister)
}

@Composable
fun RegisterRoute(vm: AuthViewModel = hiltViewModel(), onDone: () -> Unit, onLogin: () -> Unit) {
    val state by vm.state.collectAsState()
    state.session?.let {
        LaunchedEffect(it) {
            vm.handleAction(AuthAction.ConsumeNav)
            onDone()
        }
    }
    RegisterScreen(state, vm::handleAction, onLogin)
}
