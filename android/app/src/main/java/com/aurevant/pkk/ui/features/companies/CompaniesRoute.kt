package com.aurevant.pkk.ui.features.companies

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CompaniesRoute(vm: CompaniesViewModel = hiltViewModel(), onPicked: () -> Unit, onLoggedOut: () -> Unit) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(CompaniesAction.Load) }
    state.picked?.let { LaunchedEffect(it) { onPicked() } }
    if (state.loggedOut) {
        LaunchedEffect(Unit) { onLoggedOut() }
    }
    CompaniesScreen(state, vm::handleAction)
}
