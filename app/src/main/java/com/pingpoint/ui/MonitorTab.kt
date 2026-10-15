package com.pingpoint.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingpoint.model.MONITOR_CAPACITY
import com.pingpoint.model.MonitorPoint
import com.pingpoint.model.PingUiState
import com.pingpoint.ui.theme.Sky
import com.pingpoint.ui.theme.SkyDark
import com.pingpoint.ui.theme.isAppInDarkTheme

private const val CHART_MAX_MS = 240.0

@Composable
fun MonitorTab(
    state: PingUiState,
    modifier: Modifier = Modifier,
    onHostChange: (String) -> Unit,
    onInternetChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSaveCurrent: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.hostInput,
            onValueChange = onHostChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isMonitoring,
            label = { Text("Router or device") },
            placeholder = { Text(state.local?.gateway ?: "192.168.1.1") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { if (!state.isMonitoring) onStart() }),
            trailingIcon = {
                SaveBookmarkButton(
                    saved = state.hostIsSaved(state.hostInput.trim()),
                    onClick = onSaveCurrent,
                )
            },
        )
        OutlinedTextField(
            value = state.internetInput,
            onValueChange = onInternetChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isMonitoring,
            label = { Text("Internet") },
            placeholder = { Text("8.8.8.8") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { if (!state.isMonitoring) onStart() }),
        )
        if (state.isMonitoring) {
            Button(
                onClick = onStop,
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
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start monitor")
            }
        }
        state.monitorError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        if (state.isMonitoring && !state.monitorAlerts) {
            Text(
                "Notifications are off, so a bad address will not sound an alert after you leave.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.monitorPoints.isEmpty() && !state.isMonitoring && state.monitorError == null) {
            Text(
                "Charts your router and an internet address together. PingPoint keeps watching after you leave, and notifies you if an address stops answering or stays slow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            MonitorStatus(state)
            if (state.monitorCompare) {
                SeriesLegend(state)
            }
            LatencyChart(points = state.monitorPoints, compare = state.monitorCompare)
            if (state.monitorCompare) {
                val near = seriesLabel(state.monitorHost, state.local?.gateway, "Router")
                val far = seriesLabel(state.monitorInternetHost, state.local?.gateway, "Internet")
                Text(
                    verdictText(pathVerdict(state.monitorPoints), near, far),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "Good under 60 ms    Fair 60–150    Bad 150+ or no reply",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.monitorCompare) {
                CompareStats(state)
            } else {
                BandCounts(state.monitorPoints)
                WindowStats(state)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MonitorStatus(state: PingUiState) {
    val latest = state.monitorPoints.lastOrNull()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (state.isMonitoring) "Live" else "Last reading",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.monitorCompare) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Reading(
                        seriesLabel(state.monitorHost, state.local?.gateway, "Router"),
                        latest?.rttMs,
                        waiting = latest == null,
                        modifier = Modifier.weight(1f),
                    )
                    Reading(
                        seriesLabel(state.monitorInternetHost, state.local?.gateway, "Internet"),
                        latest?.internetRttMs,
                        waiting = latest == null,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Reading(
                    seriesLabel(state.monitorHost, state.local?.gateway, "Device"),
                    latest?.rttMs,
                    waiting = latest == null,
                )
            }
        }
    }
}

@Composable
private fun Reading(label: String, rttMs: Double?, waiting: Boolean, modifier: Modifier = Modifier) {
    val band = if (waiting) null else latencyBand(rttMs)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            band?.let { bandLabel(it) } ?: "Waiting",
            style = MaterialTheme.typography.headlineMedium,
            color = if (band == null) MaterialTheme.colorScheme.onSurface else bandColor(band),
        )
        Text(
            when {
                waiting -> "Sending"
                rttMs != null -> formatMs(rttMs)
                else -> "No reply"
            },
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SeriesLegend(state: PingUiState) {
    val far = if (isAppInDarkTheme()) Sky else SkyDark
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(MaterialTheme.colorScheme.onSurface, seriesLabel(state.monitorHost, state.local?.gateway, "Router"))
        LegendItem(far, seriesLabel(state.monitorInternetHost, state.local?.gateway, "Internet"))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LatencyChart(points: List<MonitorPoint>, compare: Boolean) {
    val good = bandColor(LatencyBand.Good)
    val fair = bandColor(LatencyBand.Fair)
    val bad = bandColor(LatencyBand.Bad)
    val axis = MaterialTheme.colorScheme.onSurfaceVariant
    val line = MaterialTheme.colorScheme.onSurface
    val far = if (isAppInDarkTheme()) Sky else SkyDark

    Row(Modifier.fillMaxWidth().height(260.dp)) {
        Column(Modifier.fillMaxHeight().width(48.dp)) {
            BandLabel("bad", Modifier.weight(90f), bad)
            BandLabel("fair", Modifier.weight(90f), fair)
            BandLabel("good", Modifier.weight(60f), good)
        }
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            fun yOf(ms: Double): Float {
                val clamped = ms.coerceIn(0.0, CHART_MAX_MS)
                return size.height - (clamped / CHART_MAX_MS * size.height).toFloat()
            }

            drawRect(bad.copy(alpha = 0.22f), Offset.Zero, Size(size.width, yOf(150.0)))
            drawRect(fair.copy(alpha = 0.22f), Offset(0f, yOf(150.0)), Size(size.width, yOf(60.0) - yOf(150.0)))
            drawRect(good.copy(alpha = 0.22f), Offset(0f, yOf(60.0)), Size(size.width, size.height - yOf(60.0)))
            drawLine(fair, Offset(0f, yOf(60.0)), Offset(size.width, yOf(60.0)), strokeWidth = 1.5f)
            drawLine(bad, Offset(0f, yOf(150.0)), Offset(size.width, yOf(150.0)), strokeWidth = 1.5f)

            if (points.isEmpty()) return@Canvas
            val step = size.width / (MONITOR_CAPACITY - 1).coerceAtLeast(1)
            fun xOf(index: Int) = step * index
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            fun drawSeries(color: Color, lostColor: Color, lostDx: Float, value: (MonitorPoint) -> Double?) {
                val path = Path()
                var drawing = false
                points.forEachIndexed { index, point ->
                    val ms = value(point) ?: return@forEachIndexed
                    val x = xOf(index)
                    val y = yOf(ms)
                    if (!drawing) {
                        path.moveTo(x, y)
                        drawing = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(path, color.copy(alpha = 0.9f), style = stroke)
                points.forEachIndexed { index, point ->
                    val x = xOf(index) + lostDx
                    val ms = value(point)
                    if (ms == null) {
                        val mark = 4.dp.toPx()
                        val y = mark + 2.dp.toPx()
                        drawLine(lostColor, Offset(x - mark, y - mark), Offset(x + mark, y + mark), strokeWidth = 2.dp.toPx())
                        drawLine(lostColor, Offset(x - mark, y + mark), Offset(x + mark, y - mark), strokeWidth = 2.dp.toPx())
                    } else {
                        val radius = if (index == points.lastIndex) 6.dp.toPx() else 3.5.dp.toPx()
                        drawCircle(color, radius, Offset(xOf(index), yOf(ms)))
                    }
                }
            }

            if (compare) drawSeries(far, far, 5.dp.toPx()) { it.internetRttMs }
            drawSeries(line, if (compare) line else bad, if (compare) (-5).dp.toPx() else 0f) { it.rttMs }
        }
        Column(Modifier.fillMaxHeight().width(36.dp).padding(start = 4.dp)) {
            Box(Modifier.weight(90f).fillMaxWidth()) {
                Text("240", Modifier.align(Alignment.TopStart), color = axis, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                Text("150", Modifier.align(Alignment.BottomStart), color = bad, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            }
            Box(Modifier.weight(90f).fillMaxWidth()) {
                Text("60", Modifier.align(Alignment.BottomStart), color = fair, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            }
            Box(Modifier.weight(60f).fillMaxWidth()) {
                Text("0", Modifier.align(Alignment.BottomStart), color = axis, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun BandLabel(text: String, modifier: Modifier, color: Color) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BandCounts(points: List<MonitorPoint>) {
    val good = points.count { latencyBand(it.rttMs) == LatencyBand.Good }
    val fair = points.count { latencyBand(it.rttMs) == LatencyBand.Fair }
    val bad = points.count { latencyBand(it.rttMs) == LatencyBand.Bad }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        BandCount("Good", good, bandColor(LatencyBand.Good))
        BandCount("Fair", fair, bandColor(LatencyBand.Fair))
        BandCount("Bad", bad, bandColor(LatencyBand.Bad))
    }
}

@Composable
private fun BandCount(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, style = MaterialTheme.typography.labelLarge)
        Text(
            count.toString(),
            color = color,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun CompareStats(state: PingUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SideStats(
            seriesLabel(state.monitorHost, state.local?.gateway, "Router"),
            state.monitorPoints.map { it.rttMs },
            state.monitorSent,
            state.monitorLost,
            Modifier.weight(1f),
        )
        SideStats(
            seriesLabel(state.monitorInternetHost, state.local?.gateway, "Internet"),
            state.monitorPoints.map { it.internetRttMs },
            state.internetSent,
            state.internetLost,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun SideStats(
    label: String,
    samples: List<Double?>,
    sent: Int,
    lost: Int,
    modifier: Modifier = Modifier,
) {
    val replies = samples.mapNotNull { it }
    val average = replies.takeIf { it.isNotEmpty() }?.average()
    val loss = if (sent == 0) 0 else lost * 100 / sent
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
        Text(
            "Avg  ${average?.let { formatMs(it) } ?: "—"}",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Loss  $loss%   $sent pkts",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WindowStats(state: PingUiState) {
    val replies = state.monitorPoints.mapNotNull { it.rttMs }
    val average = replies.takeIf { it.isNotEmpty() }?.average()
    val loss = if (state.monitorSent == 0) 0 else state.monitorLost * 100 / state.monitorSent
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat("Average", average?.let { formatMs(it) } ?: "—")
        Stat("Loss", "$loss%")
        Stat("Packets", state.monitorSent.toString())
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
private fun bandColor(band: LatencyBand): Color = when (band) {
    LatencyBand.Good -> latencyColor(10.0)
    LatencyBand.Fair -> latencyColor(80.0)
    LatencyBand.Bad -> latencyColor(200.0)
}
