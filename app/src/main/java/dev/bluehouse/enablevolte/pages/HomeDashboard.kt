package dev.bluehouse.enablevolte.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bluehouse.enablevolte.*
import dev.bluehouse.enablevolte.components.GlassDepth
import dev.bluehouse.enablevolte.components.LiquidPane
import dev.bluehouse.enablevolte.components.liquidGlass
import dev.bluehouse.enablevolte.ui.theme.LocalInstrument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class HomeSim(val id: Int, val name: String, val slot: Int)

/** Pure content: previews cannot connect a privileged service or modify the phone. */
@Composable
internal fun HomeDashboard(
    sims: List<HomeSim>, selectedId: Int?, ui: SetupState, accessMode: String,
    onSelectSim: (Int) -> Unit, onRefresh: () -> Unit, onNavigate: (String) -> Unit,
    onDismissNote: () -> Unit, onFix: (ReadinessCheck) -> Unit,
    patchContent: @Composable () -> Unit = {},
) {
    val inst = LocalInstrument.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val selected = sims.firstOrNull { it.id == selectedId }
    val setupChecks = ui.checks.filterNot { it.id == "endc" || it.id == "ims_reg" }

    val refreshing = ui.loading || ui.refreshing
    val access = ui.checks.firstOrNull { it.id == "privilege" }
    val patch = ui.checks.firstOrNull { it.id == "patch" }
    val ims = ui.checks.firstOrNull { it.id == "ims" }
    val headline = when {
        selected == null -> stringResource(R.string.home_no_active_sim)
        ui.loading -> stringResource(R.string.home_checking_setup)
        access?.status == CheckStatus.FAIL -> stringResource(R.string.home_connect_access, accessMode)
        setupChecks.any { it.status == CheckStatus.FAIL } -> stringResource(R.string.home_review_setup)
        else -> stringResource(R.string.home_settings_checked)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home), fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = onRefresh, enabled = !refreshing && ui.busyFix == null,
                shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 12.dp)) {
                if (refreshing) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (refreshing) stringResource(R.string.checking) else stringResource(R.string.home_check_now), fontSize = 12.sp)
            }
        }
        if (sims.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(28.dp), GlassDepth.THICK)
                .horizontalScroll(rememberScrollState()).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sims.forEach { sim ->
                    val active = sim.id == selectedId
                    Box(Modifier.then(if (active) Modifier.liquidGlass(RoundedCornerShape(23.dp), GlassDepth.TINTED, accent) else Modifier)
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelectSim(sim.id) }).padding(horizontal = 16.dp, vertical = 13.dp)) {
                        Text(stringResource(R.string.home_sim_tab, sim.name, sim.slot), fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) accent else muted)
                    }
                }
            }
        }
        LiquidPane {
            Text(selected?.name ?: stringResource(R.string.home_setup_and_access), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(headline, fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp)
            Text(when {
                selected == null -> stringResource(R.string.home_desc_no_sim)
                ui.loading -> stringResource(R.string.home_desc_loading)
                access?.status == CheckStatus.FAIL -> checkDetail(access)
                else -> stringResource(R.string.home_desc_default)
            }, fontSize = 13.sp, lineHeight = 19.sp, color = muted)
            Text(if (ui.checkedAt > 0) "${stringResource(if (refreshing) R.string.home_updating_last_check else R.string.home_setup_checked)} ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ui.checkedAt))}"
                else stringResource(R.string.home_checks_no_live_5g), fontSize = 10.sp, color = muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMetric(stringResource(R.string.home_metric_5g_mode), ui.checks.firstOrNull { it.id == "nr_mode" }?.let { if (it.status == CheckStatus.PASS) stringResource(R.string.enabled) else if (it.status == CheckStatus.FAIL) stringResource(R.string.home_value_off) else stringResource(R.string.home_value_not_reported) } ?: if (ui.loading) stringResource(R.string.checking) else stringResource(R.string.home_value_not_reported),
                stringResource(R.string.home_metric_5g_mode_desc), accent, Modifier.weight(1f))
            HomeMetric(stringResource(R.string.home_metric_calling), when { ui.loading -> stringResource(R.string.checking); ims?.status == CheckStatus.PASS -> stringResource(R.string.home_value_ims_supported); ims?.status == CheckStatus.FAIL -> stringResource(R.string.home_value_not_supported); else -> stringResource(R.string.home_value_not_reported) },
                stringResource(R.string.home_metric_calling_desc), if (ims?.status == CheckStatus.PASS) inst.good else muted, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMetric(stringResource(R.string.home_metric_access, accessMode), when { ui.loading -> stringResource(R.string.checking); access?.status == CheckStatus.PASS -> stringResource(R.string.home_value_connected); access?.status == CheckStatus.FAIL -> stringResource(R.string.home_value_disconnected); else -> stringResource(R.string.home_value_not_reported) },
                stringResource(R.string.home_metric_access_desc), if (access?.status == CheckStatus.PASS) inst.good else muted, Modifier.weight(1f))
            HomeMetric(stringResource(R.string.patchcard_modem_compatibility), when { ui.loading -> stringResource(R.string.checking); patch?.status == CheckStatus.PASS -> stringResource(R.string.home_value_patch_mounted); patch == null -> stringResource(R.string.home_value_not_reported); accessMode == "Shizuku" -> stringResource(R.string.home_value_root_required); patch.status == CheckStatus.UNKNOWN -> stringResource(R.string.home_value_not_reported); else -> stringResource(R.string.home_value_check_below) },
                stringResource(R.string.home_metric_patch_desc), if (patch?.status == CheckStatus.PASS) inst.good else muted, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeShortcut(stringResource(R.string.home_shortcut_live_network), stringResource(R.string.home_shortcut_live_network_desc), Icons.Filled.SignalCellularAlt, sims.isNotEmpty(), { onNavigate("network") }, Modifier.weight(1f))
            HomeShortcut(stringResource(R.string.field_test_short), stringResource(R.string.home_shortcut_field_test_desc), Icons.Filled.Science, sims.isNotEmpty(), { onNavigate("field-test") }, Modifier.weight(1f))
        }
        ui.noteRes?.let { noteRes ->
            LiquidPane {
                Text(
                    when {
                        ui.noteArg != null -> stringResource(noteRes, ui.noteArg)
                        ui.noteArgRes != null -> stringResource(noteRes, stringResource(ui.noteArgRes))
                        else -> stringResource(noteRes)
                    },
                    fontSize = 13.sp, lineHeight = 19.sp,
                )
                TextButton(onClick = onDismissNote) { Text(stringResource(R.string.dismiss)) }
            }
        }
        if (selected != null) {
            Text(stringResource(R.string.home_setup_checks), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp))
            LiquidPane {
                Text(stringResource(R.string.home_setup_checks_intro), fontSize = 12.sp, lineHeight = 18.sp, color = muted)
                if (ui.loading) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.home_reading_sim), fontSize = 13.sp)
                }
                setupChecks.forEachIndexed { index, check ->
                    if (index > 0) HorizontalDivider(color = muted.copy(alpha = .12f), modifier = Modifier.padding(vertical = 6.dp))
                    HomeCheck(check, busy = ui.busyFix == check.fix && check.fix != FixAction.NONE,
                        enabled = ui.busyFix == null && !refreshing, onFix = { onFix(check) })
                }
            }
        }
        patchContent()
        LiquidPane {
            HomeLink(stringResource(R.string.controls), stringResource(R.string.home_link_controls_desc), Icons.Filled.Tune) { onNavigate("controls") }
            HorizontalDivider(color = muted.copy(alpha = .12f))
            HomeLink(stringResource(R.string.how_to_use), stringResource(R.string.home_link_howto_desc), Icons.AutoMirrored.Filled.MenuBook) { onNavigate("home/how-to") }
        }
        Text("Pixel IMS 5G · ${BuildConfig.VERSION_NAME}", fontSize = 10.sp, color = muted, modifier = Modifier.padding(horizontal = 3.dp))
        Spacer(Modifier.height(18.dp))
    }
}

