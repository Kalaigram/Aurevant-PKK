package com.aurevant.pkk.ui.features.keuangan

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
import com.aurevant.pkk.core.ui.rupiah
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.ExpenseCategory
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeuanganScreen(state: KeuanganState, onAction: (KeuanganAction) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var showExpense by remember { mutableStateOf(false) }
    var payBillFor by remember { mutableStateOf<Int?>(null) }
    var payRecvFor by remember { mutableStateOf<Int?>(null) }
    val bisaInput = "biaya.input" in state.perms
    val bisaSetuju = "biaya.setujui" in state.perms

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Keuangan", style = MaterialTheme.typography.headlineSmall)
        state.profit?.let { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Laba bulan berjalan: ${rupiah(p.bersih)}", style = MaterialTheme.typography.titleSmall)
                    Text("Pendapatan ${rupiah(p.pendapatan)} • HPP ${rupiah(p.hpp)} • Beban ${rupiah(p.beban)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        TabRow(tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Biaya") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Hutang") })
            Tab(tab == 2, { tab = 2 }, text = { Text("Piutang") })
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            tab == 0 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((label, key) in listOf("Draft" to "draft", "Disetujui" to "disetujui", "Semua" to null)) {
                        OutlinedButton(onClick = { onAction(KeuanganAction.FilterStatus(key)) }) {
                            Text(label, color = if (state.filterStatus == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (bisaInput) {
                    Button(onClick = { showExpense = true }, Modifier.fillMaxWidth()) { Text("Catat biaya") }
                }
                if (state.expenses.isEmpty()) EmptyBox("Tidak ada biaya.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.expenses) { e ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${e.categoryName ?: "Biaya"} • ${rupiah(e.amount)}", style = MaterialTheme.typography.titleSmall)
                                Text("${e.date ?: "-"} • ${e.status} • ${e.description ?: ""}", style = MaterialTheme.typography.bodySmall)
                                if (bisaSetuju && e.status == "draft") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onAction(KeuanganAction.ExpenseAct(e.id, true)) }) { Text("Setujui") }
                                        OutlinedButton(onClick = { onAction(KeuanganAction.ExpenseAct(e.id, false)) }) { Text("Tolak") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            tab == 1 -> {
                if (state.bills.isEmpty()) EmptyBox("Tidak ada hutang belum lunas.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.bills) { b ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Tagihan #${b.id}", style = MaterialTheme.typography.titleSmall)
                                Text("Sisa: ${rupiah(b.remaining)} dari ${rupiah(b.amount)}" + (b.dueAt?.let { " • jatuh tempo $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                                if (bisaSetuju) {
                                    OutlinedButton(onClick = { payBillFor = b.id }) { Text("Bayar") }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                if (state.receivables.isEmpty()) EmptyBox("Tidak ada piutang belum lunas.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.receivables) { r ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Bon #${r.id}", style = MaterialTheme.typography.titleSmall)
                                Text("Sisa: ${rupiah(r.remaining)} dari ${rupiah(r.amount)}" + (r.dueAt?.let { " • jatuh tempo $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                                if (bisaSetuju) {
                                    OutlinedButton(onClick = { payRecvFor = r.id }) { Text("Terima cicilan") }
                                }
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    if (showExpense) {
        var branch by remember { mutableStateOf<Branch?>(null) }
        var cat by remember { mutableStateOf<ExpenseCategory?>(null) }
        var branchMenu by remember { mutableStateOf(false) }
        var catMenu by remember { mutableStateOf(false) }
        var amount by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExpense = false },
            title = { Text("Catat biaya") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(branchMenu, { branchMenu = it }) {
                        OutlinedTextField(branch?.name ?: "Cabang", {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(branchMenu) })
                        ExposedDropdownMenu(branchMenu, { branchMenu = false }) {
                            state.branches.forEach { b ->
                                DropdownMenuItem({ Text(b.name) }, { branch = b; branchMenu = false })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(catMenu, { catMenu = it }) {
                        OutlinedTextField(cat?.name ?: "Kategori", {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catMenu) })
                        ExposedDropdownMenu(catMenu, { catMenu = false }) {
                            state.categories.forEach { c ->
                                DropdownMenuItem({ Text(c.name) }, { cat = c; catMenu = false })
                            }
                        }
                    }
                    OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Nominal") })
                    OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), label = { Text("Keterangan") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val b = branch
                    val c = cat
                    if (b != null && c != null) {
                        onAction(KeuanganAction.Expense(b.id, c.id, amount.toDoubleOrNull() ?: 0.0, LocalDate.now().toString(), desc))
                    }
                    showExpense = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showExpense = false }) { Text("Batal") } },
        )
    }

    payBillFor?.let { id ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { payBillFor = null },
            title = { Text("Bayar hutang #$id") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Parsial boleh. Server menolak bila melebihi sisa.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Nominal") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAction(KeuanganAction.Bill(id, amount.toDoubleOrNull() ?: 0.0))
                    payBillFor = null
                }) { Text("Bayar") }
            },
            dismissButton = { TextButton(onClick = { payBillFor = null }) { Text("Batal") } },
        )
    }

    payRecvFor?.let { id ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { payRecvFor = null },
            title = { Text("Terima cicilan #$id") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Nominal") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAction(KeuanganAction.Receivable(id, amount.toDoubleOrNull() ?: 0.0))
                    payRecvFor = null
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { payRecvFor = null }) { Text("Batal") } },
        )
    }
}
