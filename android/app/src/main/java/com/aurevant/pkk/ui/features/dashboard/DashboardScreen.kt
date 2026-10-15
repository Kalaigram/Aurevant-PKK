package com.aurevant.pkk.ui.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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

private val SECTION_LABEL = mapOf(
    "keuangan" to "Keuangan",
    "penjualan" to "Penjualan hari ini",
    "operasional" to "Operasional",
    "sdm" to "Kehadiran hari ini",
    "penggajian" to "Penggajian",
    "karyawan" to "Karyawan",
)

@Composable
fun DashboardScreen(
    state: DashboardState,
    onAction: (DashboardAction) -> Unit,
    onOpen: (String) -> Unit,
    onSwitchCompany: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(state.data?.companyName ?: "Dashboard", style = MaterialTheme.typography.headlineSmall)
                state.data?.subscriptionStatus?.let {
                    Text("Langganan: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = { onAction(DashboardAction.Logout) }) { Text("Keluar") }
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.data == null -> EmptyBox("Tidak ada data.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.data.sections.forEach { (key, values) ->
                    item(key = key) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(SECTION_LABEL[key] ?: key, style = MaterialTheme.typography.titleMedium)
                                values.forEach { (k, v) -> Text("$k: $v", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = onSwitchCompany) { Text("Ganti perusahaan") }
                    if ("transaksi.buat" in state.permissions) {
                        TextButton(onClick = { onOpen("pos") }) { Text("Buka kasir →") }
                    }
                    if ("stok.transfer" in state.permissions || "stok.opname" in state.permissions) {
                        TextButton(onClick = { onOpen("attendance") }) { Text("Absensi saya →") }
                    }
                }
            }
        }
    }
}
