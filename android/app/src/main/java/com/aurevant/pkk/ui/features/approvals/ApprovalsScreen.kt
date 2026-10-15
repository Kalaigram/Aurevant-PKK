package com.aurevant.pkk.ui.features.approvals

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

@Composable
fun ApprovalsScreen(state: ApprovalsState, onAction: (ApprovalsAction) -> Unit) {
    var note by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Persetujuan", style = MaterialTheme.typography.headlineSmall)
        Text("Satu antrean untuk void, diskon, refund, PO, transfer, opname.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Catatan putusan (opsional)") }, singleLine = true)
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.items.isEmpty() -> EmptyBox("Tidak ada pengajuan menunggu.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items) { a ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(a.actionLabel ?: a.action, style = MaterialTheme.typography.titleMedium)
                            Text("Status: ${a.status}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onAction(ApprovalsAction.Decide(a.id, true, note)) }) { Text("Setujui") }
                                OutlinedButton(onClick = { onAction(ApprovalsAction.Decide(a.id, false, note)) }) { Text("Tolak") }
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
