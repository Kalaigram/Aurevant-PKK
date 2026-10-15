package com.aurevant.pkk.ui.features.sales

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
import com.aurevant.pkk.core.ui.rupiah
import com.aurevant.pkk.domain.model.Sale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(state: SalesState, onAction: (SalesAction) -> Unit) {
    var voidFor by remember { mutableStateOf<Sale?>(null) }
    var returFor by remember { mutableStateOf<Sale?>(null) }
    var reason by remember { mutableStateOf("") }
    var branchMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Nota", style = MaterialTheme.typography.headlineSmall)
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
                        onAction(SalesAction.PickBranch(b))
                    })
                }
            }
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.branch == null -> EmptyBox("Pilih cabang dulu untuk melihat nota.")
            state.notas.isEmpty() -> EmptyBox("Belum ada nota di cabang ini.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.notas) { n ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(n.number, style = MaterialTheme.typography.titleMedium)
                            Text("Status: ${n.status} • Total: ${rupiah(n.grandTotal)}", style = MaterialTheme.typography.bodySmall)
                            if (n.status == "lunas") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { reason = ""; voidFor = n }) { Text("Void") }
                                    OutlinedButton(onClick = { reason = ""; returFor = n }) { Text("Retur") }
                                }
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    voidFor?.let { sale ->
        AlertDialog(
            onDismissRequest = { voidFor = null },
            title = { Text("Void ${sale.number}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Alasan wajib diisi dan tercatat di audit.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("Alasan") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAction(SalesAction.Void(sale, reason))
                    voidFor = null
                }) { Text("Kirim") }
            },
            dismissButton = { TextButton(onClick = { voidFor = null }) { Text("Batal") } },
        )
    }

    returFor?.let { sale ->
        var itemId by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1") }
        AlertDialog(
            onDismissRequest = { returFor = null },
            title = { Text("Ajukan retur ${sale.number}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pengajuan diteruskan ke atasan. Eksekusi akhir oleh manajer.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(itemId, { itemId = it }, Modifier.fillMaxWidth(), label = { Text("ID barang") })
                    OutlinedTextField(qty, { qty = it }, Modifier.fillMaxWidth(), label = { Text("Jumlah") })
                    OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("Alasan") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAction(SalesAction.Retur(sale, itemId.toIntOrNull() ?: 0, qty.toDoubleOrNull() ?: 0.0, "layak", "tunai", reason))
                    returFor = null
                }) { Text("Ajukan") }
            },
            dismissButton = { TextButton(onClick = { returFor = null }) { Text("Batal") } },
        )
    }
}
