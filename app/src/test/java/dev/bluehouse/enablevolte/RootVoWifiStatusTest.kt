package dev.bluehouse.enablevolte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootVoWifiStatusTest {
    @Test
    fun parsesActiveIwlanStatus() {
        val status = RootVoWifiStatus.fromJson(
            """
            {
              "available": true,
              "subscriptionId": 2,
              "settingEnabled": true,
              "roamingEnabled": true,
              "mode": 2,
              "roamingMode": 2,
              "registrationState": 2,
              "transportType": 2,
              "wifiState": 3,
              "snapshotAvailable": true,
              "operationSucceeded": true,
              "failureReason": "",
              "message": "ready"
            }
            """.trimIndent(),
        )

        assertTrue(status.isVoWifiActive)
        assertEquals(R.string.root_vowifi_mode_wifi_preferred, status.modeLabel)
        assertEquals(R.string.root_vowifi_transport_iwlan, status.transportLabel)
    }

    @Test
    fun preservesRootFailureDiagnosis() {
        val status = RootVoWifiStatus.fromJson(
            """
            {
              "subscriptionId": 1,
              "failureReasonRes": ${R.string.root_vowifi_fail_ike_timeout},
              "message": "not active"
            }
            """.trimIndent(),
        )

        assertEquals(R.string.root_vowifi_fail_ike_timeout, status.failureReasonRes)
        assertEquals(R.string.root_label_unknown, status.modeLabel)
    }
}
