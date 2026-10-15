package com.aurevant.pkk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aurevant.pkk.ui.features.auth.LoginRoute
import com.aurevant.pkk.ui.features.auth.RegisterRoute
import com.aurevant.pkk.ui.features.companies.CompaniesRoute
import com.aurevant.pkk.ui.nav.Destinations
import com.aurevant.pkk.ui.nav.MainShell
import com.aurevant.pkk.ui.theme.AurevantandroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AurevantandroidTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = Destinations.LOGIN) {
                    composable(Destinations.LOGIN) {
                        LoginRoute(
                            onDone = { nav.navigate(Destinations.COMPANIES) { popUpTo(Destinations.LOGIN) { inclusive = true } } },
                            onRegister = { nav.navigate(Destinations.REGISTER) },
                        )
                    }
                    composable(Destinations.REGISTER) {
                        RegisterRoute(
                            onDone = { nav.navigate(Destinations.COMPANIES) { popUpTo(Destinations.LOGIN) { inclusive = true } } },
                            onLogin = { nav.popBackStack() },
                        )
                    }
                    composable(Destinations.COMPANIES) {
                        CompaniesRoute(
                            onPicked = { nav.navigate("shell") { popUpTo(Destinations.COMPANIES) { inclusive = true } } },
                            onLoggedOut = { nav.navigate(Destinations.LOGIN) { popUpTo(0) } },
                        )
                    }
                    composable("shell") {
                        MainShell(
                            onLoggedOut = { nav.navigate(Destinations.LOGIN) { popUpTo(0) } },
                            onSwitchCompany = { nav.navigate(Destinations.COMPANIES) },
                        )
                    }
                }
            }
        }
    }
}
