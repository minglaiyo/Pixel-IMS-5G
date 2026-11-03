package dev.bluehouse.enablevolte.pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.CarrierConfigManager
import android.telephony.CellInfo
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import dev.bluehouse.enablevolte.ui.theme.NumericSmallTextStyle
import dev.bluehouse.enablevolte.components.statusToneColor
import dev.bluehouse.enablevolte.MobileRadioIsolation
import dev.bluehouse.enablevolte.MobileRadioIsolationSession
import dev.bluehouse.enablevolte.PrivilegeManager
import dev.bluehouse.enablevolte.PrivilegeMode
import dev.bluehouse.enablevolte.R
import dev.bluehouse.enablevolte.SubscriptionModer
import dev.bluehouse.enablevolte.components.Panel
import dev.bluehouse.enablevolte.components.HeaderText
import dev.bluehouse.enablevolte.components.OnLifecycleEvent
import dev.bluehouse.enablevolte.components.PremiumPageIntro
import dev.bluehouse.enablevolte.components.PremiumStatusChip
import dev.bluehouse.enablevolte.components.StatusTone
import dev.bluehouse.enablevolte.uniqueName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MonitorSection(@StringRes val labelRes: Int) {
    LIVE(R.string.monitor_section_live),
    ATTACH(R.string.monitor_section_attach),
    RADIO(R.string.monitor_section_radio),
    CONFIG(R.string.monitor_section_config),
    CAPABILITIES(R.string.monitor_section_capabilities),
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MonitoringHub(subscriptions: List<SubscriptionInfo>) {
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf(MonitorSection.LIVE) }
    var isolationSession by remember { mutableStateOf<MobileRadioIsolationSession?>(null) }
    var isolationMessage by remember { mutableStateOf<String?>(null) }
    var isolationError by remember { mutableStateOf<String?>(null) }
    var isolationGeneration by remember { mutableStateOf(0) }

    fun restoreWifi() {
        val restored = isolationSession?.restore() ?: true
        isolationSession = null
        if (!restored) {
            isolationError = context.getString(R.string.mobile_only_wifi_restore_failed)
        }
    }

    LaunchedEffect(isolationGeneration) {
        if (isolationSession == null) {
            isolationError = null
            isolationMessage = context.getString(R.string.mobile_only_disabling_wifi)
            val result = withContext(Dispatchers.IO) { MobileRadioIsolation.begin() }
            isolationSession = result.session
            isolationMessage = result.messageRes?.let { context.getString(it) } ?: result.message
            if (!result.active) isolationError = result.messageRes?.let { context.getString(it) } ?: result.message
        }
    }
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                if (isolationSession == null) isolationGeneration += 1
            }
            Lifecycle.Event.ON_STOP -> restoreWifi()
            else -> Unit
        }
    }
    DisposableEffect(Unit) {
        onDispose { isolationSession?.restore() }
    }

    Column {
        PremiumPageIntro(
            eyebrow = context.getString(R.string.premium_monitor_eyebrow),
            title = context.getString(R.string.premium_monitor_title),
            description = context.getString(R.string.premium_monitor_description),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        Panel(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            context.getString(R.string.premium_mobile_path),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (isolationError != null) {
                                context.getString(R.string.premium_monitor_paused)
                            } else {
                                isolationMessage ?: context.getString(R.string.mobile_only_disabling_wifi)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PremiumStatusChip(
                        label = when {
                            isolationSession != null -> context.getString(R.string.premium_active)
                            isolationError != null -> context.getString(R.string.premium_attention)
                            else -> context.getString(R.string.premium_isolating)
                        },
                        tone = when {
                            isolationSession != null -> StatusTone.SUCCESS
                            isolationError != null -> StatusTone.DANGER
                            else -> StatusTone.ACCENT
                        },
                    )
                }
                isolationError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { isolationGeneration += 1 }) {
                        Text(context.getString(R.string.retry))
                    }
                }
            }
        }
        if (isolationSession != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MonitorSection.entries.forEach {
                    FilterChip(
                        selected = section == it,
                        onClick = { section = it },
                        label = { Text(stringResource(it.labelRes)) },
                    )
                }
            }
            when (section) {
                MonitorSection.LIVE -> Monitor(subscriptions)
                MonitorSection.ATTACH -> AttachTracePage(subscriptions)
                MonitorSection.RADIO -> PhysicalChannelsPage()
                MonitorSection.CONFIG -> CarrierConfigDiffPage(subscriptions)
                MonitorSection.CAPABILITIES -> NrCapabilitiesPage(subscriptions)
            }
        }
    }
}

