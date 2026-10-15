package com.aurevant.pkk.ui.features.notifications

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurevant.pkk.core.ui.EmptyBox
import com.aurevant.pkk.core.ui.ErrorBox
import com.aurevant.pkk.core.ui.LoadingBox

@Composable
fun NotificationsScreen(
    state: NotificationsState,
    onAction: (NotificationsAction) -> Unit,
    onToggleUnread: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Notifikasi", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = { onAction(NotificationsAction.ReadAll) }) { Text("Tandai dibaca") }
        }
        OutlinedButton(onClick = onToggleUnread) {
            Text(if (state.showUnreadOnly) "Tampil: belum dibaca" else "Tampil: semua")
        }
        when {
            state.busy -> LoadingBox()
            state.error != null -> ErrorBox(state.error)
            state.items.isEmpty() -> EmptyBox("Tidak ada notifikasi.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items) { n ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall)
                            n.body?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(if (n.read) "Dibaca" else "Baru", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
