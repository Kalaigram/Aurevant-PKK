package com.aurevant.pkk.ui.features.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurevant.pkk.core.ui.EmptyBox
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox
import com.aurevant.pkk.domain.model.ServiceOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(state: ServicesState, onAction: (ServicesAction) -> Unit) {
    var branchMenu by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var payFor by remember { mutableStateOf<ServiceOrder?>(null) }
    val bisaKasir = "transaksi.buat" in state.perms
    val bisaProses = "jasa.proses" in state.perms

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Order Jasa", style = MaterialTheme.typography.headlineSmall)
        Text("Baru → diproses → selesai → dibayar.", style = MaterialTheme.typography.bodySmall)
        ExposedDropdownMenuBox(branchMenu, { branchMenu = it }) {
            OutlinedTextField(
                state.branch?.let { "${it.code} — ${it.name}" } ?: "Pilih cabang",
                {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
                label = { Text("Cabang") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(branchMenu) },
            )
            ExposedDropdownMenu(branchMenu, { branchMenu = false }) {
                state.branches.forEach { b ->
                    DropdownMenuItem({ Text("${b.code} — ${b.name}") }, {
                        branchMenu = false
                        onAction(ServicesAction.PickBranch(b))
                    })
                }
            }
        }
        if (bisaKasir && state.branch != null) {
            Button(onClick = { showCreate = true }, Modifier.fillMaxWidth()) { Text("Buat order baru") }
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.branch == null -> EmptyBox("Pilih cabang dulu.")
            state.orders.isEmpty() -> EmptyBox("Tidak ada order jasa.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.orders) { o ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Order #${o.id}", style = MaterialTheme.typography.titleMedium)
                            Text("Status: ${o.status}" + (o.deposit?.let { " • DP: $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (bisaProses && o.status == "baru") {
                                    OutlinedButton(onClick = { onAction(ServicesAction.Process(o)) }) { Text("Proses") }
                                }
                                if (bisaProses && o.status == "diproses") {
                                    OutlinedButton(onClick = { onAction(ServicesAction.Complete(o)) }) { Text("Selesai") }
                                }
                                if (bisaKasir && o.status == "selesai") {
                                    OutlinedButton(onClick = { payFor = o }) { Text("Bayar") }
                                }
                                if (bisaKasir && (o.status == "baru" || o.status == "diproses")) {
                                    OutlinedButton(onClick = { onAction(ServicesAction.Cancel(o)) }) { Text("Batal") }
                                }
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    if (showCreate) {
        var itemId by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Order jasa baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(itemId, { itemId = it }, Modifier.fillMaxWidth(), label = { Text("ID jasa") })
                    OutlinedTextField(qty, { qty = it }, Modifier.fillMaxWidth(), label = { Text("Jumlah") })
                    OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Catatan (opsional)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAction(ServicesAction.Create(itemId.toIntOrNull() ?: 0, qty.toDoubleOrNull() ?: 0.0, note))
                    showCreate = false
                }) { Text("Buat") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Batal") } },
        )
    }

    payFor?.let { o ->
        var amount by remember { mutableStateOf(o.deposit ?: "") }
        AlertDialog(
            onDismissRequest = { payFor = null },
            title = { Text("Bayar order #${o.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DP yang sudah masuk otomatis dihitung.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Nominal") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAction(ServicesAction.Pay(o, amount.toDoubleOrNull() ?: 0.0, "tunai"))
                    payFor = null
                }) { Text("Bayar tunai") }
            },
            dismissButton = { TextButton(onClick = { payFor = null }) { Text("Batal") } },
        )
    }
}