@Composable
private fun PhysicalChannelsPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var output by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun capture() {
        if (PrivilegeManager.activeMode != PrivilegeMode.ROOT || !PrivilegeManager.isRootReady()) return
        scope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) {
                PrivilegeManager.getRootTelephonyDiagnostic("physical")
            }
            output = result ?: context.getString(R.string.monitor_physical_service_unavailable)
            loading = false
        }
    }

    LaunchedEffect(Unit) { capture() }
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderText(stringResource(R.string.monitor_physical_title))
        Text(
            stringResource(R.string.monitor_physical_intro),
            style = MaterialTheme.typography.bodySmall,
        )
        if (PrivilegeManager.activeMode == PrivilegeMode.ROOT && PrivilegeManager.isRootReady()) {
            Button(enabled = !loading, onClick = { capture() }) {
                Text(stringResource(if (loading) R.string.monitor_physical_reading else R.string.monitor_physical_refresh))
            }
            output?.lineSequence()?.filter { it.isNotBlank() }?.forEachIndexed { index, line ->
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val role = when {
                            line.contains("PrimaryServing") -> "PCell"
                            line.contains("SecondaryServing") -> "SCell"
                            else -> stringResource(R.string.monitor_physical_role_channel)
                        }
                        Text("$role ${index + 1}", style = MaterialTheme.typography.titleMedium)
                        Text(line, style = NumericSmallTextStyle)
                    }
                }
            }
        } else {
            Panel(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.monitor_physical_root_required),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Text(
            stringResource(R.string.monitor_physical_ca_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubscriptionPicker(
    subscriptions: List<SubscriptionInfo>,
    selectedSubId: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        subscriptions.forEach {
            FilterChip(
                selected = it.subscriptionId == selectedSubId,
                onClick = { onSelected(it.subscriptionId) },
                label = { Text(it.uniqueName) },
            )
        }
    }
}

private data class RegistryEvent(val time: String, val text: String)

private class RegistryCallback(private val event: (String) -> Unit) :
    TelephonyCallback(),
    TelephonyCallback.ServiceStateListener,
    TelephonyCallback.DisplayInfoListener,
    TelephonyCallback.CellInfoListener,
    TelephonyCallback.SignalStrengthsListener {
    override fun onServiceStateChanged(serviceState: ServiceState) {
        val nrState = runCatching {
            serviceState.javaClass.getMethod("getNrState").invoke(serviceState) as Int
        }.getOrDefault(0)
        event("ServiceState changed: state=${serviceState.state}, NR-state=$nrState")
    }

    override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
        event(
            "DisplayInfo changed: network=${telephonyDisplayInfo.networkType}, " +
                "override=${telephonyDisplayInfo.overrideNetworkType}",
        )
    }

    override fun onCellInfoChanged(cellInfo: List<CellInfo>) {
        event("CellInfo changed: ${cellInfo.size} framework-visible cells")
    }

    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
        event("Signal changed: ${signalStrength.level}/4, ${signalStrength.dbm} dBm")
    }
}

