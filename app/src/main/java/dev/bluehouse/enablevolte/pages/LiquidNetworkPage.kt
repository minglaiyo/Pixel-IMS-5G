package dev.bluehouse.enablevolte.pages

import android.app.Application
import android.content.Context
import android.telephony.SubscriptionInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import dev.bluehouse.enablevolte.*
import dev.bluehouse.enablevolte.R
import dev.bluehouse.enablevolte.components.GlassDepth
import dev.bluehouse.enablevolte.components.Sparkline
import dev.bluehouse.enablevolte.components.liquidGlass
import dev.bluehouse.enablevolte.components.LiquidPane as LivePane
import dev.bluehouse.enablevolte.ui.theme.LocalInstrument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Reading(val name: String, val value: String, val meaning: String)
private data class ReadingSheet(val title: String, val explanation: String, val readings: List<Reading>, val raw: String? = null, val sourceKey: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkPage(subscriptions: List<SubscriptionInfo>, navController: NavController) {
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableIntStateOf(-1) }
    val sub = subscriptions.firstOrNull { it.subscriptionId == selectedId } ?: subscriptions.firstOrNull()
    if (sub == null) {
        LivePane(Modifier.padding(20.dp)) { Text(stringResource(R.string.network_no_sim)) }
        return
    }
    val vm: NetworkMonitorViewModel = viewModel(key = "network-${sub.subscriptionId}",
        factory = NetworkMonitorViewModel.factory(context.applicationContext as Application, sub.subscriptionId))
    val ui by vm.state.collectAsState()
    var userPaused by rememberSaveable(sub.subscriptionId) { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    val pausedNow by rememberUpdatedState(userPaused)
    DisposableEffect(vm, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && !pausedNow) vm.start()
            if (event == Lifecycle.Event.ON_STOP) vm.stop()
        }
        owner.lifecycle.addObserver(observer)
        if (!userPaused && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) vm.start()
        onDispose { owner.lifecycle.removeObserver(observer); vm.stop() }
    }
    var sheet by remember(sub.subscriptionId) { mutableStateOf<ReadingSheet?>(null) }


    val connection = ui.connection
    val radio = ui.radio
    val name = liveCarrierName(radio?.operatorName.orEmpty(), radio?.operatorNumeric ?: "${sub.mccString}${sub.mncString}", sub.carrierName.toString())
    LaunchedEffect(ui.lastSampleAt) {
        val current = sheet ?: return@LaunchedEffect
        sheet = when {
            current.title == context.getString(R.string.network_sheet_connection_title) -> connectionSheet(context, ui)
            current.title == context.getString(R.string.network_sheet_signal_title) -> signalSheet(context, ui.serving)
            current.title == context.getString(R.string.network_sheet_all_title) -> allMeasurementsSheet(context, ui, name)
            current.sourceKey != null -> ui.connection?.carriers?.firstOrNull { it.key == current.sourceKey }?.let { carrierSheet(context, it) }
                ?: ui.neighbours.firstOrNull { cellKey(it) == current.sourceKey }?.let { nearbySheet(context, it) }
                ?: current.copy(explanation = context.getString(R.string.network_carrier_gone))
            else -> current
        }
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val good = if (LocalInstrument.current.isDark) Color(0xFF52D99B) else Color(0xFF087B4F)
    val caution = if (LocalInstrument.current.isDark) Color(0xFFFFC466) else Color(0xFF8C5300)

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.network_page_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp, modifier = Modifier.weight(1f))
            Row(Modifier.liquidGlass(RoundedCornerShape(24.dp), GlassDepth.THICK).clickable { userPaused = !userPaused; if (userPaused) vm.stop() else vm.start() }.padding(horizontal = 15.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (ui.sampling && ui.available) good else caution, CircleShape))
                Text(stringResource(if (ui.sampling) R.string.network_status_live else R.string.network_status_paused), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(28.dp), GlassDepth.THICK).horizontalScroll(rememberScrollState()).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            subscriptions.forEach { sim ->
                val selected = sim.subscriptionId == sub.subscriptionId
                val label = liveCarrierName(sim.carrierName.toString(), "${sim.mccString}${sim.mncString}", "SIM ${sim.simSlotIndex + 1}")
                Box(Modifier.then(if (selected) Modifier.liquidGlass(RoundedCornerShape(23.dp), GlassDepth.TINTED, accent) else Modifier).clickable { selectedId = sim.subscriptionId }.padding(horizontal = 16.dp, vertical = 13.dp)) {
                    Text("$label · SIM ${sim.simSlotIndex + 1}", fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) accent else muted)
                }
            }
        }
        ui.message?.let { LivePane { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) } }
        LivePane {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(radio?.operatorNumeric?.let { if (it.length >= 5) "${it.take(3)}-${it.drop(3)}" else it }.orEmpty(), color = muted, fontSize = 12.sp)
            }
            val title = when {
                !ui.available && !ui.loading -> stringResource(R.string.network_readings_unavailable)
                radio?.serviceState == "Radio off" -> stringResource(R.string.network_radio_off_title)
                radio?.serviceState in listOf("Out of service", "Emergency only") -> radio?.serviceState.orEmpty()
                else -> connection?.let { if (it.titleArg != null) stringResource(it.titleRes, it.titleArg) else stringResource(it.titleRes) } ?: stringResource(R.string.network_reading_network)
            }
            Text(title, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp)
            Text(if (!ui.sampling) stringResource(R.string.network_paused_reading) else connection?.let { stringResource(it.explanationRes) + (it.explanationSuffixRes?.let { r -> stringResource(r) } ?: "") } ?: stringResource(R.string.network_waiting_radio), color = muted, fontSize = 14.sp, lineHeight = 20.sp)
            HorizontalDivider(color = muted.copy(alpha = .13f), modifier = Modifier.padding(vertical = 3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(if (ui.nrAttached) good else caution, CircleShape))
                Text(connection?.let { stringResource(it.fiveGStatusRes) } ?: stringResource(R.string.network_fiveg_not_reported), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text(stringResource(R.string.network_what_does_this_mean), color = accent, fontSize = 13.sp, modifier = Modifier.clickable { sheet = connectionSheet(context, ui) }.padding(vertical = 5.dp))
        }
        LivePane {
            val bandwidth = connection?.caBandwidth
            val count = bandwidth?.componentCount ?: 0
            SectionTitle(when { count > 1 -> stringResource(R.string.network_ca_multi, count); connection?.caActive == true -> stringResource(R.string.network_ca_reported_incomplete); else -> stringResource(R.string.network_lte_carrier_report) })
            Text(connection?.bandCombination?.takeIf { it.isNotBlank() } ?: stringResource(R.string.network_bands_not_reported), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(when { count > 1 -> stringResource(R.string.network_ca_multi_expl, count); count == 1 -> stringResource(R.string.network_ca_single_expl); else -> stringResource(R.string.network_ca_none_expl) }, fontSize = 12.sp, lineHeight = 17.sp, color = muted)
            if (count > 0) Text(bandwidth!!.componentWidthsKhz.joinToString(" + ") { it?.let { width -> formatNumber(width / 1000.0) } ?: "?" } +
                " MHz" + (bandwidth.totalKhz?.let { stringResource(R.string.network_ca_total, formatNumber(it / 1000.0)) } ?: stringResource(R.string.network_ca_total_unknown)),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent)
            if (bandwidth != null && ui.lastSampleAt > 0) Text("${stringResource(bandwidth.sourceRes)} · ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ui.lastSampleAt))}", fontSize = 11.sp, color = muted)
            Text(if (ui.nrAttached) connection?.let { stringResource(it.explanationRes) + (it.explanationSuffixRes?.let { r -> stringResource(r) } ?: "") }.orEmpty() else stringResource(R.string.network_fiveg_not_connected), fontSize = 12.sp, lineHeight = 17.sp, color = muted)
            if (connection?.caActive == true && count < 2) Text(stringResource(R.string.network_ca_android_note), fontSize = 12.sp, color = caution)
        }
        connection?.carriers?.forEachIndexed { index, carrier ->
            LivePane {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${carrier.band} · ${carrier.rat}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = accent, modifier = Modifier.weight(1f))
                    Text(when { carrier.rat == "NR" -> "5G"; carrier.roleRes == R.string.root_role_4g_anchor -> stringResource(R.string.network_role_lte_anchor); carrier.roleRes == R.string.root_role_extra_4g_carrier -> stringResource(R.string.network_role_scc); else -> stringResource(R.string.network_role_pcc) }, fontSize = 12.sp, color = muted)
                }
                Text(when { carrier.rat == "NR" -> stringResource(R.string.network_carrier_nr_expl); carrier.roleRes == R.string.root_role_4g_anchor -> stringResource(R.string.network_carrier_anchor_expl); carrier.roleRes == R.string.root_role_extra_4g_carrier -> stringResource(R.string.network_carrier_scc_expl); else -> stringResource(R.string.network_carrier_pcc_expl) }, fontSize = 12.sp, lineHeight = 17.sp, color = muted)
                ReadingGrid(carrierSheet(context, carrier).readings.distinctBy { it.name })
            }
        }
        LivePane {
            SectionTitle(stringResource(R.string.network_signal_title))
            val cell = ui.serving
            val dbm = (cell?.rsrp ?: cell?.dbm)?.takeIf { it != Int.MAX_VALUE }
            val clarity = cell?.sinr ?: cell?.csiSinr
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f).clickable { sheet = signalSheet(context, cell) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.network_signal_strength), color = muted, fontSize = 13.sp)
                    Text(radioReading(dbm, "dBm"), fontWeight = FontWeight.Bold, fontSize = if (dbm == null) 17.sp else 24.sp)
                    val wordRes = when { dbm == null -> R.string.network_signal_no_reading; dbm >= -95 -> R.string.network_signal_good; dbm >= -105 -> R.string.network_signal_fair; else -> R.string.network_signal_weak }
                    Text(stringResource(wordRes), color = if (wordRes == R.string.network_signal_good) good else caution, fontSize = 13.sp)
                }
                Column(Modifier.weight(1f).clickable { sheet = signalSheet(context, cell) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.network_signal_clarity), color = muted, fontSize = 13.sp)
                    Text(radioReading(clarity, "dB"), fontWeight = FontWeight.Bold, fontSize = if (clarity == null) 17.sp else 24.sp)
                    Text(when { clarity == null -> stringResource(R.string.network_signal_no_reading); clarity >= 10 -> stringResource(R.string.network_signal_good); clarity >= 0 -> stringResource(R.string.network_signal_fair); else -> stringResource(R.string.network_signal_noisy) }, color = if ((clarity ?: -1) >= 10) good else caution, fontSize = 13.sp)
                }
            }
            if (ui.rsrpHistory.size > 1) {
                Text(stringResource(R.string.network_signal_history, ui.rsrpHistory.size), color = muted, fontSize = 11.sp)
                Sparkline(ui.rsrpHistory, modifier = Modifier.fillMaxWidth().height(46.dp))
                Text(stringResource(R.string.network_signal_history_range, radioReading(ui.historyMin), radioReading(ui.historyMean), radioReading(ui.historyMax)), color = muted, fontSize = 11.sp)
            }
            Text(stringResource(R.string.network_signal_not_speed), color = muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        LivePane {
            SectionTitle(stringResource(R.string.network_registration_title))
            Text(stringResource(R.string.network_registration_sub), fontSize = 12.sp, color = muted)
            ReadingGrid(connectionSheet(context, ui).readings, explain = true)
        }
        LivePane {
            SectionTitle(stringResource(R.string.network_nearby_title, ui.neighbours.size))
            Text(stringResource(R.string.network_nearby_expl), fontSize = 12.sp, lineHeight = 17.sp, color = muted)
            if (ui.neighbours.isEmpty()) Text(stringResource(R.string.network_nearby_empty), fontSize = 13.sp)
        }
        ui.neighbours.forEach { c ->
            LivePane {
                SectionTitle("${c.type} ${c.band} · PCI ${radioReading(c.pci)}")
                Text(stringResource(R.string.network_nearby_operator, c.operator.ifBlank { stringResource(R.string.network_operator_unknown) }), fontSize = 12.sp, color = muted)
                ReadingGrid(cellReadings(context, c))
            }
        }
        LivePane {
            SectionTitle(stringResource(R.string.network_howto_title))
            Text(stringResource(R.string.network_howto_body), fontSize = 12.sp, lineHeight = 20.sp, color = muted)
        }
        LivePane {
            SectionTitle(stringResource(R.string.network_history_title, ui.events.size))
            if (ui.events.isEmpty()) Text(stringResource(R.string.network_history_empty), fontSize = 13.sp)
            ui.events.take(30).forEach { event ->
                Text("${event.clock} · ${event.title}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(event.detail, fontSize = 12.sp, color = muted)
            }
            TextButton(onClick = vm::clearLog) { Text(stringResource(R.string.network_history_clear)) }
        }
        Disclosure(stringResource(R.string.network_deep_diagnostics), stringResource(R.string.network_deep_diagnostics_sub)) { navController.navigate("network/diagnostics") }
        if (ui.lastSampleAt > 0) Text("${stringResource(if (ui.sampling && ui.available) R.string.network_updated else R.string.network_last_reading)} ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ui.lastSampleAt))} · ${ui.serviceState}", fontSize = 11.sp, color = muted, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(18.dp))
    }
    sheet?.let { details ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(details.title, fontWeight = FontWeight.Bold, fontSize = 25.sp)
                Text(details.explanation, fontSize = 14.sp, lineHeight = 20.sp, color = muted)
                details.readings.forEach { reading ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(reading.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(reading.value, fontSize = 14.sp, color = accent, modifier = Modifier.weight(1f))
                        }
                        Text(reading.meaning, fontSize = 12.sp, color = muted, lineHeight = 17.sp)
                    }
                    HorizontalDivider(color = muted.copy(alpha = .12f))
                }
                details.raw?.let { raw ->
                    var expanded by remember(details.title) { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }) { Text(stringResource(if (expanded) R.string.network_sheet_hide_source else R.string.network_sheet_show_source)) }
                    if (expanded) SelectionContainer { Text(raw, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                }
                TextButton(onClick = { sheet = null }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.network_sheet_done)) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-.3).sp)
}
@Composable private fun ReadingGrid(readings: List<Reading>, explain: Boolean = false) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    readings.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { reading ->
                Column(Modifier.weight(1f).background(accent.copy(alpha = .055f), RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(reading.name, fontSize = 11.sp, lineHeight = 15.sp, color = muted)
                    Text(reading.value, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    if (explain) Text(reading.meaning, fontSize = 11.sp, lineHeight = 15.sp, color = muted)
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}
@Composable private fun Disclosure(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(23.dp), GlassDepth.THICK).clickable(onClick = onClick).padding(horizontal = 17.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
private fun formatNumber(value: Double) = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
private fun flag(context: Context, value: Boolean?) = when (value) { true -> context.getString(R.string.network_flag_yes); false -> context.getString(R.string.network_flag_no); null -> context.getString(R.string.network_not_reported) }
private fun cellKey(c: SubscriptionModer.CellSnapshot) = "${c.type}:${c.channel}:${c.pci}:${c.band}"
private fun nearbySheet(context: Context, c: SubscriptionModer.CellSnapshot) = ReadingSheet("${c.type} ${c.band}", context.getString(R.string.network_sheet_nearby_expl), cellReadings(context, c), sourceKey = cellKey(c))
private fun allMeasurementsSheet(context: Context, ui: NetworkState, name: String) = ReadingSheet(context.getString(R.string.network_sheet_all_title), context.getString(R.string.network_sheet_all_expl, name),
    connectionSheet(context, ui).readings + (ui.serving?.let { cellReadings(context, it) } ?: emptyList()) + ui.physicalChannels.map { p ->
        Reading(context.getString(R.string.network_sheet_physical_name, p.rat, p.band?.toString() ?: context.getString(R.string.network_band_hidden)), context.getString(R.string.network_sheet_physical_value, radioReading(p.channel), radioReading(p.pci)),
            context.getString(R.string.network_sheet_physical_expl))
    }, ui.radio?.serviceStateSummary)
private fun connectionSheet(context: Context, ui: NetworkState): ReadingSheet {
    val r = ui.radio
    return ReadingSheet(context.getString(R.string.network_sheet_connection_title), context.getString(R.string.network_sheet_connection_expl), listOf(
        Reading(context.getString(R.string.network_reading_connection), ui.connection?.let { if (it.titleArg != null) context.getString(it.titleRes, it.titleArg) else context.getString(it.titleRes) } ?: context.getString(R.string.network_not_reported), ui.connection?.let { context.getString(it.explanationRes) + (it.explanationSuffixRes?.let { r -> context.getString(r) } ?: "") }.orEmpty()),
        Reading(context.getString(R.string.network_reading_service), ui.serviceState.ifBlank { context.getString(R.string.network_not_reported) }, context.getString(R.string.network_reading_service_expl)),
        Reading(context.getString(R.string.network_reading_data_radio), r?.dataRat ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_data_radio_expl)),
        Reading(context.getString(R.string.network_reading_nr_state), when (r?.nrState) { 0 -> context.getString(R.string.network_nr_none); 1 -> context.getString(R.string.network_nr_restricted); 2 -> context.getString(R.string.network_nr_available_not_connected); 3 -> context.getString(R.string.network_nr_connected); else -> context.getString(R.string.network_not_reported) }, context.getString(R.string.network_reading_nr_state_expl)),
        Reading(context.getString(R.string.network_reading_reg_channel), radioReading(r?.channelNumber), context.getString(R.string.network_reading_reg_channel_expl)),
        Reading(context.getString(R.string.network_reading_duplex), when(r?.duplexMode) { 1 -> "FDD"; 2 -> "TDD"; else -> context.getString(R.string.network_not_reported) }, context.getString(R.string.network_reading_duplex_expl)),
        Reading(context.getString(R.string.network_reading_net_services), r?.registrationServices?.joinToString { when(it) {1 -> context.getString(R.string.network_service_voice); 2 -> context.getString(R.string.network_service_data); 3 -> context.getString(R.string.network_service_sms); 4 -> context.getString(R.string.network_service_video); 5 -> context.getString(R.string.network_service_emergency); else -> context.getString(R.string.network_service_code, it)} }?.ifBlank { context.getString(R.string.network_not_reported) } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_net_services_expl)),
        Reading(context.getString(R.string.network_reading_ca), flag(context, ui.connection?.caActive), context.getString(R.string.network_reading_ca_expl)),
        Reading(context.getString(R.string.network_reading_reg_bw), r?.cellBandwidthsKhz?.takeIf { it.isNotEmpty() }?.joinToString(" + ", postfix = " MHz") { formatNumber(it / 1000.0) } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_reg_bw_expl)),
        Reading(context.getString(R.string.network_reading_fiveg_state), ui.connection?.let { context.getString(it.fiveGStatusRes) } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_fiveg_state_expl)),
        Reading(context.getString(R.string.network_reading_nr_available), flag(context, r?.nrAvailable), context.getString(R.string.network_reading_nr_available_expl)),
        Reading(context.getString(R.string.network_reading_endc), flag(context, r?.endcAvailable), context.getString(R.string.network_reading_endc_expl)),
        Reading(context.getString(R.string.network_reading_nr_restricted), flag(context, r?.dcNrRestricted), context.getString(R.string.network_reading_nr_restricted_expl)),
        Reading(context.getString(R.string.network_reading_ims), if (r?.imsRegistered == true) context.getString(R.string.network_ims_registered, r.imsTransport) else context.getString(R.string.network_ims_not_registered), context.getString(R.string.network_reading_ims_expl)),
        Reading(context.getString(R.string.network_reading_roaming), flag(context, r?.roaming), context.getString(R.string.network_reading_roaming_expl)),
        Reading(context.getString(R.string.network_reading_plmn), r?.registeredPlmn ?: r?.operatorNumeric ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_plmn_expl)),
        Reading(context.getString(R.string.network_reading_reject), radioReading(r?.registrationRejectCause), context.getString(R.string.network_reading_reject_expl)),
        Reading(context.getString(R.string.network_reading_searching), flag(context, r?.networkSearching), context.getString(R.string.network_reading_searching_expl)),
        Reading(context.getString(R.string.network_reading_ntn), flag(context, r?.nonTerrestrialNetwork), context.getString(R.string.network_reading_ntn_expl)),
    ))
}
private fun carrierSheet(context: Context, c: LiveCarrier): ReadingSheet = ReadingSheet(context.getString(c.roleRes),
    "${c.rat} ${c.band}. " + context.getString(if (c.physical != null) R.string.network_carrier_expl_physical else R.string.network_carrier_expl_evidence) + " " + context.getString(R.string.network_carrier_expl_suffix), buildList {
        add(Reading(context.getString(R.string.network_reading_band), c.band, context.getString(R.string.network_reading_band_expl)))
        add(Reading(context.getString(if (c.rat == "NR") R.string.network_reading_nrarfcn else R.string.network_reading_earfcn), radioReading(c.channel), context.getString(R.string.network_reading_channel_expl)))
        add(Reading(context.getString(R.string.network_reading_pci), radioReading(c.pci), context.getString(R.string.network_reading_pci_expl)))
        add(Reading(context.getString(R.string.network_reading_dl_bw), c.bandwidthKhz?.let { "${formatNumber(it / 1000.0)} MHz" } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_dl_bw_expl)))
        c.physical?.let { p ->
            add(Reading(context.getString(R.string.network_reading_ul_channel), radioReading(p.uplinkChannel), context.getString(R.string.network_reading_ul_channel_expl)))
            add(Reading(context.getString(R.string.network_reading_ul_bw), p.uplinkBandwidthKhz?.let { "${formatNumber(it / 1000.0)} MHz" } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_ul_bw_expl)))
            add(Reading(context.getString(R.string.network_reading_dl_freq), p.downlinkFrequencyKhz?.let { "${formatNumber(it / 1000.0)} MHz" } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_dl_freq_expl)))
            add(Reading(context.getString(R.string.network_reading_ul_freq), p.uplinkFrequencyKhz?.let { "${formatNumber(it / 1000.0)} MHz" } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_ul_freq_expl)))
        }
        c.cell?.let { addAll(cellReadings(context, it)) }
    }, sourceKey = c.key)
