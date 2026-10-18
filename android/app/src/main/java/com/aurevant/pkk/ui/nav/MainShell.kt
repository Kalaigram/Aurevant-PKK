package com.aurevant.pkk.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.aurevant.pkk.domain.usecase.MenuPolicy
import com.aurevant.pkk.ui.ShellViewModel
import com.aurevant.pkk.ui.features.approvals.ApprovalsRoute
import com.aurevant.pkk.ui.features.attendance.AttendanceRoute
import com.aurevant.pkk.ui.features.card.CardRoute
import com.aurevant.pkk.ui.features.dashboard.DashboardRoute
import com.aurevant.pkk.ui.features.gaji.GajiRoute
import com.aurevant.pkk.ui.features.gudang.GudangRoute
import com.aurevant.pkk.ui.features.keuangan.KeuanganRoute
import com.aurevant.pkk.ui.features.notifications.NotificationsRoute
import com.aurevant.pkk.ui.features.pelanggan.PelangganRoute
import com.aurevant.pkk.ui.features.pos.PosRoute
import com.aurevant.pkk.ui.features.sales.SalesRoute
import com.aurevant.pkk.ui.features.services.ServicesRoute
import com.aurevant.pkk.ui.features.slip.SlipRoute
import com.aurevant.pkk.ui.features.tim.TimRoute
import com.aurevant.pkk.ui.features.timeoff.TimeoffRoute

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val ALL_TABS = listOf(
    Tab(Destinations.DASHBOARD, "Dasbor", Icons.Filled.Dashboard),
    Tab(Destinations.ATTENDANCE, "Absensi", Icons.Filled.Fingerprint),
    Tab(Destinations.POS, "Kasir", Icons.Filled.PointOfSale),
    Tab(Destinations.NOTA, "Nota", Icons.Filled.ReceiptLong),
    Tab(Destinations.JASA, "Jasa", Icons.Filled.Engineering),
    Tab(Destinations.GUDANG, "Gudang", Icons.Filled.Inventory),
    Tab(Destinations.APPROVALS, "Setuju", Icons.Filled.Checklist),
    Tab(Destinations.TIM, "Tim", Icons.Filled.Group),
    Tab(Destinations.KEUANGAN, "Keuangan", Icons.Filled.AccountBalanceWallet),
    Tab(Destinations.GAJI, "Gaji", Icons.Filled.Payments),
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
    val perms by shell.perms.collectAsState()
    LaunchedEffect(Unit) { shell.load() }
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
    val barRoutes = MenuPolicy.bottomTabs(perms)
    val barTabs = ALL_TABS.filter { it.route in barRoutes }
    val extraTabs = ALL_TABS.filter { MenuPolicy.can(it.route, perms) && it.route !in barRoutes }
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
                            if (MenuPolicy.can(Destinations.PELANGGAN, perms)) {
                                DropdownMenuItem({ Text("Pelanggan") }, { go(Destinations.PELANGGAN) })
                            }
                            extraTabs.forEach { t ->
                                DropdownMenuItem({ Text(t.label) }, { go(t.route) })
                            }
                            DropdownMenuItem({ Text("Ganti perusahaan") }, onSwitchCompany)
                            DropdownMenuItem({ Text("Keluar") }, { menu = false; shell.logout() })
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                barTabs.forEach { tab ->
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
            composable(Destinations.NOTA) { SalesRoute() }
            composable(Destinations.JASA) { ServicesRoute() }
            composable(Destinations.GUDANG) { GudangRoute() }
            composable(Destinations.APPROVALS) { ApprovalsRoute() }
            composable(Destinations.TIM) { TimRoute() }
            composable(Destinations.KEUANGAN) { KeuanganRoute() }
            composable(Destinations.GAJI) { GajiRoute() }
            composable(Destinations.NOTIFICATIONS) { NotificationsRoute() }
            composable(Destinations.LEAVES) { TimeoffRoute() }
            composable(Destinations.SLIP) { SlipRoute() }
            composable(Destinations.CARD) { CardRoute() }
            composable(Destinations.PELANGGAN) { PelangganRoute() }
        }
    }
}
