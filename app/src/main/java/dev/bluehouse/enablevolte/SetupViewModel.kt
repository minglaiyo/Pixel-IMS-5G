package dev.bluehouse.enablevolte

import android.app.Application
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.telephony.CarrierConfigManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CheckStatus { PASS, FAIL, WARN, UNKNOWN }

/** What the user can do about a check that did not pass. */
enum class FixAction {
    NONE,
    ENABLE_VOLTE,
    ENABLE_VONR,
    SET_NR_ARCHITECTURE,
    OPEN_BANDS,
    INSTALL_MODEM_PATCH,
    RESTART_IMS,
    OPEN_EXPERT,
}

/**
 * One thing that has to be true for 5G to work, and what to do when it is not.
 *
 * The app has always been able to test each of these. What it never did was
 * put them in order and say which one is the reason.
 *
 * Copy lives in string resources because a ViewModel cannot call stringResource:
 * titleRes/detailRes/fixLabelRes are R.string ids, resolved in HomeCheck.
 * detailArgRes resolves one nested resource (the NR mode label); detailArg carries
 * runtime data (locked band list); detailText wins for runtime patch messages
 * that no resource can cover.
 */
data class ReadinessCheck(
    val id: String,
    val titleRes: Int,
    val detailRes: Int,
    val detailArgRes: Int? = null,
    val detailArg: String? = null,
    val detailText: String? = null,
    val status: CheckStatus,
    val fix: FixAction = FixAction.NONE,
    val fixLabelRes: Int? = null,
    val blocking: Boolean = true,
)

data class SetupState(
    val loading: Boolean = true,
    val busyFix: FixAction? = null,
    val checks: List<ReadinessCheck> = emptyList(),
    val carrier: String = "",
    val noteRes: Int? = null,
    val noteArg: String? = null,
    val noteArgRes: Int? = null,
    val checkedAt: Long = 0,
    val refreshing: Boolean = false,
) {
    val blocking = checks.filter { it.blocking }
    val passed = blocking.count { it.status == CheckStatus.PASS }
    val total = blocking.size
    val ready = total > 0 && passed == total

    /** The first thing standing in the way, which is the only one worth leading with. */
    val firstProblem: ReadinessCheck? = checks.firstOrNull { it.blocking && it.status == CheckStatus.FAIL }
}

/**
 * Reads setup settings and observed connection state for this SIM.
 *
 * Settings and connection evidence are separate: a missing NR link does not
 * establish that the modem or network rejected it. Fix actions address only
 * the setting each check actually observed.
 */