private fun signalSheet(context: Context, c: SubscriptionModer.CellSnapshot?) = ReadingSheet(context.getString(R.string.network_sheet_signal_title), context.getString(R.string.network_sheet_signal_expl), c?.let { cellReadings(context, it) } ?: listOf(Reading(context.getString(R.string.network_reading_signal), context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_signal_expl))))
private fun cellReadings(context: Context, c: SubscriptionModer.CellSnapshot): List<Reading> = buildList {
    add(Reading(context.getString(R.string.network_reading_operator), c.operator.ifBlank { context.getString(R.string.network_not_reported) }, context.getString(R.string.network_reading_operator_expl)))
    add(Reading(context.getString(R.string.network_reading_rsrp), radioReading(c.rsrp, "dBm"), context.getString(R.string.network_reading_rsrp_expl)))
    add(Reading(context.getString(R.string.network_reading_rsrq), radioReading(c.rsrq, "dB"), context.getString(R.string.network_reading_rsrq_expl)))
    add(Reading(context.getString(R.string.network_reading_sinr), radioReading(c.sinr, "dB"), context.getString(R.string.network_reading_sinr_expl)))
    add(Reading(context.getString(R.string.network_reading_rssi), radioReading(c.rssi, "dBm"), context.getString(R.string.network_reading_rssi_expl)))
    add(Reading(context.getString(R.string.network_reading_cqi), radioReading(c.cqi), context.getString(R.string.network_reading_cqi_expl)))
    add(Reading(context.getString(R.string.network_reading_level), radioReading(c.level, "/ 4"), context.getString(R.string.network_reading_level_expl)))
    add(Reading(context.getString(R.string.network_reading_channel), radioReading(c.channel), context.getString(R.string.network_reading_channel_expl2)))
    add(Reading(context.getString(R.string.network_reading_dl_freq), c.frequencyKhz?.let { "${formatNumber(it / 1000.0)} MHz" } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_freq_expl)))
    add(Reading(context.getString(R.string.network_reading_pci), radioReading(c.pci), context.getString(R.string.network_reading_pci_expl_cell)))
    add(Reading(context.getString(R.string.network_reading_tac), radioReading(c.tac), context.getString(R.string.network_reading_tac_expl)))
    add(Reading(context.getString(R.string.network_reading_cellid), c.cellId.takeUnless { it in listOf("2147483647", "9223372036854775807", "-1") } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_cellid_expl)))
    if (c.type == "LTE") c.cellId.toLongOrNull()?.takeIf { it >= 0 && it < 268435456 }?.let {
        add(Reading(context.getString(R.string.network_reading_enb), "${it / 256} / ${it % 256}", context.getString(R.string.network_reading_enb_expl)))
    }
    add(Reading(context.getString(R.string.network_reading_bandwidth), c.bandwidthKhz?.let { "${formatNumber(it / 1000.0)} MHz" } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_bandwidth_expl)))
    add(Reading(context.getString(R.string.network_reading_ta), radioReading(c.timingAdvance, if (c.type == "NR") "µs" else "steps"), context.getString(R.string.network_reading_ta_expl)))
    add(Reading(context.getString(R.string.network_reading_reported_bands), c.allBands.takeUnless { it == "[]" || it.isBlank() } ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_reported_bands_expl)))
    add(Reading(context.getString(R.string.network_reading_conn_role), when(c.connectionStatus) { 1 -> context.getString(R.string.network_role_primary); 2 -> context.getString(R.string.network_role_secondary); 0 -> context.getString(R.string.network_role_not_serving); else -> context.getString(R.string.network_not_reported) }, context.getString(R.string.network_reading_conn_role_expl)))
    if (c.type == "NR") {
        add(Reading(context.getString(R.string.network_reading_csi_rsrp), radioReading(c.csiRsrp, "dBm"), context.getString(R.string.network_reading_csi_rsrp_expl)))
        add(Reading(context.getString(R.string.network_reading_csi_rsrq), radioReading(c.csiRsrq, "dB"), context.getString(R.string.network_reading_csi_rsrq_expl)))
        add(Reading(context.getString(R.string.network_reading_csi_sinr), radioReading(c.csiSinr, "dB"), context.getString(R.string.network_reading_csi_sinr_expl)))
        add(Reading(context.getString(R.string.network_reading_csi_table), radioReading(c.csiCqiTable), context.getString(R.string.network_reading_csi_table_expl)))
        add(Reading(context.getString(R.string.network_reading_csi_report), c.csiCqiReport ?: context.getString(R.string.network_not_reported), context.getString(R.string.network_reading_csi_report_expl)))
    }
}

