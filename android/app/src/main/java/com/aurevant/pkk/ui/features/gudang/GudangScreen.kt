package com.aurevant.pkk.ui.features.gudang

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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurevant.pkk.core.ui.EmptyBox
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.OpnameInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GudangScreen(state: GudangState, onAction: (GudangAction) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var branchMenu by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showOpname by remember { mutableStateOf(false) }
    val bisaTransfer = "stok.transfer" in state.perms
    val bisaOpname = "stok.opname" in state.perms

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Gudang", style = MaterialTheme.typography.headlineSmall)
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
                        onAction(GudangAction.PickBranch(b))
                    })
                }
            }
        }
        TabRow(tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Stok") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Transfer") })
            Tab(tab == 2, { tab = 2 }, text = { Text("Opname") })
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.branch == null -> EmptyBox("Pilih cabang dulu.")
            tab == 0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.stock.isEmpty()) item { EmptyBox("Stok kosong.") }
                items(state.stock) { s ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(s.itemName ?: "Item #${s.itemId}", style = MaterialTheme.typography.titleSmall)
                            Text("Qty: ${s.qty}" + (s.avgCost?.let { " • Modal rata-rata: $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            tab == 1 -> {
                if (bisaTransfer) {
                    Button(onClick = { showTransfer = true }, Modifier.fillMaxWidth()) { Text("Transfer baru") }
                }
                if (state.transfers.isEmpty()) EmptyBox("Tidak ada transfer.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.transfers) { t ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Transfer #${t.id}", style = MaterialTheme.typography.titleSmall)
                                Text("Status: ${t.status}", style = MaterialTheme.typography.bodySmall)
                                if (bisaTransfer) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (aksi in nextTransfer(t.status)) {
                                            OutlinedButton(onClick = { onAction(GudangAction.TransferAct(t, aksi)) }) {
                                                Text(aksiLabel(aksi))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                if (bisaOpname) {
                    Button(onClick = { showOpname = true }, Modifier.fillMaxWidth()) { Text("Opname baru") }
                }
                if (state.opnames.isEmpty()) EmptyBox("Tidak ada opname.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.opnames) { o ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Opname #${o.id}", style = MaterialTheme.typography.titleSmall)
                                Text("Status: ${o.status} • ${o.lines.size} baris", style = MaterialTheme.typography.bodySmall)
                                if (bisaOpname) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (aksi in nextOpname(o.status)) {
                                            OutlinedButton(onClick = { onAction(GudangAction.OpnameAct(o, aksi)) }) {
                                                Text(aksiLabel(aksi))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    if (showTransfer) {
        var toMenu by remember { mutableStateOf(false) }
        var to by remember { mutableStateOf<Branch?>(null) }
        var itemId by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTransfer = false },
            title = { Text("Transfer baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(toMenu, { toMenu = it }) {
                        OutlinedTextField(to?.name ?: "Cabang tujuan", {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(toMenu) })
                        ExposedDropdownMenu(toMenu, { toMenu = false }) {
                            state.branches.filter { it.id != state.branch?.id }.forEach { b ->
                                DropdownMenuItem({ Text("${b.code} — ${b.name}") }, {
                                    to = b
                                    toMenu = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(itemId, { itemId = it }, Modifier.fillMaxWidth(), label = { Text("ID barang") })
                    OutlinedTextField(qty, { qty = it }, Modifier.fillMaxWidth(), label = { Text("Jumlah") })
                    OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Catatan (opsional)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    to?.let { onAction(GudangAction.Transfer(it, itemId.toIntOrNull() ?: 0, qty.toDoubleOrNull() ?: 0.0, note)) }
                    showTransfer = false
                }) { Text("Buat") }
            },
            dismissButton = { TextButton(onClick = { showTransfer = false }) { Text("Batal") } },
        )
    }

    if (showOpname) {
        var itemId by remember { mutableStateOf("") }
        var counted by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showOpname = false },
            title = { Text("Opname baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Satu baris per dialog. Ulangi untuk tiap barang.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(itemId, { itemId = it }, Modifier.fillMaxWidth(), label = { Text("ID barang") })
                    OutlinedTextField(counted, { counted = it }, Modifier.fillMaxWidth(), label = { Text("Hasil hitung fisik") })
                    OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Catatan (opsional)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAction(GudangAction.Opname(listOf(OpnameInput(itemId.toIntOrNull() ?: 0, counted.toDoubleOrNull() ?: 0.0)), note))
                    showOpname = false
                }) { Text("Buat") }
            },
            dismissButton = { TextButton(onClick = { showOpname = false }) { Text("Batal") } },
        )
    }
}

private fun nextTransfer(status: String): List<String> = when (status) {
    "draft" -> listOf("submit", "cancel")
    "diajukan" -> listOf("approve", "cancel")
    "disetujui" -> listOf("ship")
    "dikirim" -> listOf("receive")
    else -> emptyList()
}

private fun nextOpname(status: String): List<String> = when (status) {
    "draft" -> listOf("submit")
    "diajukan" -> listOf("approve", "reject")
    else -> emptyList()
}

private fun aksiLabel(aksi: String): String = when (aksi) {
    "submit" -> "Ajukan"
    "approve" -> "Setujui"
    "reject" -> "Tolak"
    "ship" -> "Kirim"
    "receive" -> "Terima"
    "cancel" -> "Batal"
    else -> aksi
}
