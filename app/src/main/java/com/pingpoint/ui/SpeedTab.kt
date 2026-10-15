package com.pingpoint.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pingpoint.model.PingUiState
import com.pingpoint.net.SpeedDial
import com.pingpoint.net.SpeedPhase
import com.pingpoint.ui.theme.Signal
import com.pingpoint.ui.theme.SignalDark
import com.pingpoint.ui.theme.Sky
import com.pingpoint.ui.theme.SkyDark
import com.pingpoint.ui.theme.isAppInDarkTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SpeedTab(
    state: PingUiState,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val hasResult = state.downloadMbps != null || state.uploadMbps != null
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isSpeedTesting) {
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
                Text("Start speed test")
            }
        }
        state.speedError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        if (!state.isSpeedTesting && !hasResult && state.speedError == null) {
            Text(
                "Measures how fast this connection can move data, in megabits per second. Ping time is a separate number. A test downloads up to 40 MB and uploads up to 20 MB.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SpeedReadout(state)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SpeedReadout(state: PingUiState) {
    val downloadColor = if (isAppInDarkTheme()) Signal else SignalDark
    val uploadColor = if (isAppInDarkTheme()) Sky else SkyDark
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.isSpeedTesting) {
                val upload = state.speedPhase == SpeedPhase.Upload
                Text(
                    if (upload) "Upload" else "Download",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpeedGauge(
                    mbps = state.speedLiveMbps,
                    caption = state.speedLiveMbps?.let { formatMbps(it) } ?: "Connecting",
                    color = if (upload) uploadColor else downloadColor,
                    compact = false,
                )
            }
            val download = state.downloadMbps
            val uploadMbps = state.uploadMbps
            if (!state.isSpeedTesting && (download != null || uploadMbps != null)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SpeedGauge(
                        mbps = download,
                        caption = download?.let { formatMbps(it) } ?: "—",
                        color = downloadColor,
                        compact = true,
                        label = "Download",
                        modifier = Modifier.weight(1f),
                    )
                    SpeedGauge(
                        mbps = uploadMbps,
                        caption = uploadMbps?.let { formatMbps(it) } ?: "—",
                        color = uploadColor,
                        compact = true,
                        label = "Upload",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!state.isSpeedTesting && download != null && uploadMbps != null) {
                UseRatings(rateConnection(download, uploadMbps))
            }
        }
    }
}

@Composable
private fun UseRatings(ratings: List<ActivityRating>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("What this speed can handle", style = MaterialTheme.typography.titleMedium)
        ratings.forEach { rating ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rating.activity, style = MaterialTheme.typography.bodyLarge)
                Text(
                    fitLabel(rating.fit),
                    color = fitColor(rating.fit),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Text(
            "Gaming also needs a low ping. The Monitor tab measures that.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun fitColor(fit: ConnectionFit): Color = when (fit) {
    ConnectionFit.Good -> latencyColor(10.0)
    ConnectionFit.Fair -> latencyColor(80.0)
    ConnectionFit.Poor -> latencyColor(200.0)
}

@Composable
private fun SpeedGauge(
    mbps: Double?,
    caption: String,
    color: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val fraction = SpeedDial.fraction(mbps ?: 0.0)
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        label?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(if (compact) 112.dp else 188.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeWidth = if (compact) 8.dp.toPx() else 14.dp.toPx()
                val radius = min(size.width / 2f, size.height) - strokeWidth
                val center = Offset(size.width / 2f, size.height - 2.dp.toPx())
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                drawArc(track, 180f, 180f, false, topLeft, arcSize, style = stroke)
                if (fraction > 0f) {
                    drawArc(color, 180f, 180f * fraction, false, topLeft, arcSize, style = stroke)
                }
                val radians = Math.toRadians(180.0 + 180.0 * fraction)
                val reach = radius * 0.72f
                val tip = Offset(
                    center.x + cos(radians).toFloat() * reach,
                    center.y + sin(radians).toFloat() * reach,
                )
                drawLine(color, center, tip, strokeWidth = if (compact) 2.dp.toPx() else 3.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(color, radius = if (compact) 4.dp.toPx() else 6.dp.toPx(), center = center)
            }
        }
        Text(
            caption,
            fontFamily = FontFamily.Monospace,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        if (!compact) {
            Text(
                "0 to 1,000 Mb/s",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