@Composable
private fun AttachTracePage(subscriptions: List<SubscriptionInfo>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSubId by rememberSaveable { mutableStateOf(subscriptions.firstOrNull()?.subscriptionId ?: -1) }
    // Subscriptions can arrive (or change) after this page opens; never stay pinned to a missing SIM.
    LaunchedEffect(subscriptions) {
        if (subscriptions.none { it.subscriptionId == selectedSubId }) {
            selectedSubId = subscriptions.firstOrNull()?.subscriptionId ?: -1
        }
    }
    var radio by remember { mutableStateOf<SubscriptionModer.RadioDiagnostics?>(null) }
    var report by remember { mutableStateOf<SubscriptionModer.RootForceReport?>(null) }
    var registryState by remember { mutableStateOf(context.getString(R.string.monitortrace_waiting_permission)) }
    var registryActive by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf(emptyList<RegistryEvent>()) }
    var rootEvidence by remember { mutableStateOf<String?>(null) }
    var rootCaptureRunning by remember { mutableStateOf(false) }
    var hasPhonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasPhonePermission = result[Manifest.permission.READ_PHONE_STATE] == true
    }

    DisposableEffect(selectedSubId, hasPhonePermission) {
        if (selectedSubId < 0 || !hasPhonePermission) {
            onDispose { }
        } else {
            val manager = context.getSystemService(TelephonyManager::class.java)
                .createForSubscriptionId(selectedSubId)
            val callback = RegistryCallback { message ->
                val now = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
                events = (listOf(RegistryEvent(now, message)) + events).take(80)
            }
            runCatching {
                manager.registerTelephonyCallback(context.mainExecutor, callback)
                registryState = context.getString(R.string.monitortrace_active)
                registryActive = true
            }.onFailure {
                registryState = context.getString(R.string.monitortrace_unavailable, it.message ?: it.javaClass.simpleName)
                registryActive = false
            }
            onDispose { runCatching { manager.unregisterTelephonyCallback(callback) } }
        }
    }

    LaunchedEffect(selectedSubId) {
        while (selectedSubId >= 0) {
            runCatching {
                withContext(Dispatchers.IO) {
                    val moder = SubscriptionModer(context, selectedSubId)
                    val current = moder.getRadioDiagnostics()
                    current to moder.getRootForceReport(current)
                }
            }.onSuccess {
                radio = it.first
                report = it.second
            }
            delay(2_500)
        }
    }

    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderText(stringResource(R.string.monitortrace_title))
        SubscriptionPicker(subscriptions, selectedSubId) {
            selectedSubId = it
            radio = null
            report = null
            events = emptyList()
        }

        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.monitortrace_callback_sources), style = MaterialTheme.typography.titleMedium)
                DiagnosticRow("TelephonyRegistry", registryState, registryActive)
                DiagnosticRow(
                    "ServiceStateTracker",
                    if (PrivilegeManager.activeMode == PrivilegeMode.ROOT) {
                        stringResource(R.string.monitortrace_root_snapshot)
                    } else {
                        stringResource(R.string.monitortrace_root_required_phone)
                    },
                    PrivilegeManager.activeMode == PrivilegeMode.ROOT,
                )
                DiagnosticRow(
                    "NetworkRegistrationManager",
                    if (PrivilegeManager.activeMode == PrivilegeMode.ROOT) {
                        stringResource(R.string.monitortrace_root_dumpsys)
                    } else {
                        stringResource(R.string.monitortrace_shizuku_only)
                    },
                    PrivilegeManager.activeMode == PrivilegeMode.ROOT,
                )
                if (!hasPhonePermission) {
                    Button(onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION),
                        )
                    }) { Text(stringResource(R.string.monitortrace_allow_callbacks)) }
                }
            }
        }

        radio?.let { current ->
            val gates = report
            val carrierNr = gates?.carrierNsa == true || gates?.carrierSa == true
            val localNrOpen = gates?.gates?.filter { it.mask != null }?.all { it.nrAllowed } == true
            val nsaConnected = current.displayTechnology == "5G NSA" || current.nrState == 3
            val reason = attachReason(context, current, carrierNr, localNrOpen)
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(stringResource(R.string.monitortrace_sequence_title), style = MaterialTheme.typography.titleMedium)
                    TraceStep(stringResource(R.string.monitortrace_step_lte), current.serviceState == "In service" && current.dataRat in setOf("LTE", "NR"))
                    TraceStep(stringResource(R.string.monitortrace_step_ims), current.imsRegistered)
                    TraceStep(stringResource(R.string.monitortrace_step_carrier_nr), carrierNr)
                    TraceStep(stringResource(R.string.monitortrace_step_gates), localNrOpen)
                    TraceStep(stringResource(R.string.monitortrace_step_nr_available), current.nrAvailable == true)
                    TraceStep(stringResource(R.string.monitortrace_step_dcnr), current.dcNrRestricted == false)
                    TraceStep(stringResource(R.string.monitortrace_step_endc), current.endcAvailable == true)
                    TraceStep(stringResource(R.string.monitortrace_step_nr_neighbor), current.nrBands.isNotEmpty())
                    TraceStep(stringResource(R.string.monitortrace_step_nsa_connected), nsaConnected)
                    Text(stringResource(R.string.monitortrace_diagnosis_title), style = MaterialTheme.typography.titleSmall)
                    Text(reason, color = if (nsaConnected) statusToneColor(StatusTone.SUCCESS) else MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(R.string.monitortrace_rrc_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.monitortrace_root_evidence_title), style = MaterialTheme.typography.titleMedium)
                if (PrivilegeManager.activeMode == PrivilegeMode.ROOT && PrivilegeManager.isRootReady()) {
                    Button(
                        enabled = !rootCaptureRunning,
                        onClick = {
                            scope.launch {
                                rootCaptureRunning = true
                                rootEvidence = withContext(Dispatchers.IO) {
                                    listOf("registry", "phone", "radio").joinToString("\n\n") {
                                        "[$it]\n" +
                                            PrivilegeManager.getRootTelephonyDiagnostic(it)
                                                .orEmpty()
                                                .ifBlank { context.getString(R.string.monitortrace_no_events) }
                                    }
                                }
                                rootCaptureRunning = false
                            }
                        }
                    ) {
                        Text(stringResource(if (rootCaptureRunning) R.string.monitortrace_capturing else R.string.monitortrace_capture))
                    }
                    rootEvidence?.let { Text(it.take(12_000), style = NumericSmallTextStyle) }
                } else {
                    Text(
                        stringResource(R.string.monitortrace_root_not_available),
                    )
                }
            }
        }

        if (events.isNotEmpty()) {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.monitortrace_event_log_title), style = MaterialTheme.typography.titleMedium)
                    events.forEach { Text("${it.time}  ${it.text}", style = NumericSmallTextStyle) }
                }
            }
        }
    }
}

