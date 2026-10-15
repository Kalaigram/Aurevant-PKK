package com.aurevant.pkk.ui.features.gudang

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GudangRoute(vm: GudangViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(GudangAction.Load) }
    GudangScreen(state = state, onAction = vm::handleAction)
}
