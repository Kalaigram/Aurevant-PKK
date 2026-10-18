package com.aurevant.pkk.ui.features.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox

@Composable
fun EmployeeCardScreen(state: CardState) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kartu karyawan", style = MaterialTheme.typography.headlineSmall)
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.card != null -> {
                val card = state.card
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(card.name, style = MaterialTheme.typography.headlineSmall)
                        Text("${card.role ?: ""} · ${card.branch ?: ""}")
                        Text(card.company ?: "", style = MaterialTheme.typography.bodySmall)
                        Text("Kode: ${card.qrContent ?: "—"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
