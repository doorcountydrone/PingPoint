package com.pingpoint.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pingpoint.BuildConfig

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "PingPoint checks the devices on your network and the path out to the internet.",
            style = MaterialTheme.typography.bodyLarge,
        )
        HelpBlock(
            title = "Updates",
            body = "When a newer release is on GitHub, PingPoint shows a banner with Get update. That opens the download page. Not now hides that version for the next three times you open the app, then the banner comes back. A still newer release shows right away.",
        )
        HelpBlock(
            title = "Theme",
            body = "The sun, moon, or auto icon in the top bar cycles light, dark, and follow the phone. The choice is remembered.",
        )
        HelpBlock(
            title = "Sound",
            body = "The speaker icon loops the classic Pong hit while a ping, monitor, network scan, or speed test is running, and stops when that run finishes. Starting again plays it again. Tap the icon to turn the sound off or on. The choice is remembered.",
        )
        HelpBlock(
            title = "Ping",
            body = "Sends a few packets to an address and times each reply. Under 20 ms is Fast, under 60 ms is Good, under 150 ms is Fair, and anything slower is Slow. Choose 1, 4, 8, or 16 packets. The bookmark saves that address.",
        )
        HelpBlock(
            title = "Monitor",
            body = "Charts your router and an internet address, usually 8.8.8.8, about once a second. Green is good (under 60 ms), amber is fair (60–150 ms), and red is bad (150 ms or more, or no reply). If both go bad, the problem is on the path to the router. If only the internet goes bad, the router still answers and the trouble is past it. PingPoint keeps watching after you leave and notifies you when an address stops answering or stays slow. Allow notifications for that alert.",
        )
        HelpBlock(
            title = "Devices",
            body = "Looks for phones, computers, and routers on this Wi-Fi or Ethernet connection. A row shows a name when the device shares one. Tap a row to ping it. Devices that ignore ping stay hidden, and cellular networks are not scanned.",
        )
        HelpBlock(
            title = "Saved devices",
            body = "The bookmark keeps a device on the Ping screen. The chart icon monitors it. The ethernet icon checks three ports: web (80), secure web (443), and DNS (53). The power icon sends a Wake-on-LAN packet when a MAC address is saved on the device. Open means something is listening. Closed means the device refused that port. DNS is checked over TCP, so a closed DNS port can still answer ordinary name lookups.",
        )
        HelpBlock(
            title = "MAC addresses",
            body = "Android does not let apps read the MAC addresses of other devices, so a row often says MAC unavailable. When an address is available, the first half can name the maker, such as Apple or TP-Link. A randomized phone address is labeled Private MAC and is not matched to a brand.",
        )
        HelpBlock(
            title = "DNS",
            body = "Looks up a name with this phone's DNS and lists the IPv4 and IPv6 addresses. Enter an IP address instead and it looks for the name listed for that address.",
        )
        HelpBlock(
            title = "History",
            body = "Keeps recent finished speed tests and monitor sessions, with averages and loss. Clear removes the list from this phone.",
        )
        HelpBlock(
            title = "Speed",
            body = "Measures download and upload in megabits per second, on a dial and as a number. The dial runs from 0 to 1,000 Mb/s. After the test, PingPoint rates browsing, HD and 4K streaming, video calls, and gaming. Gaming smoothness still depends on ping time. A test downloads up to 40 MB and uploads up to 20 MB.",
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Version ${BuildConfig.VERSION_NAME}",
            modifier = Modifier.fillMaxWidth(),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HelpBlock(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
