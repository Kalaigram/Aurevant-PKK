package com.aurevant.pkk.ui.features.companies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurevant.pkk.core.ui.EmptyBox
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox

@Composable
fun CompaniesScreen(state: CompaniesState, onAction: (CompaniesAction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pilih perusahaan", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { onAction(CompaniesAction.Logout) }) { Text("Keluar") }
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.items.isEmpty() -> EmptyBox("Belum ada perusahaan yang bisa diakses.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items) { c ->
                    Card(Modifier.fillMaxWidth().clickable { onAction(CompaniesAction.Pick(c)) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (c.status == "aktif") "Aktif" else "Ditangguhkan", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
