package com.aurevant.pkk.ui.features.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ServicesRoute(vm: ServicesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(ServicesAction.Load) }
    ServicesScreen(state = state, onAction = vm::handleAction)
}
