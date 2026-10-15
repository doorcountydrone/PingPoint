package com.pingpoint.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pingpoint.model.HistoryEntry
import com.pingpoint.model.MonitorHistory
import com.pingpoint.model.PingUiState
import com.pingpoint.model.SpeedHistory
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryTab(
    state: PingUiState,
    modifier: Modifier = Modifier,
    onClear: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("History", style = MaterialTheme.typography.titleMedium)
            if (state.history.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
        if (state.history.isEmpty()) {
            Text(
                "Finished speed tests and monitor sessions show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.history.forEach { entry ->
                HistoryCard(entry)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                formatHistoryTime(entry.atMillis),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (entry) {
                is SpeedHistory -> {
                    Text("Speed test", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Down  ${formatMbps(entry.downloadMbps)}    Up  ${formatMbps(entry.uploadMbps)}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is MonitorHistory -> {
                    Text("Monitor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${entry.host}    ${entry.avgMs?.let { formatMs(it) } ?: "—"}    loss ${entry.lossPercent}%    ${entry.sent} pkts",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    entry.internetHost?.let { far ->
                        Text(
                            "$far    ${entry.internetAvgMs?.let { formatMs(it) } ?: "—"}    loss ${entry.internetLossPercent ?: 0}%",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun formatHistoryTime(atMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(atMillis))
}
