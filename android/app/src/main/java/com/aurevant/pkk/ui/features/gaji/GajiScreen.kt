package com.aurevant.pkk.ui.features.gaji

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aurevant.pkk.core.ui.EmptyBox
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox

@Composable
fun GajiRoute(vm: GajiViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.handleAction(GajiAction.Load) }
    GajiScreen(state = state, onAction = vm::handleAction)
}

@Composable
fun GajiScreen(state: GajiState, onAction: (GajiAction) -> Unit) {
    val bisaSetuju = "gaji.setujui" in state.perms
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Penggajian", style = MaterialTheme.typography.headlineSmall)
        Text("Draft → review → disetujui → dibayar. Periode dibayar terkunci.", style = MaterialTheme.typography.bodySmall)
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.periods.isEmpty() -> EmptyBox("Belum ada periode gaji.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.periods) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Text("${p.from ?: "-"} → ${p.to ?: "-"} • Status: ${p.status}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (p.status == "draft") {
                                    Button(onClick = { onAction(GajiAction.Act(p, "calculate")) }) { Text("Hitung") }
                                }
                                if (p.status == "draft") {
                                    OutlinedButton(onClick = { onAction(GajiAction.Act(p, "review")) }) { Text("Review") }
                                }
                                if (p.status == "review" && bisaSetuju) {
                                    Button(onClick = { onAction(GajiAction.Act(p, "approve")) }) { Text("Setujui") }
                                }
                                if (p.status == "disetujui") {
                                    Button(onClick = { onAction(GajiAction.Act(p, "pay")) }) { Text("Bayar") }
                                }
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
