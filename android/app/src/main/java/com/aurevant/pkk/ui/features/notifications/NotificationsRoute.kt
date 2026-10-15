package com.aurevant.pkk.ui.features.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NotificationsRoute(vm: NotificationsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(NotificationsAction.Load) }
    NotificationsScreen(state, vm::handleAction, vm::toggleUnread)
}
