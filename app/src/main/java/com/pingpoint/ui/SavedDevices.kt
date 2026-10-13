package com.pingpoint.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingpoint.model.PingUiState
import com.pingpoint.model.PortCheck
import com.pingpoint.model.SavePrompt
import com.pingpoint.model.SavedDevice
import com.pingpoint.net.MacVendor
import com.pingpoint.net.PortState
import com.pingpoint.net.portStateLabel

@Composable
fun SavedDeviceSection(
    devices: List<SavedDevice>,
    vendors: Map<String, String>,
    onPing: (String) -> Unit,
    onMonitor: (String) -> Unit,
    onCheckPorts: (String) -> Unit,
    onEdit: (SavedDevice) -> Unit,
) {
    if (devices.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Saved", style = MaterialTheme.typography.titleMedium)
        devices.forEach { device ->
            SavedDeviceRow(
                device = device,
                vendors = vendors,
                onPing = { onPing(device.host) },
                onMonitor = { onMonitor(device.host) },
                onCheckPorts = { onCheckPorts(device.host) },
                onEdit = { onEdit(device) },
            )
        }
    }
}

@Composable
fun SaveDeviceDialog(
    prompt: SavePrompt,
    alreadySaved: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (alreadySaved) "Edit device" else "Save device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = prompt.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name") },
                )
                Text(prompt.host, fontFamily = FontFamily.Monospace)
                prompt.mac?.let { mac ->
                    Text(mac, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (alreadySaved) {
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun SaveBookmarkButton(saved: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (saved) "Edit saved device" else "Save device",
        )
    }
}

@Composable
fun PortCheckDialog(check: PortCheck, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(check.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(check.host, fontFamily = FontFamily.Monospace)
                if (check.running && check.results.isEmpty()) {
                    Text("Checking web, secure web, and DNS…")
                }
                check.results.forEach { result ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${result.service.name}  ${result.service.number}")
                        Text(
                            portStateLabel(result.state),
                            color = portStateColor(result.state),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Text(
                    "Open means something is listening. Closed means the device refused that port. A ping cannot tell these apart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun portStateColor(state: PortState): Color = when (state) {
    PortState.Open -> latencyColor(10.0)
    PortState.Closed -> MaterialTheme.colorScheme.onSurfaceVariant
    PortState.NoAnswer -> latencyColor(80.0)
}

fun PingUiState.hostIsSaved(host: String): Boolean {
    return savedDevices.any { it.host.equals(host, ignoreCase = true) }
}

@Composable
private fun SavedDeviceRow(
    device: SavedDevice,
    vendors: Map<String, String>,
    onPing: () -> Unit,
    onMonitor: () -> Unit,
    onCheckPorts: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onPing).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    device.host,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                device.mac?.let { mac ->
                    Text(
                        MacVendor.macLine(mac, vendors),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onMonitor) {
                Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = "Monitor ${device.name}")
            }
            IconButton(onClick = onCheckPorts) {
                Icon(Icons.Outlined.SettingsEthernet, contentDescription = "Check ports for ${device.name}")
            }
            SaveBookmarkButton(saved = true, onClick = onEdit)
        }
    }
}
