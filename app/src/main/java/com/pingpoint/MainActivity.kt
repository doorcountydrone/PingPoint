package com.pingpoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pingpoint.ui.PingPointApp
import com.pingpoint.ui.theme.PingPointTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PingViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PingPointTheme {
                PingPointApp(
                    state = state,
                    onHostChange = viewModel::onHostChange,
                    onInternetChange = viewModel::onInternetChange,
                    onPacketCount = viewModel::onPacketCount,
                    onPing = viewModel::startPing,
                    onStopPing = viewModel::stopPing,
                    onScan = viewModel::startScan,
                    onStopScan = viewModel::stopScan,
                    onStartMonitor = viewModel::startMonitor,
                    onStopMonitor = viewModel::stopMonitor,
                    onNotificationPermissionResult = viewModel::onNotificationPermissionResult,
                    onSelectTab = viewModel::selectTab,
                    onDeviceClick = viewModel::pingDevice,
                    onRefreshNetwork = viewModel::refreshNetwork,
                    onSaveCurrent = viewModel::promptSaveCurrent,
                    onSaveDevice = viewModel::promptSaveDevice,
                    onSaveNameChange = viewModel::onSaveNameChange,
                    onConfirmSave = viewModel::confirmSave,
                    onDismissSave = viewModel::dismissSave,
                    onRemoveSaved = viewModel::removeSaved,
                    onMonitorSaved = viewModel::monitorSaved,
                    onCheckPorts = viewModel::checkSavedPorts,
                    onDismissPortCheck = viewModel::dismissPortCheck,
                    onStartSpeed = viewModel::startSpeedTest,
                    onStopSpeed = viewModel::stopSpeedTest,
                    onDnsChange = viewModel::onDnsChange,
                    onDnsLookup = viewModel::startDnsLookup,
                )
            }
        }
    }
}
