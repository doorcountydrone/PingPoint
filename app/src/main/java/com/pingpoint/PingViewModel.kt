package com.pingpoint

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pingpoint.model.MONITOR_CAPACITY
import com.pingpoint.model.MonitorPoint
import com.pingpoint.model.PingUiState
import com.pingpoint.model.PortCheck
import com.pingpoint.model.SavePrompt
import com.pingpoint.model.SavedDevice
import com.pingpoint.net.ArpTable
import com.pingpoint.net.DeviceNameResolver
import com.pingpoint.net.DnsLookup
import com.pingpoint.net.HostInput
import com.pingpoint.net.LocalNetwork
import com.pingpoint.net.MonitorRelay
import com.pingpoint.net.OuiTable
import com.pingpoint.net.PortProbe
import com.pingpoint.net.PingEngine
import com.pingpoint.net.PingEvent
import com.pingpoint.net.SavedDeviceCodec
import com.pingpoint.net.SpeedEvent
import com.pingpoint.net.SpeedTest
import com.pingpoint.net.SavedDeviceStore
import com.pingpoint.net.ScanEvent
import com.pingpoint.net.Subnet
import com.pingpoint.ui.seriesLabel
import com.pingpoint.net.WatchTick
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class PingViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext = app.applicationContext
    private val savedStore = SavedDeviceStore(appContext)
    private val _uiState = MutableStateFlow(PingUiState(savedDevices = savedStore.read()))
    val uiState: StateFlow<PingUiState> = _uiState.asStateFlow()

    private var pingJob: Job? = null
    private var scanJob: Job? = null
    private var monitorJob: Job? = null
    private var portJob: Job? = null
    private var speedJob: Job? = null
    private var dnsJob: Job? = null
    private var pingGeneration = 0
    private var scanGeneration = 0
    private var monitorGeneration = 0
    private var portGeneration = 0
    private var speedGeneration = 0
    private var dnsGeneration = 0
    private var notificationAsked = false

    init {
        refreshNetwork()
        viewModelScope.launch(Dispatchers.IO) {
            val vendors = try {
                OuiTable.load(appContext)
            } catch (_: Exception) {
                emptyMap()
            }
            _uiState.update { it.copy(vendors = vendors) }
        }
    }

    fun onHostChange(value: String) {
        _uiState.update { it.copy(hostInput = value, pingError = null, monitorError = null) }
    }

    fun onInternetChange(value: String) {
        _uiState.update { it.copy(internetInput = value, monitorError = null) }
    }

    fun onDnsChange(value: String) {
        _uiState.update { it.copy(dnsInput = value, dnsError = null) }
    }

    fun onPacketCount(count: Int) {
        if (count !in PACKET_CHOICES || _uiState.value.isPinging) return
        _uiState.update { it.copy(packetCount = count) }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun refreshNetwork() {
        val local = LocalNetwork.read(appContext)
        _uiState.update { state ->
            state.copy(
                local = local,
                hostInput = if (state.hostInput.isBlank()) local.gateway.orEmpty() else state.hostInput,
            )
        }
    }

    fun startPing() {
        val host = HostInput.normalize(_uiState.value.hostInput)
        if (host == null) {
            _uiState.update { it.copy(pingError = "Enter an IP address or hostname.") }
            return
        }
        val count = _uiState.value.packetCount
        refreshNetwork()
        val generation = ++pingGeneration
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPinging = true,
                    samples = emptyList(),
                    summary = null,
                    targetMac = null,
                    targetName = null,
                    pingError = null,
                )
            }
            try {
                PingEngine.ping(host, count).collect { event ->
                    if (generation != pingGeneration) return@collect
                    when (event) {
                        is PingEvent.Sample -> _uiState.update { it.copy(samples = it.samples + event.sample) }
                        is PingEvent.Mac -> _uiState.update { it.copy(targetMac = event.mac) }
                        is PingEvent.Summary -> {
                            _uiState.update { it.copy(summary = event.summary) }
                            val repliedFrom = _uiState.value.samples.lastOrNull()?.from
                            val address = listOfNotNull(repliedFrom, host).firstOrNull { Subnet.isPrivateLan(it) }
                            if (address != null) {
                                launch(Dispatchers.IO) {
                                    val name = resolveName(address)
                                    if (name != null && generation == pingGeneration) {
                                        _uiState.update { it.copy(targetName = name) }
                                    }
                                }
                            }
                        }
                        is PingEvent.Failure -> _uiState.update { it.copy(pingError = event.message) }
                        is PingEvent.Lost -> Unit
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == pingGeneration) {
                    _uiState.update { it.copy(pingError = error.message ?: "Ping failed.") }
                }
            } finally {
                if (generation == pingGeneration) {
                    _uiState.update { it.copy(isPinging = false) }
                }
            }
        }
    }

    fun stopPing() {
        pingJob?.cancel()
    }

    fun startScan() {
        val local = LocalNetwork.read(appContext)
        _uiState.update { it.copy(local = local) }
        if (!local.canScan || local.ipv4 == null || local.prefixLength == null) {
            _uiState.update {
                it.copy(scanError = local.statusNote ?: "Connect to Wi-Fi to scan this network.")
            }
            return
        }
        val hosts = Subnet.hostsForScan(local.ipv4, local.prefixLength)
        if (hosts.isEmpty()) {
            _uiState.update { it.copy(scanError = "Couldn't build a scan for this network.") }
            return
        }
        val generation = ++scanGeneration
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    devices = emptyList(),
                    scanCompleted = 0,
                    scanTotal = hosts.size,
                    scanError = null,
                )
            }
            val nameGate = Semaphore(6)
            try {
                PingEngine.scan(hosts).collect { event ->
                    if (generation != scanGeneration) return@collect
                    when (event) {
                        is ScanEvent.Found -> {
                            _uiState.update { state ->
                                val merged = (state.devices.filter { it.ip != event.device.ip } + event.device)
                                    .sortedBy { Subnet.parseIpv4(it.ip) ?: Long.MAX_VALUE }
                                state.copy(devices = merged)
                            }
                            val ip = event.device.ip
                            launch(Dispatchers.IO) {
                                nameGate.withPermit {
                                    val name = resolveName(ip)
                                    if (name != null && generation == scanGeneration) {
                                        _uiState.update { state ->
                                            state.copy(
                                                devices = state.devices.map { device ->
                                                    if (device.ip == ip) device.copy(name = name) else device
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is ScanEvent.Progress -> _uiState.update {
                            it.copy(scanCompleted = event.completed, scanTotal = event.total)
                        }
                    }
                }
                if (generation != scanGeneration) return@launch
                val arp = ArpTable.readAll()
                _uiState.update { state ->
                    state.copy(
                        devices = state.devices.map { device ->
                            if (device.mac != null) device else device.copy(mac = arp[device.ip])
                        }
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == scanGeneration) {
                    _uiState.update { it.copy(scanError = error.message ?: "Scan failed.") }
                }
            } finally {
                if (generation == scanGeneration) {
                    _uiState.update { it.copy(isScanning = false) }
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
    }

    private suspend fun resolveName(ip: String): String? {
        return try {
            DeviceNameResolver.lookup(appContext, ip)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    fun startMonitor() {
        val state = _uiState.value
        val host = HostInput.normalize(state.hostInput)
        if (host == null) {
            _uiState.update { it.copy(monitorError = "Enter an IP address or hostname.", tab = 1) }
            return
        }
        val internetText = state.internetInput.trim()
        val internet = if (internetText.isEmpty()) {
            null
        } else {
            HostInput.normalize(internetText)
        }
        if (internetText.isNotEmpty() && internet == null) {
            _uiState.update { it.copy(monitorError = "Enter an internet address such as 8.8.8.8.", tab = 1) }
            return
        }
        val compare = internet != null && !internet.equals(host, ignoreCase = true)
        if (needsNotificationPrompt()) {
            notificationAsked = true
            _uiState.update { it.copy(askNotificationPermission = true, tab = 1) }
            return
        }
        val watchedInternet = if (compare) internet else null
        val gateway = state.local?.gateway
        val generation = ++monitorGeneration
        monitorJob?.cancel()
        _uiState.update {
            it.copy(
                isMonitoring = true,
                monitorHost = host,
                monitorInternetHost = watchedInternet,
                monitorCompare = compare,
                monitorPoints = emptyList(),
                monitorSent = 0,
                monitorLost = 0,
                internetSent = 0,
                internetLost = 0,
                monitorError = null,
                monitorAlerts = notificationsAllowed(),
                askNotificationPermission = false,
                tab = 1,
            )
        }
        monitorJob = viewModelScope.launch {
            MonitorRelay.ticks.collect { tick ->
                if (generation != monitorGeneration) return@collect
                when (tick) {
                    is WatchTick.Round -> appendMonitorPoint(
                        MonitorPoint(tick.primaryRtt, if (tick.compare) tick.internetRtt else null),
                        lost = tick.primaryRtt == null,
                        internetLost = tick.compare && tick.internetRtt == null,
                    )
                    is WatchTick.Failed -> _uiState.update {
                        it.copy(monitorError = tick.message, isMonitoring = false)
                    }
                    WatchTick.Stopped -> _uiState.update { it.copy(isMonitoring = false) }
                }
            }
        }
        try {
            MonitorService.start(
                appContext,
                host,
                watchedInternet,
                seriesLabel(host, gateway, "Router"),
                watchedInternet?.let { seriesLabel(it, gateway, "Internet") },
            )
        } catch (error: Exception) {
            monitorGeneration++
            monitorJob?.cancel()
            _uiState.update {
                it.copy(
                    isMonitoring = false,
                    monitorError = error.message ?: "Could not keep watching in the background.",
                )
            }
        }
    }

    fun onNotificationPermissionResult() {
        _uiState.update { it.copy(askNotificationPermission = false) }
        startMonitor()
    }

    fun startSpeedTest() {
        val generation = ++speedGeneration
        speedJob?.cancel()
        _uiState.update {
            it.copy(
                isSpeedTesting = true,
                speedPhase = null,
                speedLiveMbps = null,
                downloadMbps = null,
                uploadMbps = null,
                speedError = null,
                tab = 3,
            )
        }
        speedJob = viewModelScope.launch {
            try {
                SpeedTest.run().collect { event ->
                    if (generation != speedGeneration) return@collect
                    when (event) {
                        is SpeedEvent.Live -> _uiState.update {
                            it.copy(speedPhase = event.phase, speedLiveMbps = event.megabitsPerSecond)
                        }
                        is SpeedEvent.DownloadDone -> _uiState.update {
                            it.copy(downloadMbps = event.megabitsPerSecond)
                        }
                        is SpeedEvent.Finished -> _uiState.update {
                            it.copy(
                                downloadMbps = event.downloadMbps,
                                uploadMbps = event.uploadMbps,
                                speedLiveMbps = null,
                                speedPhase = null,
                            )
                        }
                        is SpeedEvent.Failure -> _uiState.update { it.copy(speedError = event.message) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == speedGeneration) {
                    _uiState.update { it.copy(speedError = "Could not reach the test server.") }
                }
            } finally {
                if (generation == speedGeneration) {
                    _uiState.update { it.copy(isSpeedTesting = false, speedPhase = null) }
                }
            }
        }
    }

    fun stopSpeedTest() {
        speedGeneration++
        speedJob?.cancel()
        _uiState.update { it.copy(isSpeedTesting = false, speedPhase = null, speedLiveMbps = null) }
    }

    fun startDnsLookup() {
        val query = HostInput.normalize(_uiState.value.dnsInput)
        if (query == null) {
            _uiState.update { it.copy(dnsError = "Enter a name or IP address.", tab = 4) }
            return
        }
        val generation = ++dnsGeneration
        dnsJob?.cancel()
        _uiState.update {
            it.copy(
                isLookingUp = true,
                dnsRecords = emptyList(),
                dnsElapsedMs = null,
                dnsError = null,
                tab = 4,
            )
        }
        dnsJob = viewModelScope.launch {
            val started = System.nanoTime()
            try {
                val records = DnsLookup.lookup(query)
                if (generation != dnsGeneration) return@launch
                val elapsed = (System.nanoTime() - started) / 1_000_000
                if (records.isEmpty()) {
                    val message = if (DnsLookup.isIpAddress(query)) {
                        "No name is listed for that address."
                    } else {
                        "No DNS answer for that name."
                    }
                    _uiState.update { it.copy(dnsError = message, dnsElapsedMs = elapsed) }
                } else {
                    _uiState.update { it.copy(dnsRecords = records, dnsElapsedMs = elapsed) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == dnsGeneration) {
                    _uiState.update { it.copy(dnsError = "No DNS answer for that name.") }
                }
            } finally {
                if (generation == dnsGeneration) {
                    _uiState.update { it.copy(isLookingUp = false) }
                }
            }
        }
    }

    fun stopMonitor() {
        monitorGeneration++
        monitorJob?.cancel()
        MonitorService.stop(appContext)
        _uiState.update { it.copy(isMonitoring = false) }
    }

    override fun onCleared() {
        monitorGeneration++
        monitorJob?.cancel()
        pingJob?.cancel()
        scanJob?.cancel()
        portJob?.cancel()
        speedJob?.cancel()
        dnsJob?.cancel()
        MonitorService.stop(appContext)
    }

    private fun needsNotificationPrompt(): Boolean {
        if (notificationAsked || notificationsAllowed()) return false
        return true
    }

    private fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun appendMonitorPoint(point: MonitorPoint, lost: Boolean, internetLost: Boolean) {
        _uiState.update { state ->
            state.copy(
                monitorPoints = (state.monitorPoints + point).takeLast(MONITOR_CAPACITY),
                monitorSent = state.monitorSent + 1,
                monitorLost = state.monitorLost + if (lost) 1 else 0,
                internetSent = state.internetSent + if (state.monitorCompare) 1 else 0,
                internetLost = state.internetLost + if (internetLost) 1 else 0,
            )
        }
    }

    fun promptSaveCurrent() {
        val state = _uiState.value
        val typed = HostInput.normalize(state.hostInput)
        val replied = state.samples.lastOrNull()?.from?.let(HostInput::normalize)
        val host = replied?.takeIf { Subnet.parseIpv4(it) != null } ?: typed
        if (host == null) {
            _uiState.update { it.copy(pingError = "Enter an IP address or hostname.") }
            return
        }
        val existing = state.savedDevices.find { it.host.equals(host, ignoreCase = true) }
        openSavePrompt(
            host = host,
            mac = state.targetMac ?: existing?.mac,
            name = existing?.name ?: state.targetName ?: host,
        )
    }

    fun promptSaveDevice(ip: String, mac: String?, name: String?) {
        val host = HostInput.normalize(ip) ?: return
        val existing = _uiState.value.savedDevices.find { it.host.equals(host, ignoreCase = true) }
        openSavePrompt(
            host = host,
            mac = mac ?: existing?.mac,
            name = existing?.name ?: name ?: host,
        )
    }

    fun onSaveNameChange(value: String) {
        _uiState.update { state ->
            val prompt = state.savePrompt ?: return@update state
            state.copy(savePrompt = prompt.copy(name = value.take(40)))
        }
    }

    fun confirmSave() {
        val prompt = _uiState.value.savePrompt ?: return
        val host = HostInput.normalize(prompt.host) ?: return
        val device = SavedDevice(
            name = SavedDeviceCodec.normalizeName(prompt.name, host),
            host = host,
            mac = SavedDeviceCodec.normalizeMac(prompt.mac),
        )
        val updated = SavedDeviceCodec.upsert(_uiState.value.savedDevices, device)
        savedStore.write(updated)
        _uiState.update { it.copy(savedDevices = updated, savePrompt = null) }
    }

    fun dismissSave() {
        _uiState.update { it.copy(savePrompt = null) }
    }

    fun removeSaved(host: String) {
        val updated = _uiState.value.savedDevices.filterNot { it.host.equals(host, ignoreCase = true) }
        savedStore.write(updated)
        _uiState.update { it.copy(savedDevices = updated, savePrompt = null) }
    }

    fun checkSavedPorts(host: String) {
        val device = _uiState.value.savedDevices.find { it.host.equals(host, ignoreCase = true) } ?: return
        val normalized = HostInput.normalize(device.host) ?: return
        val generation = ++portGeneration
        portJob?.cancel()
        _uiState.update {
            it.copy(
                portCheck = PortCheck(
                    name = device.name,
                    host = normalized,
                    running = true,
                ),
            )
        }
        portJob = viewModelScope.launch {
            try {
                val results = PortProbe.check(normalized)
                if (generation == portGeneration) {
                    _uiState.update { state ->
                        val current = state.portCheck ?: return@update state
                        state.copy(portCheck = current.copy(running = false, results = results))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == portGeneration) {
                    _uiState.update { state ->
                        state.copy(portCheck = state.portCheck?.copy(running = false))
                    }
                }
            }
        }
    }

    fun dismissPortCheck() {
        portGeneration++
        portJob?.cancel()
        _uiState.update { it.copy(portCheck = null) }
    }

    fun monitorSaved(host: String) {
        _uiState.update { it.copy(hostInput = host, tab = 1, monitorError = null) }
        startMonitor()
    }

    private fun openSavePrompt(host: String, mac: String?, name: String) {
        _uiState.update {
            it.copy(
                savePrompt = SavePrompt(
                    host = host,
                    mac = SavedDeviceCodec.normalizeMac(mac),
                    name = name,
                ),
            )
        }
    }

    fun pingDevice(ip: String) {
        scanJob?.cancel()
        _uiState.update {
            it.copy(
                hostInput = ip,
                tab = 0,
                isScanning = false,
                pingError = null,
            )
        }
        startPing()
    }

    private companion object {
        val PACKET_CHOICES = setOf(1, 4, 8, 16)
    }
}
