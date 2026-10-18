package com.aurevant.pkk.ui.features.keuangan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun KeuanganRoute(vm: KeuanganViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(KeuanganAction.Load) }
    KeuanganScreen(state = state, onAction = vm::handleAction)
}
