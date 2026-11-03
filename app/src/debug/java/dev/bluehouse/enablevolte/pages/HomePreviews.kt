package dev.bluehouse.enablevolte.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.bluehouse.enablevolte.*
import dev.bluehouse.enablevolte.R
import dev.bluehouse.enablevolte.components.AppBackdrop
import dev.bluehouse.enablevolte.ui.theme.EnableVoLTETheme
import dev.bluehouse.enablevolte.ui.theme.ThemeMode

/** Design fixtures only. These previews never access a device or a privileged service. */
private fun homeFixture(): SetupState = SetupState(
    loading = false, carrier = "Dialog", checkedAt = 1792745256000,
    checks = listOf(
        ReadinessCheck(id = "privilege", titleRes = R.string.setup_check_privilege_root_title, detailRes = R.string.setup_check_privilege_root_pass, status = CheckStatus.PASS),
        ReadinessCheck(id = "ims", titleRes = R.string.setup_check_ims_title, detailRes = R.string.setup_check_ims_pass, status = CheckStatus.PASS),
        ReadinessCheck(id = "volte", titleRes = R.string.setup_check_volte_title, detailRes = R.string.setup_check_volte_pass, status = CheckStatus.PASS),
        ReadinessCheck(id = "nr_mode", titleRes = R.string.setup_check_nr_title, detailRes = R.string.setup_check_nr_set_to, detailArgRes = R.string.nr_nsa_sa, status = CheckStatus.PASS),
        ReadinessCheck(id = "bands", titleRes = R.string.setup_check_bands_title, detailRes = R.string.setup_check_bands_no_lock, status = CheckStatus.PASS, fix = FixAction.OPEN_BANDS, fixLabelRes = R.string.setup_check_bands_fix, blocking = false),
        ReadinessCheck(id = "patch", titleRes = R.string.setup_check_patch_title, detailRes = R.string.setup_check_patch_active, status = CheckStatus.PASS),
        ReadinessCheck(id = "endc", titleRes = R.string.home_check_not_connected, detailRes = R.string.nr_advertised_no_endc, status = CheckStatus.FAIL),
        ReadinessCheck(id = "ims_reg", titleRes = R.string.setup_check_ims_title, detailRes = R.string.network_ims_registered, detailArg = "LTE", status = CheckStatus.PASS, blocking = false),
    ),
)

@Preview(name = "Home dark · sample data", widthDp = 412, heightDp = 915, showBackground = true)
@Composable private fun HomeDarkPreview() = HomeDesignPreview(ThemeMode.DARK, homeFixture())

@Preview(name = "Home light · sample data", widthDp = 412, heightDp = 915, showBackground = true)
@Composable private fun HomeLightPreview() = HomeDesignPreview(ThemeMode.LIGHT, homeFixture())

@Preview(name = "Home checking", widthDp = 360, heightDp = 800, showBackground = true)
@Composable private fun HomeCheckingPreview() = HomeDesignPreview(ThemeMode.DARK, SetupState())

@Preview(name = "Home no SIM", widthDp = 360, heightDp = 800, showBackground = true)
@Composable private fun HomeNoSimPreview() = HomeDesignPreview(ThemeMode.LIGHT, SetupState(loading = false), noSim = true)

@Preview(name = "Home large text", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable private fun HomeLargeTextPreview() = HomeDesignPreview(ThemeMode.DARK, homeFixture())

@Composable private fun HomeDesignPreview(theme: ThemeMode, state: SetupState, noSim: Boolean = false) {
    EnableVoLTETheme(themeMode = theme) {
        AppBackdrop {
            Box(Modifier.fillMaxSize()) {
                HomeDashboard(
                    sims = if (noSim) emptyList() else listOf(HomeSim(1, "Dialog", 1), HomeSim(2, "SLTMobitel", 2)),
                    selectedId = if (noSim) null else 1, ui = state, accessMode = "Root",
                    onSelectSim = {}, onRefresh = {}, onNavigate = {}, onDismissNote = {}, onFix = {},
                )
            }
        }
    }
}