class SetupViewModel(
    application: Application,
    private val subId: Int,
) : AndroidViewModel(application) {
    private val moder = SubscriptionModer(application, subId)
    private val carrierModer = CarrierModer(application)

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(SetupState())
    val state: kotlinx.coroutines.flow.StateFlow<SetupState> = _state

    private var checkJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        // A newer refresh supersedes an older one rather than racing it to the state.
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = _state.value.checks.isEmpty(), refreshing = true)
            val result = withContext(Dispatchers.IO) { runChecks() }
            _state.value = _state.value.copy(loading = false, checks = result.first, carrier = result.second,
                checkedAt = System.currentTimeMillis(), busyFix = null, refreshing = false)
        }
    }

    fun applyFix(action: FixAction) {
        if (action == FixAction.NONE || _state.value.busyFix != null) return
        _state.value = _state.value.copy(busyFix = action)
        viewModelScope.launch {
            val (noteRes, noteArg, noteArgRes) = withContext(Dispatchers.IO) {
                runCatching {
                    when (action) {
                        FixAction.ENABLE_VOLTE -> {
                            moder.updateCarrierConfig(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
                            moder.restartIMSRegistration()
                            Triple(R.string.setup_fix_volte_on, null, null)
                        }
                        FixAction.ENABLE_VONR -> {
                            moder.updateCarrierConfig(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
                            moder.updateCarrierConfig(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
                            moder.restartIMSRegistration()
                            Triple(R.string.setup_fix_vonr_on, null, null)
                        }
                        FixAction.SET_NR_ARCHITECTURE -> {
                            moder.updateCarrierConfig(
                                CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                                intArrayOf(
                                    CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                                    CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA,
                                ),
                            )
                            Triple(R.string.setup_fix_nr_set, null, null)
                        }
                        // The modem patch is never applied from here: it goes through
                        // RegionalPatchViewModel and its confirmation dialog.
                        FixAction.RESTART_IMS -> {
                            moder.restartIMSRegistration()
                            Triple(R.string.setup_fix_ims_restarted, null, null)
                        }
                        else -> Triple<Int?, String?, Int?>(null, null, null)
                    }
                }.getOrElse {
                    Triple(R.string.setup_fix_failed, it.message,
                        if (it.message == null) R.string.setup_fix_failed_fallback else null)
                }
            }
            _state.value = _state.value.copy(noteRes = noteRes, noteArg = noteArg, noteArgRes = noteArgRes)
            refresh()
        }
    }

    fun dismissNote() {
        _state.value = _state.value.copy(noteRes = null, noteArg = null, noteArgRes = null)
    }

    private fun runChecks(): Pair<List<ReadinessCheck>, String> {
        val out = mutableListOf<ReadinessCheck>()
        val carrier = runCatching { moder.carrierName }.getOrNull().orEmpty()
        val rootMode = PrivilegeManager.activeMode == PrivilegeMode.ROOT

        val privileged = runCatching {
            ReadinessRules.hasAccess(
                rootMode = rootMode,
                rootReady = rootMode && PrivilegeManager.isRootReady(),
                // Only asked in Shizuku mode: the check can raise a permission prompt.
                shizukuGranted = !rootMode && checkShizukuPermission(0) == ShizukuStatus.GRANTED,
            )
        }.getOrDefault(false)

        out += ReadinessCheck(
            id = "privilege",
            titleRes = if (rootMode) R.string.setup_check_privilege_root_title else R.string.setup_check_privilege_shizuku_title,
            detailRes = if (privileged) {
                if (rootMode) R.string.setup_check_privilege_root_pass else R.string.shizuku_permission_granted
            } else {
                R.string.setup_check_privilege_fail
            },
            status = if (privileged) CheckStatus.PASS else CheckStatus.FAIL,
        )
        if (!privileged) return out to carrier

        val supportsIms = runCatching { carrierModer.deviceSupportsIMS }.getOrDefault(false)
        out += ReadinessCheck(
            id = "ims",
            titleRes = R.string.setup_check_ims_title,
            detailRes = if (supportsIms) R.string.setup_check_ims_pass else R.string.setup_check_ims_fail,
            status = if (supportsIms) CheckStatus.PASS else CheckStatus.FAIL,
        )

        val volte = runCatching { moder.isVoLteConfigEnabled }.getOrDefault(false)
        out += ReadinessCheck(
            id = "volte",
            titleRes = R.string.setup_check_volte_title,
            detailRes = if (volte) R.string.setup_check_volte_pass else R.string.setup_check_volte_fail,
            status = if (volte) CheckStatus.PASS else CheckStatus.FAIL,
            fix = if (volte) FixAction.NONE else FixAction.ENABLE_VOLTE,
            fixLabelRes = if (volte) null else R.string.enable_volte,
        )

        val nrIndex = runCatching { moder.nrAvailabilityIndex }.getOrDefault(0)
        out += ReadinessCheck(
            id = "nr_mode",
            titleRes = R.string.setup_check_nr_title,
            detailRes = if (nrIndex == 0) R.string.setup_check_nr_off else R.string.setup_check_nr_set_to,
            detailArgRes = when (nrIndex) {
                1 -> R.string.nr_nsa
                2 -> R.string.nr_sa
                3 -> R.string.nr_nsa_sa
                else -> null
            },
            status = if (nrIndex == 0) CheckStatus.FAIL else CheckStatus.PASS,
            fix = if (nrIndex == 0) FixAction.SET_NR_ARCHITECTURE else FixAction.NONE,
            fixLabelRes = if (nrIndex == 0) R.string.setup_check_nr_fix else null,
        )

        val bands = runCatching { moder.getBandSelection() }.getOrNull()
        val nrLocked = bands?.nrBands?.isNotEmpty() == true
        out += ReadinessCheck(
            id = "bands",
            titleRes = R.string.setup_check_bands_title,
            detailRes = when {
                bands == null -> R.string.setup_check_bands_unreadable
                !nrLocked -> R.string.setup_check_bands_no_lock
                else -> R.string.setup_check_bands_locked
            },
            detailArg = if (nrLocked) bands?.nrBands?.joinToString(", ") { "n$it" } else null,
            status = when {
                bands == null -> CheckStatus.UNKNOWN
                else -> CheckStatus.PASS
            },
            fix = FixAction.OPEN_BANDS,
            fixLabelRes = R.string.setup_check_bands_fix,
            blocking = false,
        )

        if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val vonr = runCatching { moder.isVoNrConfigEnabled }.getOrDefault(false)
            out += ReadinessCheck(
                id = "vonr",
                titleRes = R.string.setup_check_vonr_title,
                detailRes = if (vonr) R.string.setup_check_vonr_pass else R.string.setup_check_vonr_warn,
                status = if (vonr) CheckStatus.PASS else CheckStatus.WARN,
                fix = if (vonr) FixAction.NONE else FixAction.ENABLE_VONR,
                fixLabelRes = if (vonr) null else R.string.enable_vonr,
                blocking = false,
            )
        }

        if (rootMode) {
            val patch = runCatching { PrivilegeManager.getRegionalModemPatchStatus() }.getOrNull()
            // Runtime patch.message survives only via detailText: no resource can cover it.
            val patchDetailText = patch?.takeIf {
                it.supported && it.installed && !it.active && !it.rebootRequired && !it.removalPending
            }?.message?.takeIf { it.isNotBlank() }
            out += ReadinessCheck(
                id = "patch",
                titleRes = R.string.setup_check_patch_title,
                detailRes = when {
                    patch == null -> R.string.setup_check_patch_unreadable
                    !patch.supported -> R.string.setup_check_patch_not_applicable
                    patch.removalPending -> R.string.setup_check_patch_removal_pending
                    patch.installed && patch.rebootRequired -> R.string.setup_check_patch_reboot_required
                    patch.installed && patch.active -> R.string.setup_check_patch_active
                    patch.installed -> R.string.setup_check_patch_installed_inactive
                    !patch.magiskAvailable -> R.string.setup_check_patch_no_root
                    !patch.sourceAvailable -> R.string.setup_check_patch_no_source
                    else -> R.string.setup_check_patch_not_installed
                },
                detailText = patchDetailText,
                status = ReadinessRules.patchStatus(patch),
                fix = if (patch != null && ReadinessRules.canInstallPatch(patch)) FixAction.INSTALL_MODEM_PATCH else FixAction.NONE,
                fixLabelRes = if (patch != null && ReadinessRules.canInstallPatch(patch)) R.string.setup_check_patch_fix else null,
            )
        } else {
            out += ReadinessCheck(
                id = "patch",
                titleRes = R.string.setup_check_patch_title,
                detailRes = R.string.setup_check_patch_shizuku,
                status = CheckStatus.WARN,
                blocking = false,
            )
        }

        return out to carrier
    }

    companion object {
        fun factory(
            application: Application,
            subId: Int,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SetupViewModel(application, subId) as T
            }
    }
}
