package com.pingpoint.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingpoint.model.LanDevice
import com.pingpoint.model.LocalNetworkInfo
import com.pingpoint.model.PingSample
import com.pingpoint.model.PingSummary
import com.pingpoint.model.PingUiState
import com.pingpoint.net.AppUpdate
import com.pingpoint.net.MacVendor
import com.pingpoint.net.Subnet

private val packetChoices = listOf(1, 4, 8, 16)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingPointApp(
    state: PingUiState,
    onHostChange: (String) -> Unit,
    onInternetChange: (String) -> Unit,
    onPacketCount: (Int) -> Unit,
    onPing: () -> Unit,
    onStopPing: () -> Unit,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit,
    onNotificationPermissionResult: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onDeviceClick: (String) -> Unit,
    onRefreshNetwork: () -> Unit,
    onSaveCurrent: () -> Unit,
    onSaveDevice: (String, String?, String?) -> Unit,
    onSaveNameChange: (String) -> Unit,
    onConfirmSave: () -> Unit,
    onDismissSave: () -> Unit,
    onRemoveSaved: (String) -> Unit,
    onMonitorSaved: (String) -> Unit,
    onCheckPorts: (String) -> Unit,
    onDismissPortCheck: () -> Unit,
    onStartSpeed: () -> Unit,
    onStopSpeed: () -> Unit,
    onDnsChange: (String) -> Unit,
    onDnsLookup: () -> Unit,
    onToggleSound: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onNotificationPermissionResult() }
    LaunchedEffect(state.askNotificationPermission) {
        if (state.askNotificationPermission && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (helpOpen) "Help" else "PingPoint") },
                    navigationIcon = {
                        if (helpOpen) {
                            IconButton(onClick = { helpOpen = false }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (!helpOpen) {
                            IconButton(onClick = onToggleSound) {
                                Icon(
                                    imageVector = if (state.soundEnabled) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                                    contentDescription = if (state.soundEnabled) "Sound on" else "Sound off",
                                )
                            }
                            IconButton(onClick = { helpOpen = true }) {
                                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Help")
                            }
                            IconButton(onClick = onRefreshNetwork) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh network")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                if (!helpOpen) ScrollableTabRow(selectedTabIndex = state.tab, edgePadding = 8.dp) {
                    Tab(
                        selected = state.tab == 0,
                        onClick = { onSelectTab(0) },
                        text = { Text("Ping") },
                        icon = { Icon(Icons.Outlined.Speed, contentDescription = null) },
                    )
                    Tab(
                        selected = state.tab == 1,
                        onClick = { onSelectTab(1) },
                        text = { Text("Monitor") },
                        icon = { Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = null) },
                    )
                    Tab(
                        selected = state.tab == 2,
                        onClick = { onSelectTab(2) },
                        text = { Text("Devices") },
                        icon = { Icon(Icons.Outlined.Devices, contentDescription = null) },
                    )
                    Tab(
                        selected = state.tab == 3,
                        onClick = { onSelectTab(3) },
                        text = { Text("Speed") },
                        icon = { Icon(Icons.Outlined.SwapVert, contentDescription = null) },
                    )
                    Tab(
                        selected = state.tab == 4,
                        onClick = { onSelectTab(4) },
                        text = { Text("DNS") },
                        icon = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.availableUpdate?.let { update ->
                UpdateBanner(update = update, onDismiss = onDismissUpdate)
            }
            if (helpOpen) {
                HelpScreen(Modifier.fillMaxSize())
            } else when (state.tab) {
                0 -> PingTab(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onHostChange = onHostChange,
                    onPacketCount = onPacketCount,
                    onPing = onPing,
                    onStopPing = onStopPing,
                    onSaveCurrent = onSaveCurrent,
                    onPingSaved = onDeviceClick,
                    onMonitorSaved = onMonitorSaved,
                    onCheckPorts = onCheckPorts,
                    onEditSaved = { device -> onSaveDevice(device.host, device.mac, device.name) },
                )
                1 -> MonitorTab(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onHostChange = onHostChange,
                    onInternetChange = onInternetChange,
                    onStart = onStartMonitor,
                    onStop = onStopMonitor,
                    onSaveCurrent = onSaveCurrent,
                )
                2 -> DevicesTab(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onScan = onScan,
                    onStopScan = onStopScan,
                    onDeviceClick = onDeviceClick,
                    onSaveDevice = onSaveDevice,
                    savedHosts = state.savedDevices.map { it.host },
                )
                3 -> SpeedTab(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onStart = onStartSpeed,
                    onStop = onStopSpeed,
                )
                else -> DnsTab(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onQueryChange = onDnsChange,
                    onLookup = onDnsLookup,
                )
            }
        }
        val prompt = state.savePrompt
        state.portCheck?.let { check ->
            PortCheckDialog(check = check, onDismiss = onDismissPortCheck)
        }
        if (prompt != null) {
            SaveDeviceDialog(
                prompt = prompt,
                alreadySaved = state.hostIsSaved(prompt.host),
                onNameChange = onSaveNameChange,
                onConfirm = onConfirmSave,
                onRemove = { onRemoveSaved(prompt.host) },
                onDismiss = onDismissSave,
            )
        }
    }
}

@Composable
private fun UpdateBanner(update: AppUpdate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Version ${update.versionName} is available",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "A newer PingPoint is ready to download.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.pageUrl)))
                    },
                ) {
                    Text("Get update")
                }
                TextButton(onClick = onDismiss) {
                    Text("Not now")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PingTab(
    state: PingUiState,
    modifier: Modifier = Modifier,
    onHostChange: (String) -> Unit,
    onPacketCount: (Int) -> Unit,
    onPing: () -> Unit,
    onStopPing: () -> Unit,
    onSaveCurrent: () -> Unit,
    onPingSaved: (String) -> Unit,
    onMonitorSaved: (String) -> Unit,
    onCheckPorts: (String) -> Unit,
    onEditSaved: (com.pingpoint.model.SavedDevice) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NetworkCard(state.local)
        SavedDeviceSection(
            devices = state.savedDevices,
            vendors = state.vendors,
            onPing = onPingSaved,
            onMonitor = onMonitorSaved,
            onCheckPorts = onCheckPorts,
            onEdit = onEditSaved,
        )
        OutlinedTextField(
            value = state.hostInput,
            onValueChange = onHostChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isPinging,
            label = { Text("IP address or hostname") },
            placeholder = { Text(state.local?.gateway ?: "8.8.8.8") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { if (!state.isPinging) onPing() }),
            trailingIcon = {
                SaveBookmarkButton(
                    saved = state.hostIsSaved(state.hostInput.trim()),
                    onClick = onSaveCurrent,
                )
            },
        )
        Text("Packets", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            packetChoices.forEachIndexed { index, count ->
                SegmentedButton(
                    selected = state.packetCount == count,
                    onClick = { onPacketCount(count) },
                    enabled = !state.isPinging,
                    shape = SegmentedButtonDefaults.itemShape(index, packetChoices.size),
                ) {
                    Text(count.toString())
                }
            }
        }
        if (state.isPinging) {
            Button(
                onClick = onStopPing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stop")
            }
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            Button(
                onClick = onPing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ping")
            }
        }
        state.pingError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        state.summary?.let { summary ->
            SummaryCard(
                summary = summary,
                samples = state.samples,
                mac = state.targetMac,
                name = state.targetName,
                vendors = state.vendors,
                onSave = onSaveCurrent,
            )
        }
        if (state.samples.isNotEmpty()) {
            Text("Replies", style = MaterialTheme.typography.titleMedium)
            state.samples.forEach { sample ->
                SampleRow(sample)
            }
        } else if (!state.isPinging && state.summary == null && state.pingError == null) {
            Text(
                "Ping an address to see how long each packet takes to come back. On your own Wi-Fi, the MAC address shows up too.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DevicesTab(
    state: PingUiState,
    modifier: Modifier = Modifier,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onSaveDevice: (String, String?, String?) -> Unit,
    savedHosts: List<String>,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NetworkCard(state.local)
        if (state.isScanning) {
            Button(
                onClick = onStopScan,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stop scan")
            }
            val total = state.scanTotal.coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { state.scanCompleted / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Checking ${state.scanCompleted} of ${state.scanTotal}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val local = state.local
            val scanLabel = if (local != null && local.canScan && local.scanCidr != null) {
                "Scan ${local.scanCidr}"
            } else {
                "Scan network"
            }
            Button(
                onClick = onScan,
                enabled = local?.canScan == true,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(scanLabel)
            }
        }
        state.scanError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        if (state.devices.isEmpty() && !state.isScanning) {
            val finishedEmpty = state.scanError == null &&
                state.scanTotal > 0 &&
                state.scanCompleted == state.scanTotal
            Text(
                if (finishedEmpty) {
                    "No devices replied. Phones that ignore ping stay hidden."
                } else {
                    "Finds phones, computers, and routers on this Wi-Fi. A row shows the device name, or the maker from its MAC, plus the IP and packet time. Devices that ignore ping stay hidden."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                if (state.isScanning) {
                    "${state.devices.size} found so far"
                } else {
                    "${state.devices.size} found · tap one to ping it"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.devices, key = { it.ip }) { device ->
                    DeviceRow(
                        device = device,
                        vendors = state.vendors,
                        saved = savedHosts.any { it.equals(device.ip, ignoreCase = true) },
                        onClick = { onDeviceClick(device.ip) },
                        onSave = { onSaveDevice(device.ip, device.mac, device.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkCard(local: LocalNetworkInfo?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                buildString {
                    append(local?.transport ?: "Network")
                    local?.interfaceName?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                local?.ipv4 ?: "No IPv4 address",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
            )
            val details = buildList {
                val info = local
                if (info != null) {
                    if (info.canScan && info.scanCidr != null) add("Scan ${info.scanCidr}")
                    info.gateway?.let { add("Gateway $it") }
                }
            }
            if (details.isNotEmpty()) {
                Text(
                    details.joinToString("   "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            local?.gatewayMac?.let { mac ->
                Text(
                    "Gateway MAC  $mac",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            local?.statusNote?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: PingSummary,
    samples: List<PingSample>,
    mac: String?,
    name: String?,
    vendors: Map<String, String>,
    onSave: () -> Unit,
) {
    val repliedFrom = samples.lastOrNull()?.from
    val macTarget = repliedFrom ?: summary.host
    val macLine = when {
        mac != null -> mac
        Subnet.isPrivateLan(macTarget) -> "MAC not in the ARP table"
        else -> "MAC addresses only show for devices on your Wi-Fi"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val maker = if (name == null) MacVendor.maker(mac, vendors) else null
            if (name != null) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (maker != null) {
                Text(maker, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                summary.host,
                style = if (name == null && maker == null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontFamily = if (name == null && maker == null) null else FontFamily.Monospace,
                color = if (name == null && maker == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (repliedFrom != null && repliedFrom != summary.host) {
                Text(
                    repliedFrom,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (mac == null) "MAC  $macLine" else MacVendor.macLine(mac, vendors, makerAlreadyShown = maker != null),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onSave) { Text("Save device") }
            if (samples.isNotEmpty()) {
                RttBars(samples)
            }
            val avg = summary.avgMs
            if (avg != null) {
                Text(
                    formatMs(avg),
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    color = latencyColor(avg),
                )
                Text(
                    "Average round trip · ${latencyLabel(avg)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("No replies", style = MaterialTheme.typography.headlineSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Min", summary.minMs?.let { formatMs(it) } ?: "—")
                Stat("Max", summary.maxMs?.let { formatMs(it) } ?: "—")
                Stat("Loss", "${summary.lossPercent}%")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Sent", summary.transmitted.toString())
                Stat("Replied", summary.received.toString())
                Stat("Size", samples.firstOrNull()?.bytes?.let { "$it B" } ?: "—")
            }
            Text(
                "Round-trip time is how long a packet takes to reach the device and come back.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RttBars(samples: List<PingSample>) {
    val times = samples.map { it.rttMs }
    val max = times.maxOrNull()?.coerceAtLeast(1.0) ?: return
    val colors = samples.map { latencyColor(it.rttMs) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
    ) {
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (times.size - 1).coerceAtLeast(0)) / times.size).coerceAtLeast(2f)
        times.forEachIndexed { index, time ->
            val barHeight = (time / max * size.height).toFloat().coerceAtLeast(4f)
            drawRoundRect(
                color = colors[index],
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }
    }
}

@Composable
private fun SampleRow(sample: PingSample) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(latencyColor(sample.rttMs))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "seq ${sample.sequence}  ·  ${sample.bytes} bytes",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                sample.ttl?.let { "ttl $it" } ?: "ttl —",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatMs(sample.rttMs, sample.rttIsUpperBound),
                fontFamily = FontFamily.Monospace,
                color = latencyColor(sample.rttMs),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                latencyLabel(sample.rttMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeviceRow(
    device: LanDevice,
    vendors: Map<String, String>,
    saved: Boolean,
    onClick: () -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val maker = if (device.name == null) MacVendor.maker(device.mac, vendors) else null
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    device.name ?: maker ?: device.ip,
                    fontFamily = if (device.name == null && maker == null) FontFamily.Monospace else null,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (device.name != null || maker != null) {
                    Text(
                        device.ip,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    MacVendor.macLine(device.mac, vendors, makerAlreadyShown = maker != null),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SaveBookmarkButton(saved = saved, onClick = onSave)
            device.rttMs?.let { rtt ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatMs(rtt),
                        fontFamily = FontFamily.Monospace,
                        color = latencyColor(rtt),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        latencyLabel(rtt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
