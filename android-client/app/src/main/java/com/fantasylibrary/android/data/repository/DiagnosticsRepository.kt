package com.fantasylibrary.android.data.repository

import com.fantasylibrary.android.data.network.Diagnostics
import com.fantasylibrary.android.data.network.HealthReport
import com.fantasylibrary.android.data.network.MediaProbeReport

/** 服务状态与媒体可达性探测。 */
class DiagnosticsRepository(private val diagnostics: Diagnostics) {

    suspend fun checkHealth(baseUrl: String): HealthReport = diagnostics.checkHealth(baseUrl)

    suspend fun probeMedia(url: String): MediaProbeReport = diagnostics.probeMedia(url)
}
