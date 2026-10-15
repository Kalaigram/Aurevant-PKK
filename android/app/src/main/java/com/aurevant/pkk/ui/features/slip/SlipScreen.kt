package com.aurevant.pkk.ui.features.slip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox
import com.aurevant.pkk.core.ui.rupiah

@Composable
fun SlipScreen(state: SlipState, onAction: (SlipAction) -> Unit) {
    var period by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Slip gaji saya", style = MaterialTheme.typography.headlineSmall)
        Text("ID periode bisa ditanyakan ke HR.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                period, { period = it }, Modifier.weight(1f), label = { Text("ID periode") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
            )
            Button(onClick = { period.toIntOrNull()?.let { onAction(SlipAction.Load(it)) } }, enabled = !state.busy) {
                Text("Lihat")
            }
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.slip != null -> {
                val slip = state.slip
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(slip.periodName, style = MaterialTheme.typography.titleMedium)
                        Text("${slip.from ?: ""} → ${slip.to ?: ""} · ${slip.status ?: ""}", style = MaterialTheme.typography.bodySmall)
                        slip.lines.forEach { line ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(line.label, Modifier.weight(1f))
                                Text(rupiah(line.amount))
                            }
                        }
                        Text("Bersih: ${rupiah(slip.net)}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
