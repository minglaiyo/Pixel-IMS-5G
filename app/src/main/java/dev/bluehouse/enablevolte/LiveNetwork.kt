package dev.bluehouse.enablevolte

import androidx.annotation.StringRes

/** A current physical carrier, retaining the logical phone/slot it came from. */
data class LivePhysicalChannel(
    val phoneId: Int, val rat: String, val status: Int, val band: Int?, val pci: Int?,
    val channel: Int?, val bandwidthKhz: Int?, val uplinkChannel: Int?, val uplinkBandwidthKhz: Int?,
    val downlinkFrequencyKhz: Int?, val uplinkFrequencyKhz: Int?,
)

/** Deliberately excludes the registry history and all subscriber identifiers. */
internal fun livePhysicalRegistry(raw: String): String = raw.lineSequence().filter {
    it.trim().startsWith("Phone Id=") || it.trim().startsWith("mPhysicalChannelConfigs=")
}.joinToString("\n")

fun parseLivePhysicalChannels(raw: String, phoneId: Int): List<LivePhysicalChannel> {
    var currentPhone = -1
    return buildList {
        raw.lineSequence().forEach { line ->
            val text = line.trim()
            if (text.startsWith("Phone Id=")) currentPhone = text.substringAfter('=').trim().toIntOrNull() ?: -1
            if (currentPhone != phoneId || !text.startsWith("mPhysicalChannelConfigs=")) return@forEach
            Regex("\\{mConnectionStatus=[^}]+\\}").findAll(text).forEach { match ->
                fun field(name: String): String = Regex("\\b$name=([^,}]+)").find(match.value)?.groupValues?.get(1)?.trim().orEmpty()
                fun number(name: String): Int? = field(name).toIntOrNull()?.takeIf { it >= 0 && it != Int.MAX_VALUE }
                val rat = when (field("mNetworkType").uppercase()) { "LTE", "13", "LTE_CA", "19" -> "LTE"; "NR", "20" -> "NR"; else -> return@forEach }
                val status = when (field("mConnectionStatus")) {
                    "PrimaryServing", "PRIMARY_SERVING", "1" -> 1
                    "SecondaryServing", "SECONDARY_SERVING", "2" -> 2
                    else -> 0
                }
                if (status == 0) return@forEach
                add(LivePhysicalChannel(phoneId, rat, status, number("mBand")?.takeIf { it > 0 },
                    number("mPhysicalCellId"), number("mDownlinkChannelNumber"), number("mCellBandwidthDownlinkKhz")?.takeIf { it > 0 },
                    number("mUplinkChannelNumber")?.takeIf { it > 0 }, number("mCellBandwidthUplinkKhz")?.takeIf { it > 0 },
                    number("mDownlinkFrequency")?.takeIf { it > 0 }, number("mUplinkFrequency")?.takeIf { it > 0 }))
            }
        }
    }.distinctBy { listOf(it.rat, it.channel, it.pci, it.band, it.status) }
}

fun liveCarrierName(name: String, numeric: String, fallback: String): String {
    val clean = name.trim()
    if (clean.isNotBlank() && clean.any(Char::isLetter) && !clean.contains('=') && !clean.contains('{')) return clean
    return when (numeric.filter(Char::isDigit)) {
        "41301" -> "SLT-MOBITEL"
        "41302" -> "Dialog"
        "41305" -> "Airtel Lanka"
        "41308" -> "Hutch"
        else -> fallback.takeIf { it.isNotBlank() && !it.contains('{') } ?: "Mobile network"
    }
}

fun radioReading(value: Int?, unit: String = ""): String =
    if (value == null || value == Int.MAX_VALUE || value == Int.MIN_VALUE) "Not reported"
    else (value.toString().replace('-', '−') + if (unit.isBlank()) "" else " $unit")

data class LiveCarrier(
    @StringRes val roleRes: Int, val rat: String, val band: String,
    val cell: SubscriptionModer.CellSnapshot?, val physical: LivePhysicalChannel?,
) {
    val channel: Int? get() = physical?.channel ?: cell?.channel?.takeIf { it >= 0 && it != Int.MAX_VALUE }
    val pci: Int? get() = physical?.pci ?: cell?.pci?.takeIf { it >= 0 && it != Int.MAX_VALUE }
    val bandwidthKhz: Int? get() = physical?.bandwidthKhz ?: cell?.bandwidthKhz
    val key: String get() = "$rat:$channel:$pci:$band"
}