private fun attachReason(
    context: Context,
    radio: SubscriptionModer.RadioDiagnostics,
    carrierNr: Boolean,
    localNrOpen: Boolean,
): String = when {
    radio.dataRat == "NR" -> context.getString(R.string.monitortrace_reason_sa)
    radio.displayTechnology == "5G NSA" || radio.nrState == 3 -> context.getString(R.string.monitortrace_reason_nsa)
    !carrierNr -> context.getString(R.string.monitortrace_reason_carrier_nr)
    !localNrOpen -> context.getString(R.string.monitortrace_reason_gates)
    radio.dcNrRestricted == true -> context.getString(R.string.monitortrace_reason_dcnr)
    radio.nrAvailable == true && radio.endcAvailable == false ->
        context.getString(R.string.monitortrace_reason_endc_missing)
    radio.nrAvailable == false -> context.getString(R.string.monitortrace_reason_nr_missing)
    radio.endcAvailable == true ->
        context.getString(R.string.monitortrace_reason_endc_idle)
    else -> context.getString(R.string.monitortrace_reason_unknown)
}

@Composable
private fun TraceStep(label: String, passed: Boolean) {
    Text(
        "${if (passed) "✓" else "✕"}  $label",
        style = MaterialTheme.typography.bodyMedium,
        color = if (passed) statusToneColor(StatusTone.SUCCESS) else statusToneColor(StatusTone.DANGER),
    )
}

/**
 * A label and the state the framework reports for it.
 *
 * The value is monospaced: these are readings, and a column of them should line
 * up rather than drift as each one changes length.
 */