@Composable private fun HomeMetric(label: String, value: String, meaning: String, tone: Color, modifier: Modifier) {
    LiquidPane(modifier) {
        Text(label, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = tone)
        Text(meaning, fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun HomeShortcut(title: String, detail: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.liquidGlass(RoundedCornerShape(25.dp), GlassDepth.THICK).clickable(enabled = enabled, onClick = onClick)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, Modifier.size(23.dp), tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(detail, fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Resolves a check's resource-based copy; detailText wins for runtime patch messages. */
@Composable private fun checkDetail(check: ReadinessCheck): String = check.detailText ?: when {
    check.detailArgRes != null -> stringResource(check.detailRes, stringResource(check.detailArgRes))
    check.detailArg != null -> stringResource(check.detailRes, check.detailArg)
    else -> stringResource(check.detailRes)
}

@Composable private fun HomeCheck(check: ReadinessCheck, busy: Boolean, enabled: Boolean, onFix: () -> Unit) {
    val inst = LocalInstrument.current
    val tone = when (check.status) { CheckStatus.PASS -> inst.good; CheckStatus.FAIL -> if (check.id == "endc") inst.fair else inst.poor; CheckStatus.WARN -> inst.fair; CheckStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 3.dp).size(23.dp).background(tone.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(when (check.status) { CheckStatus.PASS -> Icons.Filled.Check; CheckStatus.FAIL, CheckStatus.WARN -> Icons.Filled.PriorityHigh; CheckStatus.UNKNOWN -> Icons.Filled.QuestionMark }, null, Modifier.size(14.dp), tint = tone)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(stringResource(check.titleRes), fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(when (check.status) { CheckStatus.PASS -> stringResource(R.string.home_check_confirmed); CheckStatus.FAIL -> if (check.id == "endc") stringResource(R.string.home_check_not_connected) else stringResource(R.string.home_check_needs_attention); CheckStatus.WARN -> stringResource(R.string.home_check_review); CheckStatus.UNKNOWN -> stringResource(R.string.unknown) }, fontSize = 10.sp, lineHeight = 15.sp, color = tone, modifier = Modifier.widthIn(max = 95.dp))
            }
            Text(checkDetail(check), fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (check.fixLabelRes != null && check.fix != FixAction.NONE) {
                FilledTonalButton(onClick = onFix, enabled = enabled && !busy, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp)) {
                    if (busy) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp); Spacer(Modifier.width(7.dp)) }
                    Text(if (busy) stringResource(R.string.home_check_applying) else stringResource(check.fixLabelRes), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable private fun HomeLink(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
