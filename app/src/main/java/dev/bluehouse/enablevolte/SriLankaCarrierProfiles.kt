package dev.bluehouse.enablevolte

import androidx.annotation.StringRes

data class SriLankaCarrierProfile(
    val operatorNumeric: String,
    val name: String,
    val lteBands: List<Int>,
    val nrBands: List<Int>,
    @StringRes val wifiCallingNoteRes: Int,
)

object SriLankaCarrierProfiles {
    private val countryLteBands = listOf(1, 3, 5, 8, 40, 41)

    private val profiles = mapOf(
        "41301" to SriLankaCarrierProfile(
            "41301",
            "SLT-MOBITEL",
            countryLteBands,
            listOf(78),
            R.string.root_lk_slt_note,
        ),
        "41302" to SriLankaCarrierProfile(
            "41302",
            "Dialog",
            countryLteBands,
            listOf(78),
            R.string.root_lk_dialog_note,
        ),
        "41305" to SriLankaCarrierProfile(
            "41305",
            "Airtel Lanka SIM",
            countryLteBands,
            emptyList(),
            R.string.root_lk_airtel_note,
        ),
        "41308" to SriLankaCarrierProfile(
            "41308",
            "Hutch",
            countryLteBands,
            emptyList(),
            R.string.root_lk_hutch_note,
        ),
    )

    fun find(mcc: String?, mnc: String?): SriLankaCarrierProfile? {
        if (mcc.isNullOrBlank() || mnc.isNullOrBlank()) return null
        return profiles[mcc + mnc.padStart(2, '0')]
    }
}
