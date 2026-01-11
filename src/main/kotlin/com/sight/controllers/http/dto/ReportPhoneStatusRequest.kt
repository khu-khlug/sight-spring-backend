package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.sight.domain.device.BatteryStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class ReportPhoneStatusRequest(
    @field:Min(value = 0, message = "배터리 잔량은 0 이상이어야 합니다")
    @field:Max(value = 100, message = "배터리 잔량은 100 이하여야 합니다")
    @field:JsonProperty("batteryPercent")
    val batteryPercent: Int,
    @field:JsonProperty("batteryStatus")
    val batteryStatus: BatteryStatus,
)
