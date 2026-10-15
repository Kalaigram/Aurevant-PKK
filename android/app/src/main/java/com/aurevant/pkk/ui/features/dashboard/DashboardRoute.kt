package com.aurevant.pkk.ui.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DashboardRoute(
    vm: DashboardViewModel = hiltViewModel(),
    onOpen: (String) -> Unit,
    onSwitchCompany: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(DashboardAction.Load) }
    if (state.loggedOut) {
        LaunchedEffect(Unit) { onLoggedOut() }
    }
    DashboardScreen(state, vm::handleAction, onOpen, onSwitchCompany)
}