@Composable
private fun DiagnosticRow(label: String, value: String, good: Boolean?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.42f))
        Text(
            value,
            style = NumericSmallTextStyle,
            modifier = Modifier.weight(0.58f),
            color = when (good) {
                true -> statusToneColor(StatusTone.SUCCESS)
                false -> statusToneColor(StatusTone.DANGER)
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private data class ConfigGate(
    val label: String,
    val key: String,
    val current: String,
    val androidDefault: String,
    val open: Boolean?,
)

@Composable
private fun CarrierConfigDiffPage(subscriptions: List<SubscriptionInfo>) {
    val context = LocalContext.current
    var selectedSubId by rememberSaveable { mutableStateOf(subscriptions.firstOrNull()?.subscriptionId ?: -1) }
    // Subscriptions can arrive (or change) after this page opens; never stay pinned to a missing SIM.
    LaunchedEffect(subscriptions) {
        if (subscriptions.none { it.subscriptionId == selectedSubId }) {
            selectedSubId = subscriptions.firstOrNull()?.subscriptionId ?: -1
        }
    }
    var rows by remember { mutableStateOf(emptyList<ConfigGate>()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSubId) {
        if (selectedSubId < 0) return@LaunchedEffect
        runCatching {
            withContext(Dispatchers.IO) { carrierConfigRows(context, selectedSubId) }
        }.onSuccess {
            rows = it
            error = null
        }.onFailure { error = it.message }
    }

    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderText(stringResource(R.string.monitortrace_config_title))
        SubscriptionPicker(subscriptions, selectedSubId) { selectedSubId = it }
        Text(
            stringResource(R.string.monitortrace_config_intro),
            style = MaterialTheme.typography.bodySmall,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        rows.forEach {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DiagnosticRow(it.label, it.current, it.open)
                    Text(it.key, style = MaterialTheme.typography.labelSmall)
                    Text(
                        stringResource(R.string.monitortrace_config_android_default, it.androidDefault) +
                            if (it.current != it.androidDefault) stringResource(R.string.monitortrace_config_overridden) else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun carrierConfigRows(context: Context, subId: Int): List<ConfigGate> {
    val moder = SubscriptionModer(context, subId)
    val defaults = CarrierConfigManager.getDefaultConfig()
    fun value(key: String): Any? = runCatching { moder.getValue(key) }.getOrNull()
    fun text(value: Any?): String = when (value) {
        is IntArray -> value.joinToString(prefix = "[", postfix = "]")
        is BooleanArray -> value.joinToString(prefix = "[", postfix = "]")
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]")
        null -> context.getString(R.string.monitortrace_value_not_exposed)
        else -> value.toString()
    }
    fun defaultText(key: String): String = text(defaults[key])
    val nrKey = CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY
    val nrModes = moder.getIntArrayValue(nrKey)
    val gates = moder.getRootForceReport().gates
    val nrAllowed = gates.filter { it.mask != null }.all { it.nrAllowed }
    val hide5gInferred = nrModes.isEmpty()
    val entries = listOf(
        ConfigGate(context.getString(R.string.monitortrace_gate_allow_nr), "allowed_network_types + $nrKey", nrAllowed.toString(), "true", nrAllowed),
        ConfigGate(context.getString(R.string.monitortrace_gate_carrier_nr), nrKey, text(nrModes), defaultText(nrKey), nrModes.isNotEmpty()),
        ConfigGate(
            context.getString(R.string.monitortrace_gate_hide_5g),
            "inferred: carrier NR modes empty",
            hide5gInferred.toString(),
            "false",
            !hide5gInferred,
        ),
        ConfigGate(
            context.getString(R.string.monitortrace_gate_vonr),
            CarrierConfigManager.KEY_VONR_ENABLED_BOOL,
            text(value(CarrierConfigManager.KEY_VONR_ENABLED_BOOL)),
            defaultText(CarrierConfigManager.KEY_VONR_ENABLED_BOOL),
            moder.getBooleanValue(CarrierConfigManager.KEY_VONR_ENABLED_BOOL),
        ),
        ConfigGate(
            context.getString(R.string.monitortrace_gate_volte),
            CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL,
            text(value(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL)),
            defaultText(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL),
            moder.getBooleanValue(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL),
        ),
        ConfigGate(
            context.getString(R.string.monitortrace_gate_vowifi),
            CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL,
            text(value(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL)),
            defaultText(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL),
            moder.getBooleanValue(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL),
        ),
        ConfigGate(
            context.getString(R.string.monitortrace_gate_lte_plus_icon),
            CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL,
            text(value(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL)),
            defaultText(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL),
            !moder.getBooleanValue(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL),
        ),
        ConfigGate(
            context.getString(R.string.monitortrace_gate_vowifi_icon),
            CarrierConfigManager.KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL,
            text(value(CarrierConfigManager.KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL)),
            defaultText(CarrierConfigManager.KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL),
            moder.getBooleanValue(CarrierConfigManager.KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL),
        ),
    )
    return entries
}

@Composable
private fun NrCapabilitiesPage(subscriptions: List<SubscriptionInfo>) {
    val context = LocalContext.current
    var selectedSubId by rememberSaveable { mutableStateOf(subscriptions.firstOrNull()?.subscriptionId ?: -1) }
    // Subscriptions can arrive (or change) after this page opens; never stay pinned to a missing SIM.
    LaunchedEffect(subscriptions) {
        if (subscriptions.none { it.subscriptionId == selectedSubId }) {
            selectedSubId = subscriptions.firstOrNull()?.subscriptionId ?: -1
        }
    }
    var radio by remember { mutableStateOf<SubscriptionModer.RadioDiagnostics?>(null) }
    var gates by remember { mutableStateOf<SubscriptionModer.RootForceReport?>(null) }
    var endcControl by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(selectedSubId) {
        if (selectedSubId < 0) return@LaunchedEffect
        runCatching {
            withContext(Dispatchers.IO) {
                val moder = SubscriptionModer(context, selectedSubId)
                val current = moder.getRadioDiagnostics()
                val manager = context.getSystemService(TelephonyManager::class.java)
                    .createForSubscriptionId(selectedSubId)
                Triple(
                    current,
                    moder.getRootForceReport(current),
                    runCatching {
                        manager.isRadioInterfaceCapabilitySupported(
                            "CAPABILITY_NR_DUAL_CONNECTIVITY_CONFIGURATION_AVAILABLE",
                        )
                    }.getOrNull(),
                )
            }
        }.onSuccess {
            radio = it.first
            gates = it.second
            endcControl = it.third
        }
    }

    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderText(stringResource(R.string.monitortrace_capabilities_title))
        SubscriptionPicker(subscriptions, selectedSubId) { selectedSubId = it }
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.monitortrace_device_title), style = MaterialTheme.typography.titleMedium)
                DiagnosticRow(stringResource(R.string.monitortrace_cap_nsa), stateText(context, gates?.carrierNsa), gates?.carrierNsa)
                DiagnosticRow(stringResource(R.string.monitortrace_cap_sa), stateText(context, gates?.carrierSa), gates?.carrierSa)
                DiagnosticRow(
                    stringResource(R.string.monitortrace_cap_endc_control),
                    endcControl?.let { if (it) stringResource(R.string.monitortrace_cap_endc_hal_supported) else stringResource(R.string.monitortrace_cap_endc_hal_missing) } ?: stringResource(R.string.monitortrace_value_unknown),
                    endcControl,
                )
                DiagnosticRow(
                    stringResource(R.string.monitortrace_cap_endc_now),
                    stateText(context, radio?.endcAvailable),
                    radio?.endcAvailable,
                )
                DiagnosticRow(
                    stringResource(R.string.monitortrace_cap_dcnr_now),
                    radio?.dcNrRestricted?.let { if (it) stringResource(R.string.monitortrace_cap_dcnr_restricted) else stringResource(R.string.monitortrace_cap_dcnr_open) } ?: stringResource(R.string.monitortrace_value_unknown),
                    radio?.dcNrRestricted?.not(),
                )
                DiagnosticRow(
                    stringResource(R.string.monitortrace_cap_vonr),
                    if (selectedSubId >= 0) {
                        stateText(
                            context,
                            runCatching {
                                SubscriptionModer(context, selectedSubId)
                                    .getBooleanValue(CarrierConfigManager.KEY_VONR_ENABLED_BOOL)
                            }.getOrNull(),
                        )
                    } else {
                        stringResource(R.string.monitortrace_value_unknown)
                    },
                    null,
                )
                DiagnosticRow(
                    stringResource(R.string.monitortrace_cap_nr_connection),
                    radio?.displayTechnology ?: stringResource(R.string.monitortrace_value_unknown),
                    radio?.displayTechnology?.startsWith("5G"),
                )
            }
        }
        Text(
            stringResource(R.string.monitortrace_configured_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun stateText(context: Context, value: Boolean?): String = when (value) {
    true -> context.getString(R.string.monitortrace_state_supported)
    false -> context.getString(R.string.monitortrace_state_not_enabled)
    null -> context.getString(R.string.monitortrace_value_unknown)
}
