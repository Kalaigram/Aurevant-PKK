package com.aurevant.pkk.ui.features.timeoff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

private val TYPES = listOf("izin", "sakit", "cuti")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeoffScreen(state: TimeoffState, onAction: (TimeoffAction) -> Unit) {
    var type by remember { mutableStateOf("izin") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cuti saya", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { showForm = !showForm }, Modifier.fillMaxWidth()) {
            Text(if (showForm) "Tutup form" else "Ajukan izin / sakit / cuti")
        }
        if (showForm) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(open, { open = it }) {
                        OutlinedTextField(type, {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true, label = { Text("Jenis") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) })
                        ExposedDropdownMenu(open, { open = false }) {
                            TYPES.forEach { t ->
                                DropdownMenuItem({ Text(t) }, { open = false; type = t })
                            }
                        }
                    }
                    OutlinedTextField(from, { from = it }, Modifier.fillMaxWidth(), label = { Text("Dari (yyyy-mm-dd)") }, singleLine = true)
                    OutlinedTextField(to, { to = it }, Modifier.fillMaxWidth(), label = { Text("Sampai (yyyy-mm-dd)") }, singleLine = true)
                    OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("Alasan") })
                    Button(
                        onClick = { onAction(TimeoffAction.File(type, from, to, reason)); showForm = false },
                        enabled = !state.busy && from.isNotBlank() && to.isNotBlank() && reason.isNotBlank(),
                    ) { Text("Kirim pengajuan") }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.items.isEmpty() -> EmptyBox("Belum ada pengajuan.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${c.type} · ${c.status}", style = MaterialTheme.typography.titleSmall)
                            Text("${c.from ?: ""} → ${c.to ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
