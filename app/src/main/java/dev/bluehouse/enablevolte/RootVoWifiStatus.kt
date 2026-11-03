package dev.bluehouse.enablevolte

import androidx.annotation.StringRes
import org.json.JSONObject

data class RootVoWifiStatus(
    val available: Boolean,
    val subscriptionId: Int,
    val settingEnabled: Boolean,
    val roamingEnabled: Boolean,
    val mode: Int,
    val roamingMode: Int,
    val registrationState: Int,
    val transportType: Int,
    val wifiState: Int,
    val snapshotAvailable: Boolean,
    val operationSucceeded: Boolean,
    @StringRes val failureReasonRes: Int? = null,
    val message: String,
    @StringRes val messageRes: Int? = null,
) {
    @get:StringRes
    val modeLabel: Int
        get() = when (mode) {
            0 -> R.string.root_vowifi_mode_wifi_only
            1 -> R.string.root_vowifi_mode_cellular_preferred
            2 -> R.string.root_vowifi_mode_wifi_preferred
            else -> R.string.root_label_unknown
        }

    @get:StringRes
    val roamingModeLabel: Int
        get() = when (roamingMode) {
            0 -> R.string.root_vowifi_mode_wifi_only
            1 -> R.string.root_vowifi_mode_cellular_preferred
            2 -> R.string.root_vowifi_mode_wifi_preferred
            else -> R.string.root_label_unknown
        }

    @get:StringRes
    val registrationLabel: Int
        get() = when (registrationState) {
            2 -> R.string.root_vowifi_reg_registered
            1 -> R.string.root_vowifi_reg_registering
            0 -> R.string.root_vowifi_reg_not_registered
            else -> R.string.root_label_unknown
        }

    @get:StringRes
    val transportLabel: Int
        get() = when (transportType) {
            2 -> R.string.root_vowifi_transport_iwlan
            1 -> R.string.root_vowifi_transport_cellular
            else -> R.string.root_label_unknown
        }

    val isVoWifiActive: Boolean
        get() = registrationState == 2 && transportType == 2

    companion object {
        fun fromJson(value: String): RootVoWifiStatus {
            val json = JSONObject(value)
            return RootVoWifiStatus(
                available = json.optBoolean("available"),
                subscriptionId = json.optInt("subscriptionId", -1),
                settingEnabled = json.optBoolean("settingEnabled"),
                roamingEnabled = json.optBoolean("roamingEnabled"),
                mode = json.optInt("mode", -1),
                roamingMode = json.optInt("roamingMode", -1),
                registrationState = json.optInt("registrationState", -1),
                transportType = json.optInt("transportType", -1),
                wifiState = json.optInt("wifiState", 0),
                snapshotAvailable = json.optBoolean("snapshotAvailable"),
                operationSucceeded = json.optBoolean("operationSucceeded"),
                failureReasonRes = json.optInt("failureReasonRes", -1).takeIf { it >= 0 },
                message = json.optString("message"),
                messageRes = json.optInt("messageRes", -1).takeIf { it >= 0 },
            )
        }

        fun unavailable(
            subscriptionId: Int,
            message: String,
            @StringRes messageRes: Int? = null,
        ) = RootVoWifiStatus(
            available = false,
            subscriptionId = subscriptionId,
            settingEnabled = false,
            roamingEnabled = false,
            mode = -1,
            roamingMode = -1,
            registrationState = -1,
            transportType = -1,
            wifiState = 0,
            snapshotAvailable = false,
            operationSucceeded = false,
            failureReasonRes = null,
            message = message,
            messageRes = messageRes,
        )
    }
}
