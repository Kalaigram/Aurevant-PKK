package com.aurevant.pkk.ui.features.sales

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SalesRoute(vm: SalesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(SalesAction.Load) }
    SalesScreen(state = state, onAction = vm::handleAction)
}
