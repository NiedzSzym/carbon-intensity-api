package com.niedzszym.carbon_intensity_api.dto;

import java.time.LocalDateTime;

public record ChargingWindowResponse(
        LocalDateTime start,
        LocalDateTime end,
        int intervalCount,
        double cleanEnergyPercent
) {
}
