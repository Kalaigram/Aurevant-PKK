package com.aurevant.pkk.ui.features.timeoff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TimeoffRoute(vm: TimeoffViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(TimeoffAction.Load) }
    TimeoffScreen(state, vm::handleAction)
}
