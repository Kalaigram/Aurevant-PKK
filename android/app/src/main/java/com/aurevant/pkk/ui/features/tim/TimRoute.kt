package com.aurevant.pkk.ui.features.tim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TimRoute(vm: TimViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(TimAction.Load) }
    TimScreen(state = state, onAction = vm::handleAction)
}
