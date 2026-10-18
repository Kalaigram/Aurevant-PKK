package com.aurevant.pkk.ui.features.attendance

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
import androidx.compose.ui.unit.dp
import com.aurevant.pkk.core.ui.EmptyBox
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    state: AttendanceState,
    onAction: (AttendanceAction) -> Unit,
    onScanQr: () -> Unit,
    onTakeSelfie: () -> Unit,
    onFetchLocation: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Absensi saya", style = MaterialTheme.typography.headlineSmall)
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
                    DropdownMenuItem({ Text("${b.name} (${b.code})") }, { open = false; onAction(AttendanceAction.PickBranch(b)) })
                }
            }
        }
        if (state.branch != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kode QR: ${state.qr.ifEmpty { "—" }}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onScanQr) { Text("Pindai QR cabang") }
                        OutlinedButton(onClick = onFetchLocation) { Text("Ambil lokasi") }
                    }
                    Text(
                        if (state.lat != null) "Lokasi: ${state.lat}, ${state.lng}" else "Lokasi belum diambil.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTakeSelfie) { Text(if (state.selfie != null) "Swafoto OK ✓" else "Swafoto") }
                    }
                    Button(
                        onClick = { onAction(AttendanceAction.ClockIn) },
                        Modifier.fillMaxWidth(),
                        enabled = !state.busy && state.qr.isNotEmpty() && state.selfie != null,
                    ) { Text(if (state.busy) "Mengirim…" else "Clock-In") }
                    OutlinedButton(
                        onClick = { onAction(AttendanceAction.ClockOut) },
                        Modifier.fillMaxWidth(),
                        enabled = !state.busy && state.qr.isNotEmpty(),
                    ) { Text("Clock-Out") }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = { onAction(AttendanceAction.History) }, Modifier.fillMaxWidth()) { Text("Riwayat 30 hari") }
        if (state.busy && state.history.isEmpty() && state.branches.isNotEmpty()) LoadingBox()
        if (state.history.isEmpty() && !state.busy) EmptyBox("Belum ada riwayat pada rentang ini.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.history) { h ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "${h.status ?: "-"} · masuk ${h.clockInAt ?: "—"} · pulang ${h.clockOutAt ?: "—"}",
                        Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
