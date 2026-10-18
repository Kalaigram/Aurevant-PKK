package com.aurevant.pkk.ui.features.approvals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ApprovalsRoute(vm: ApprovalsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(ApprovalsAction.Load) }
    ApprovalsScreen(state, vm::handleAction)
}
