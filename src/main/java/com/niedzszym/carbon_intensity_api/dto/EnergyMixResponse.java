package com.niedzszym.carbon_intensity_api.dto;

public record EnergyMixResponse(
    DailyEnergyMixResponse today,
    DailyEnergyMixResponse tomorrow,
    DailyEnergyMixResponse dayAfterTomorrow
) {
}
