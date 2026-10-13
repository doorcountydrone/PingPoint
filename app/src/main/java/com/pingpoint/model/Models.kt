package com.pingpoint.model

import com.pingpoint.net.DnsRecord
import com.pingpoint.net.PortResult
import com.pingpoint.net.SpeedPhase

data class PingSample(
    val sequence: Int,
    val bytes: Int,
    val ttl: Int?,
    val rttMs: Double,
    val rttIsUpperBound: Boolean,
    val from: String,
)

data class PingSummary(
    val host: String,
    val transmitted: Int,
    val received: Int,
    val lossPercent: Int,
    val minMs: Double?,
    val avgMs: Double?,
    val maxMs: Double?,
)

data class LanDevice(
    val ip: String,
    val mac: String?,
    val rttMs: Double?,
    val name: String? = null,
)

data class SavedDevice(
    val name: String,
    val host: String,
    val mac: String?,
)

data class SavePrompt(
    val host: String,
    val mac: String?,
    val name: String,
)

data class PortCheck(
    val name: String,
    val host: String,
    val running: Boolean,
    val results: List<PortResult> = emptyList(),
)

/** One monitor sample. A null time means that side got no reply. */
data class MonitorPoint(
    val rttMs: Double?,
    val internetRttMs: Double? = null,
)

const val MONITOR_CAPACITY = 40

data class LocalNetworkInfo(
    val ipv4: String?,
    val prefixLength: Int?,
    val scanCidr: String?,
    val gateway: String?,
    val gatewayMac: String?,
    val interfaceName: String?,
    val transport: String,
    val canScan: Boolean,
    val statusNote: String?,
)

data class PingUiState(
    val hostInput: String = "",
    val packetCount: Int = 4,
    val isPinging: Boolean = false,
    val samples: List<PingSample> = emptyList(),
    val summary: PingSummary? = null,
    val targetMac: String? = null,
    val targetName: String? = null,
    val pingError: String? = null,
    val local: LocalNetworkInfo? = null,
    val devices: List<LanDevice> = emptyList(),
    val isScanning: Boolean = false,
    val scanCompleted: Int = 0,
    val scanTotal: Int = 0,
    val scanError: String? = null,
    val internetInput: String = "8.8.8.8",
    val isMonitoring: Boolean = false,
    val monitorHost: String? = null,
    val monitorInternetHost: String? = null,
    val monitorCompare: Boolean = false,
    val monitorPoints: List<MonitorPoint> = emptyList(),
    val monitorSent: Int = 0,
    val monitorLost: Int = 0,
    val internetSent: Int = 0,
    val internetLost: Int = 0,
    val monitorError: String? = null,
    val monitorAlerts: Boolean = true,
    val askNotificationPermission: Boolean = false,
    val savedDevices: List<SavedDevice> = emptyList(),
    val savePrompt: SavePrompt? = null,
    val portCheck: PortCheck? = null,
    val isSpeedTesting: Boolean = false,
    val speedPhase: SpeedPhase? = null,
    val speedLiveMbps: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val speedError: String? = null,
    val dnsInput: String = "",
    val isLookingUp: Boolean = false,
    val dnsRecords: List<DnsRecord> = emptyList(),
    val dnsElapsedMs: Long? = null,
    val dnsError: String? = null,
    val vendors: Map<String, String> = emptyMap(),
    val tab: Int = 0,
)