data class LiveConnection(
    @StringRes val titleRes: Int,
    val titleArg: String? = null,
    @StringRes val explanationRes: Int,
    @StringRes val explanationSuffixRes: Int? = null,
    @StringRes val fiveGStatusRes: Int,
    val nrConnected: Boolean, val caActive: Boolean, val carriers: List<LiveCarrier>,
    val neighbours: List<SubscriptionModer.CellSnapshot>, @StringRes val carrierNoteRes: Int,
) {
    val caBandwidth: LiveCaBandwidth get() {
        val lte = carriers.filter { it.rat == "LTE" && (it.physical != null || it.cell != null) }
        return LiveCaBandwidth(lte.map { it.bandwidthKhz?.takeIf { width -> width > 0 && width != Int.MAX_VALUE } },
            if (lte.any { it.physical != null }) {
                R.string.root_live_source_physical
            } else {
                R.string.root_live_source_serving
            })
    }
    val bandCombination: String get() = carriers.filter { it.rat == "LTE" && it.band != "Not reported" }.joinToString(" + ") { it.band }
    val serving: SubscriptionModer.CellSnapshot? get() = carriers.firstOrNull { it.rat == "NR" && it.cell != null }?.cell
        ?: carriers.firstOrNull { it.rat == "LTE" && it.cell != null }?.cell
    val anchor: SubscriptionModer.CellSnapshot? get() = carriers.firstOrNull { it.roleRes == R.string.root_role_4g_anchor }?.cell
}

/** Identified components in this sample only; ServiceState's retained summary is separate. */
data class LiveCaBandwidth(val componentWidthsKhz: List<Int?>, @StringRes val sourceRes: Int) {
    val componentCount: Int get() = componentWidthsKhz.size
    val totalKhz: Int? get() = if (componentWidthsKhz.isNotEmpty() && componentWidthsKhz.all { it != null })
        componentWidthsKhz.filterNotNull().sum() else null
}

