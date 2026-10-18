package com.aurevant.pkk.ui.features.slip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SlipRoute(vm: SlipViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    SlipScreen(state, vm::handleAction)
}
