package com.aurevant.pkk.ui.features.tim

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimScreen(state: TimState, onAction: (TimAction) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var branchMenu by remember { mutableStateOf(false) }
    var correctFor by remember { mutableStateOf<Int?>(null) }
    val bisaPutus = "absensi.approve" in state.perms
    val bisaKoreksi = "absensi.koreksi" in state.perms

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tim & Kehadiran", style = MaterialTheme.typography.headlineSmall)
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
                        onAction(TimAction.PickBranch(b))
                    })
                }
            }
        }
        TabRow(tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Hadir") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Cuti") })
            Tab(tab == 2, { tab = 2 }, text = { Text("Jadwal") })
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.branch == null && tab < 2 -> EmptyBox("Pilih cabang dulu.")
            tab == 0 -> {
                if (state.hadir.isEmpty()) EmptyBox("Belum ada data kehadiran hari ini.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.hadir) { a ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Status: ${a.status ?: "-"}", style = MaterialTheme.typography.titleSmall)
                                Text("Masuk: ${a.clockInAt ?: "-"} • Pulang: ${a.clockOutAt ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                if (bisaKoreksi) {
                                    OutlinedButton(onClick = { correctFor = a.id }) { Text("Koreksi") }
                                }
                            }
                        }
                    }
                }
            }
            tab == 1 -> {
                if (state.cuti.isEmpty()) EmptyBox("Tidak ada pengajuan cuti menunggu.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.cuti) { c ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${c.type} • ${c.from} → ${c.to}", style = MaterialTheme.typography.titleSmall)
                                if (bisaPutus) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onAction(TimAction.Decide(c, true)) }) { Text("Setujui") }
                                        OutlinedButton(onClick = { onAction(TimAction.Decide(c, false)) }) { Text("Tolak") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                if (state.jadwal.isEmpty()) EmptyBox("Jadwal 7 hari ke depan kosong.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.jadwal) { j ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(j.date, style = MaterialTheme.typography.titleSmall)
                                Text(if (j.libur) "Libur" else "${j.shiftName ?: "-"}${j.jam?.let { " • $it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    correctFor?.let { id ->
        var status by remember { mutableStateOf("hadir") }
        AlertDialog(
            onDismissRequest = { correctFor = null },
            title = { Text("Koreksi kehadiran") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Isi status yang benar. Alasan tercatat otomatis.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(status, { status = it }, Modifier.fillMaxWidth(), label = { Text("Status (hadir/terlambat/alpa/izin/sakit/cuti)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAction(TimAction.Correct(id, status))
                    correctFor = null
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { correctFor = null }) { Text("Batal") } },
        )
    }
}
