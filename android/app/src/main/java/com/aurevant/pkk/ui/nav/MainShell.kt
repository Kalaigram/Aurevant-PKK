package com.aurevant.pkk.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aurevant.pkk.ui.ShellViewModel
import com.aurevant.pkk.ui.features.approvals.ApprovalsRoute
import com.aurevant.pkk.ui.features.attendance.AttendanceRoute
import com.aurevant.pkk.ui.features.card.CardRoute
import com.aurevant.pkk.ui.features.dashboard.DashboardRoute
import com.aurevant.pkk.ui.features.notifications.NotificationsRoute
import com.aurevant.pkk.ui.features.pos.PosRoute
import com.aurevant.pkk.ui.features.slip.SlipRoute
import com.aurevant.pkk.ui.features.timeoff.TimeoffRoute

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Destinations.DASHBOARD, "Dasbor", Icons.Filled.Dashboard),
    Tab(Destinations.ATTENDANCE, "Absensi", Icons.Filled.Fingerprint),
    Tab(Destinations.POS, "Kasir", Icons.Filled.PointOfSale),
    Tab(Destinations.APPROVALS, "Setuju", Icons.Filled.Checklist),
    Tab(Destinations.NOTIFICATIONS, "Notifikasi", Icons.Filled.Notifications),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    onLoggedOut: () -> Unit,
    onSwitchCompany: () -> Unit,
    shell: ShellViewModel = hiltViewModel(),
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    var menu by remember { mutableStateOf(false) }
    val loggedOut by shell.loggedOut.collectAsState()
    if (loggedOut) {
        LaunchedEffect(Unit) { onLoggedOut() }
    }
    fun go(route: String) {
        menu = false
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aurevant PKK") },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("Cuti saya") }, { go(Destinations.LEAVES) })
                            DropdownMenuItem({ Text("Slip gaji") }, { go(Destinations.SLIP) })
                            DropdownMenuItem({ Text("Kartu karyawan") }, { go(Destinations.CARD) })
                            DropdownMenuItem({ Text("Ganti perusahaan") }, onSwitchCompany)
                            DropdownMenuItem({ Text("Keluar") }, { menu = false; shell.logout() })
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { go(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { inner ->
        NavHost(nav, startDestination = Destinations.DASHBOARD, Modifier.padding(inner)) {
            composable(Destinations.DASHBOARD) {
                DashboardRoute(onOpen = { go(it) }, onSwitchCompany = onSwitchCompany, onLoggedOut = onLoggedOut)
            }
            composable(Destinations.ATTENDANCE) { AttendanceRoute() }
            composable(Destinations.POS) { PosRoute() }
            composable(Destinations.APPROVALS) { ApprovalsRoute() }
            composable(Destinations.NOTIFICATIONS) { NotificationsRoute() }
            composable(Destinations.LEAVES) { TimeoffRoute() }
            composable(Destinations.SLIP) { SlipRoute() }
            composable(Destinations.CARD) { CardRoute() }
        }
    }
}