fun resolveLiveConnection(
    cells: List<SubscriptionModer.CellSnapshot>, plmn: String, dataRat: String, nrState: Int,
    usingCa: Boolean, nrAvailable: Boolean?, endcAvailable: Boolean?,
    physical: List<LivePhysicalChannel> = emptyList(),
    cellsScopedToSubscription: Boolean = false,
): LiveConnection {
    val digits = plmn.filter(Char::isDigit)
    // Never use a different operator's registered cell as selected-SIM evidence.
    val hasOwnPrimary = cells.any { it.operator.filter(Char::isDigit) == digits && (it.registered || it.connectionStatus == 1) }
    val own = cells.filter {
        digits.isNotBlank() && (it.operator.filter(Char::isDigit) == digits ||
            (cellsScopedToSubscription && hasOwnPrimary && it.operator.filter(Char::isDigit).isBlank() && it.connectionStatus == 2))
    }
    val registered = own.filter { it.registered || it.connectionStatus in 1..2 }
    val nr = dataRat == "NR" || (dataRat in setOf("LTE", "LTE_CA") && nrState == 3) ||
        (dataRat !in setOf("LTE", "LTE_CA", "NR") &&
            (registered.any { it.type == "NR" } || physical.any { it.rat == "NR" }))
    val activePhysical = physical.filter { it.status in 1..2 && (it.rat != "NR" || nr) && (it.rat != "LTE" || dataRat != "NR") }
    val nsa = nr && dataRat != "NR"
    val result = mutableListOf<LiveCarrier>()
    fun role(rat: String, primary: Boolean): Int = when {
        rat == "NR" -> R.string.root_role_5g_carrier
        primary && nsa -> R.string.root_role_4g_anchor
        primary -> R.string.root_role_main_4g_carrier
        else -> R.string.root_role_extra_4g_carrier
    }
    activePhysical.sortedWith(compareBy<LivePhysicalChannel> { it.rat == "NR" }.thenBy { it.status }).forEach { p ->
        // Secondary CellInfo frequently omits MCC/MNC. A current, slot-scoped
        // physical channel plus an exact channel/PCI match can identify it.
        // Never attach another operator's metrics or an ambiguous match.
        val candidates = cells.filter { c ->
            val operator = c.operator.filter(Char::isDigit)
            c.type == p.rat && (operator == digits || operator.isBlank()) &&
                p.channel != null && p.pci != null && c.channel == p.channel && c.pci == p.pci
        }
        val c = candidates.firstOrNull { it.operator.filter(Char::isDigit) == digits }
            ?: candidates.singleOrNull()
        val band = p.band?.let { if (p.rat == "NR") "n$it" else "B$it" } ?: c?.band ?: "Not reported"
        result += LiveCarrier(role(p.rat, p.status == 1), p.rat, band, c, p)
    }
    registered.sortedWith(compareBy<SubscriptionModer.CellSnapshot> { it.type == "NR" }.thenBy { it.connectionStatus != 1 }).forEach { c ->
        if (c.type == "NR" && !nr) return@forEach
        if (c.type == "LTE" && dataRat == "NR") return@forEach
        // A physical report is the current component list for this RAT. Do not
        // append older secondary CellInfo entries when that list shrinks.
        if (activePhysical.any { it.rat == c.type }) return@forEach
        if (result.none { it.rat == c.type && it.channel == c.channel && it.pci == c.pci }) {
            val primary = c.connectionStatus == 1 || (c.connectionStatus != 2 && result.none { it.rat == c.type })
            result += LiveCarrier(role(c.type, primary), c.type, c.band.takeIf { it != "—" && it.isNotBlank() } ?: "Not reported", c, null)
        }
    }
    if (nr && result.none { it.rat == "NR" }) result += LiveCarrier(R.string.root_role_5g_carrier, "NR", "Not reported", null, null)
    if (nsa && result.none { it.rat == "LTE" }) result.add(0, LiveCarrier(R.string.root_role_4g_anchor, "LTE", "Not reported", null, null))
    val lteCount = result.count { it.rat == "LTE" }
    val ca = dataRat != "NR" && (usingCa || dataRat == "LTE_CA" || lteCount > 1)
    val titleRes = when {
        nr && nsa -> R.string.root_live_title_5g_nsa
        nr -> R.string.root_live_title_5g_sa
        ca && lteCount > 1 -> R.string.root_live_title_4g_plus_connected
        ca -> R.string.root_live_title_4g_plus_reported
        dataRat in setOf("LTE", "LTE_CA") -> R.string.root_live_title_4g_connected
        dataRat == "Unknown" -> R.string.root_live_title_waiting
        else -> R.string.root_live_title_other
    }
    // Only the generic branch carries an argument: the RAT name itself, which
    // stays untranslated (LTE, NR, UMTS, ...). All other titles need no argument.
    val titleArg = if (titleRes == R.string.root_live_title_other) dataRat else null
    val explanationRes = when {
        nsa -> R.string.root_live_expl_nsa
        nr -> R.string.root_live_expl_sa
        ca && lteCount > 1 -> R.string.root_live_expl_ca_multi
        ca -> R.string.root_live_expl_ca_incomplete
        dataRat == "LTE" -> R.string.root_live_expl_lte
        else -> R.string.root_live_expl_waiting
    }
    // The suffix is concatenated directly after the base explanation: its
    // English text starts with a space, its Chinese text does not.
    val explanationSuffixRes = when {
        nr -> null
        nrState == 1 -> R.string.root_live_expl_suffix_restricted
        nrAvailable == true && endcAvailable == false -> R.string.root_live_expl_suffix_advertised_no_endc
        else -> null
    }
    val carrierNoteRes = when {
        usingCa && lteCount < 2 -> R.string.root_live_note_ca_no_detail
        activePhysical.isNotEmpty() -> R.string.root_live_note_physical
        result.isNotEmpty() -> R.string.root_live_note_serving
        else -> R.string.root_live_note_no_serving
    }
    return LiveConnection(
        titleRes, titleArg, explanationRes, explanationSuffixRes, when {
            nr -> R.string.root_live_5g_connected
            nrState == 1 -> R.string.root_live_5g_restricted
            nrAvailable == true && endcAvailable == false -> R.string.root_live_5g_advertised_lte_only
            nrAvailable == true || endcAvailable == true -> R.string.root_live_5g_available_not_connected
            else -> R.string.root_live_5g_not_reported
        }, nr, ca, result, cells.filter { c -> result.none { it.cell === c } }, carrierNoteRes,
    )
}
