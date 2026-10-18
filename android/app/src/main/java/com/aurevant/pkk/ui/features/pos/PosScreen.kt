package com.aurevant.pkk.ui.features.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aurevant.pkk.core.ui.EmptyBox
import com.aurevant.pkk.core.ui.LoadingBox
import com.aurevant.pkk.core.ui.rupiah
import com.aurevant.pkk.domain.usecase.CartCalculator

private val METHODS = listOf("tunai", "transfer", "qris", "ewallet", "dp")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    state: PosState,
    onAction: (PosAction) -> Unit,
    onPrint: () -> Unit,
) {
    val totals = CartCalculator.cartTotal(state.cart)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kasir", style = MaterialTheme.typography.headlineSmall)
        var open by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(open, { open = it }) {
            OutlinedTextField(
                state.branch?.name ?: "Pilih cabang",
                {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
                label = { Text("Cabang") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            )
            ExposedDropdownMenu(open, { open = false }) {
                state.branches.forEach { b ->
                    DropdownMenuItem({ Text("${b.name} (${b.code})") }, { open = false; onAction(PosAction.PickBranch(b)) })
                }
            }
        }
        if (state.branch == null) {
            if (state.busy) LoadingBox() else EmptyBox("Pilih cabang untuk mulai.")
            return
        }
        if (state.shift == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Belum ada shift terbuka.", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        if (state.openingCash == 0.0) "" else state.openingCash.toString(),
                        { onAction(PosAction.SetOpeningCash(it.toDoubleOrNull() ?: 0.0)) },
                        Modifier.fillMaxWidth(), label = { Text("Modal awal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    )
                    Button(onClick = { onAction(PosAction.Open) }, Modifier.fillMaxWidth(), enabled = !state.busy) {
                        Text("Buka shift")
                    }
                }
            }
        } else {
            OutlinedTextField(
                state.query, { onAction(PosAction.Search(it)) }, Modifier.fillMaxWidth(),
                label = { Text("Cari barang / jasa") }, singleLine = true,
            )
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.catalog) { item ->
                    Card(Modifier.fillMaxWidth(), onClick = { onAction(PosAction.Add(item)) }) {
                        Text("${item.name} · ${item.code}", Modifier.padding(12.dp))
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Keranjang (${state.cart.size})", style = MaterialTheme.typography.titleMedium)
                    state.cart.forEach { line ->
                        val t = CartCalculator.lineTotal(line)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${line.item.name} x${line.qty}", Modifier.weight(1f))
                            Text(rupiah(t.grand))
                            androidx.compose.material3.TextButton(onClick = { onAction(PosAction.Remove(line.item.id)) }) {
                                Text("Hapus")
                            }
                        }
                    }
                    Text("Total: ${rupiah(totals.grand)}", style = MaterialTheme.typography.titleMedium)
                    state.pays.forEachIndexed { i, p ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var mOpen by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(mOpen, { mOpen = it }, Modifier.weight(1f)) {
                                OutlinedTextField(p.method, {}, Modifier.menuAnchor(), readOnly = true, label = { Text("Cara") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mOpen) })
                                ExposedDropdownMenu(mOpen, { mOpen = false }) {
                                    METHODS.forEach { m ->
                                        DropdownMenuItem({ Text(m) }, { mOpen = false; onAction(PosAction.SetPayMethod(i, m)) })
                                    }
                                }
                            }
                            OutlinedTextField(
                                if (p.amount == 0.0) "" else p.amount.toString(),
                                { onAction(PosAction.SetPayAmount(i, it.toDoubleOrNull() ?: 0.0)) },
                                Modifier.weight(1f), label = { Text("Nominal") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAction(PosAction.AddPayRow) }) { Text("+ Cara bayar") }
                        Button(
                            onClick = { onAction(PosAction.Pay) },
                            enabled = !state.busy && state.cart.isNotEmpty(),
                        ) { Text("Bayar") }
                    }
                }
            }
        }
        state.sale?.let { sale ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nota ${sale.number} (${sale.status})", style = MaterialTheme.typography.titleMedium)
                    Text("Total ${rupiah(sale.grandTotal)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAction(PosAction.Receipt) }) { Text("Simpan PDF") }
                        if (state.receiptFile != null) {
                            Button(onClick = onPrint) { Text("Cetak") }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
