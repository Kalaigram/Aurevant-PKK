package com.aurevant.pkk.ui.features.pelanggan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aurevant.pkk.core.ui.EmptyBox
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox

@Composable
fun PelangganRoute(vm: PelangganViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val q = state.query.trim().lowercase()
    val tampil = if (q.isEmpty()) state.items else state.items.filter {
        it.name.lowercase().contains(q) || (it.phone ?: "").contains(q)
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pelanggan", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(state.query, vm::search, Modifier.fillMaxWidth(), label = { Text("Cari nama / HP") }, singleLine = true)
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            tampil.isEmpty() -> EmptyBox("Tidak ada pelanggan.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tampil) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                            Text(listOfNotNull(c.phone, c.address).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
